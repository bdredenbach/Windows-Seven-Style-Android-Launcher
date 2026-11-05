package com.nthexperience.desktop;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Launcher diagnostics and an in-app Steps Recorder, surfaced from the tray. */
final class ActionCenter extends LinearLayout {
    interface Host {
        boolean isHome();
        void home();
        void fileSetup();
        void open(String id);
        boolean capture();
        void export();
    }
    private final Host host;
    private final DesktopStore store;
    private final StepRecorder recorder;
    private final android.content.SharedPreferences notices;

    ActionCenter(Context context,DesktopStore store,StepRecorder recorder,Host host){
        super(context);this.host=host;this.store=store;this.recorder=recorder;
        notices=context.getSharedPreferences("seven_action_center",Context.MODE_PRIVATE);
        setOrientation(VERTICAL);setContentDescription("Nth Action Center");
        refresh();
    }

    static void notice(Context context,String key,String message){
        context.getSharedPreferences("seven_action_center",Context.MODE_PRIVATE).edit().putString(key,message).apply();
    }
    void refresh(){
        removeAllViews();
        ScrollView scroll=new ScrollView(getContext());scroll.setFillViewport(true);
        LinearLayout content=Aero.column(getContext());Aero.pad(content,14);scroll.addView(content);
        addView(scroll,new LayoutParams(-1,-1));
        heading(content,"Action Center");
        if(host.isHome())line(content,"Nth Desktop Seven is the default Home app.");
        else action(content,"Choose the default Home app…",host::home);
        if(!StorageAccess.has(getContext()))action(content,"Review device file access…",host::fileSetup);
        int missing=missingLinks(getContext(),store);
        if(missing>0){
            line(content,missing+" linked folder"+(missing==1?" needs":"s need")+" access again.");
            action(content,"Review linked folders in Computer",()->host.open("@computer"));
        }
        String file=notices.getString("files","");
        if(!file.isEmpty()){
            line(content,file);
            action(content,"Open Nth Explorer",()->host.open("@files"));
        }
        String backup=notices.getString("backup","");
        if(!backup.isEmpty()){
            line(content,backup);
            action(content,"Open Backup and Restore",()->host.open("@backup"));
        }
        if(!file.isEmpty()||!backup.isEmpty())action(content,"Dismiss recorded alerts",()->{notices.edit().clear().apply();refresh();});
        heading(content,"Steps Recorder");
        line(content,"Record Nth actions and optionally capture the desktop. Review the steps and pictures before exporting.");
        if(recorder.active()){
            action(content,"Capture desktop",()->{if(!host.capture())line(content,"Desktop capture is unavailable or the report has reached its limit.");else refresh();});
            action(content,"Stop recording",()->{recorder.stop();refresh();});
        }else{
            action(content,recorder.empty()?"Start recording":"Start a new recording",()->{
                if(recorder.empty()){recorder.start();refresh();}
                else Aero.dialog(getContext()).setTitle("Start a new recording?")
                        .setMessage("The current report will be replaced.").setPositiveButton("Start",(dialog,which)->{recorder.start();refresh();})
                        .setNegativeButton("Cancel",null).show();
            });
        }
        if(!recorder.empty()){
            line(content,recorder.size()+" recorded steps");
            action(content,"Review recorded steps…",()->review());
            action(content,"Remove last screenshot",()->{if(recorder.removePicture())refresh();});
            if(!recorder.active())action(content,"Export report…",host::export);
        }
    }

    private void review(){
        ScrollView scroll=new ScrollView(getContext());scroll.addView(recorder.review(getContext()));
        Aero.dialog(getContext()).setTitle("Recorded steps").setView(scroll).setPositiveButton("Close",null).show();
    }
    static final class Alert {
        final String title,message,target;
        Alert(String title,String message,String target){this.title=title;this.message=message;this.target=target;}
    }
    static java.util.List<Alert> alerts(Context context,DesktopStore store){
        java.util.List<Alert> result=new java.util.ArrayList<>();
        int missing=missingLinks(context,store);
        if(missing>0)result.add(new Alert("Folder access",missing+" linked folder"+(missing==1?" needs":"s need")+" access again.","@computer"));
        android.content.SharedPreferences saved=context.getSharedPreferences("seven_action_center",Context.MODE_PRIVATE);
        String files=saved.getString("files",""),backup=saved.getString("backup","");
        if(!files.isEmpty())result.add(new Alert("Nth Explorer",files,"@files"));
        if(!backup.isEmpty())result.add(new Alert("Backup and Restore",backup,"@backup"));
        return result;
    }
    private static int missingLinks(Context context,DesktopStore store){
        Set<Uri> allowed=new HashSet<>();
        for(UriPermission permission:context.getContentResolver().getPersistedUriPermissions())
            if(permission.isReadPermission())allowed.add(permission.getUri());
        JSONArray roots=DocumentFolders.links(store);
        int missing=0;
        for(int i=0;i<roots.length();i++){
            JSONObject item=roots.optJSONObject(i);
            if(item!=null){Uri uri=Uri.parse(item.optString("uri"));if(!allowed.contains(uri)&&!(StorageAccess.ours(uri)&&StorageAccess.has(context)))missing++;}
        }
        return missing;
    }
    private void heading(LinearLayout content,String text){
        TextView label=Aero.text(getContext(),text,19,0xff2e682e);Aero.pad(label,6);content.addView(label);
    }
    private void line(LinearLayout content,String text){
        TextView label=Aero.text(getContext(),text,13,Aero.INK);Aero.pad(label,6);content.addView(label);
    }
    private void action(LinearLayout content,String text,Runnable action){
        content.addView(Aero.button(getContext(),text,action),new LayoutParams(-1,-2));
    }
}
