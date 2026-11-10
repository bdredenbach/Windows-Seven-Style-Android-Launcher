package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.widget.*;
import android.view.*;
import java.util.*;
import java.util.concurrent.Executor;
import static com.nthexperience.desktop.InitializationState.Step;

/** One optional, ordered setup flow. Each Android grant remains the user's choice. */
class InitializationFlow {
    static final int HOME=12,USAGE=620,NOTIFICATIONS=621,BRIGHTNESS=622,REPAIR=623,FOLDER=624,LOCATION=625,USAGE_INFO=627;
    interface Host {boolean ready();boolean home();boolean requestHome();void changed();void wake();}
    private final Activity activity;private final DesktopStore store;private final Host host;private final Executor worker;
    final InitializationState state;
    private final Handler main=new Handler(Looper.getMainLooper());
    private AlertDialog dialog;private boolean resumed,closed,working;private long launchedAt;
    private List<FolderRepair.Target> missing;
    private final Runnable wake=this::wakeHost;
    private final View.OnLayoutChangeListener bounds=(v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)layoutDialog();};
    private void wakeHost(){if(!closed)host.wake();}
    InitializationFlow(Activity activity,DesktopStore store,Host host,Executor worker){this(activity,store,host,worker,true);}
    InitializationFlow(Activity activity,DesktopStore store,Host host,Executor worker,boolean restored){this.activity=activity;this.store=store;this.host=host;this.worker=worker;state=new InitializationState(activity);if(state.prefs.getBoolean("processing",false)||!restored&&state.waiting()!=0)state.clearWaiting();activity.getWindow().getDecorView().addOnLayoutChangeListener(bounds);}
    void resumed(){resumed=true;}
    void paused(){resumed=false;if(state.waiting()!=0)state.prefs.edit().putBoolean("left",true).commit();}
    boolean pending(){
        if(closed||!resumed||!host.ready())return true;
        if(dialog!=null||working)return true;
        if(state.waiting()!=0){
            if(state.waiting()==REPAIR||state.waiting()==FOLDER)return true;
            boolean returned=state.prefs.getBoolean("returned",false),left=state.prefs.getBoolean("left",false);
            if(!left&&!returned)return true;
            // Some Settings implementations send a result before their activity takes focus.
            if(!left&&SystemClock.uptimeMillis()-launchedAt<400){main.removeCallbacks(wake);main.postDelayed(wake,400);return true;}
            // Returning from either Usage Access screen is not a completed permission choice.
            // Recheck the actual grant below; denial/Back must keep App info recovery available.
            if(state.waiting()==USAGE||state.waiting()==USAGE_INFO){state.clearWaiting();host.changed();}
            else completedStep();
        }
        while(state.step()!=Step.DONE){
            Step step=state.step();
            if(step==Step.FOLDERS){repair();return true;}
            if(granted(step)){state.advance();continue;}
            prompt(step);return true;
        }
        return false;
    }
    boolean granted(Step step){switch(step){case HOME:return host.home();case FILES:return StorageAccess.has(activity);case USAGE:return ExternalTasks.permitted(activity);case NOTIFICATIONS:return TenNotifications.allowed(activity);case BRIGHTNESS:return Settings.System.canWrite(activity);case LOCATION:return LocationAccess.permitted(activity);default:return false;}}
    private void prompt(Step step){
        String title,message,button;
        switch(step){
            case HOME:title="Default Launcher";message="Choose Nth Desktop as your default Home app.";button="Choose default launcher";break;
            case FILES:title="Set up Nth Explorer";message="Browse and manage your shared device files in familiar Nth Explorer windows. "+(Build.VERSION.SDK_INT>=30?"Android will ask you to allow ‘All files access’.":"Android will ask for storage permission.")+"\n\nFiles stay on your device. Android protects system files and other apps’ private data. You can connect individual folders instead, or change access later in Control Panel → File access.";button="Enable file browsing";break;
            case USAGE:title="Set up Usage Access";message="Usage Access is optional. It lets "+activity.getString(R.string.app_name)+" show apps you open elsewhere on the taskbar. Tap OK, turn on Usage Access for "+activity.getString(R.string.app_name)+", then press Back. The checkbox turns on when you return.";button="OK";break;
            case NOTIFICATIONS:title="Allow Notifications Access";message="Allow the taskbar notification center to show your phone's notifications. On Android's next screen, select this desktop and allow Notification access.";button="Allow notification access";break;
            case BRIGHTNESS:title="Modify System Settings (Brightness)";message="Allow Nth Desktop's brightness control to adjust the phone's brightness. On Android's next screen, allow modifying system settings.";button="Allow brightness control";break;
            case LOCATION:title="Allow Location";message="Use your location for local weather while Nth Desktop is visible. Approximate location is enough for weather. "+(Build.VERSION.SDK_INT>=31?"Android will let you choose approximate or precise access.":"Choose approximate or precise access below.");button=Build.VERSION.SDK_INT>=31?"Choose location access":"Use precise location";break;
            default:return;
        }
        LinearLayout content=Aero.column(activity);Aero.pad(content,16);
        content.addView(paragraph(message));
        if(step==Step.USAGE&&Build.VERSION.SDK_INT>=33){
            TextView recovery=paragraph("If Android says ‘App was denied access’, tap Close, then press Back to return to this guide. Tap Open App info below, open the three-dot menu (⋮), and choose Allow restricted settings if shown. Then press Back to return here and tap OK to try Usage Access again.");
            recovery.setPadding(0,Aero.dp(activity,12),0,Aero.dp(activity,10));content.addView(recovery);
            content.addView(new UsageAccessExample(activity),new LinearLayout.LayoutParams(-1,-2));
        }
        TextView optional=paragraph("This is optional. Choose Later to continue and change access later in Android settings.");optional.setPadding(0,Aero.dp(activity,12),0,0);content.addView(optional);
        ScrollView scroll=new ScrollView(activity);scroll.setContentDescription("Initialization guide");scroll.addView(content);
        AlertDialog.Builder builder=Aero.dialog(activity).setTitle(title).setView(scroll).setPositiveButton(button,(d,w)->request(step)).setNegativeButton("Later",(d,w)->{state.advance();postWake();});
        if(step==Step.USAGE&&Build.VERSION.SDK_INT>=33)builder.setNeutralButton("Open App info",(d,w)->usageAppInfo());
        if(step==Step.LOCATION&&Build.VERSION.SDK_INT<31)builder.setNeutralButton("Use approximate location",(d,w)->requestApproximateLocation());
        if(step==Step.FILES)builder.setNeutralButton("Choose folders",(d,w)->pick(FOLDER,null));
        show(builder,()->{state.advance();postWake();});
    }
    private void show(AlertDialog.Builder builder,Runnable cancel){
        AlertDialog shown=builder.create();dialog=shown;shown.setOnCancelListener(d->cancel.run());shown.setOnDismissListener(d->{if(dialog==shown)dialog=null;postWake();});shown.show();Aero.styleDialog(activity,shown);layoutDialog();
    }
    private TextView paragraph(String text){TextView view=Aero.text(activity,text,14,Aero.INK);view.setLineSpacing(Aero.dp(activity,3),1);return view;}
    private void layoutDialog(){if(!closed)fitDialog(activity,dialog);}
    static void fitDialog(Activity activity,AlertDialog shown){
        if(shown==null||!shown.isShowing()||shown.getWindow()==null||activity.isFinishing()||activity.isDestroyed())return;
        View host=activity.getWindow().getDecorView();int width=host.getWidth(),height=host.getHeight();
        if(width<=0)width=activity.getResources().getDisplayMetrics().widthPixels;
        if(height<=0)height=activity.getResources().getDisplayMetrics().heightPixels;
        WindowInsets insets=host.getRootWindowInsets();
        if(insets!=null){if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());width-=safe.left+safe.right;height-=safe.top+safe.bottom;}else{width-=insets.getSystemWindowInsetLeft()+insets.getSystemWindowInsetRight();height-=insets.getSystemWindowInsetTop()+insets.getSystemWindowInsetBottom();}}
        int fitWidth=Math.max(1,Math.min(Aero.dp(activity,420),width-Aero.dp(activity,32))),maxHeight=Math.max(1,height-Aero.dp(activity,32));
        shown.getWindow().setLayout(fitWidth,ViewGroup.LayoutParams.WRAP_CONTENT);
        View frame=shown.getWindow().getDecorView();frame.post(()->{if(!activity.isFinishing()&&!activity.isDestroyed()&&shown.isShowing()&&frame.getHeight()>maxHeight)shown.getWindow().setLayout(fitWidth,maxHeight);});
    }
    private void usageAppInfo(){
        state.waitFor(USAGE_INFO,null);launchedAt=SystemClock.uptimeMillis();
        try{activity.startActivityForResult(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",activity.getPackageName(),null)),USAGE_INFO);}
        catch(ActivityNotFoundException|SecurityException unavailable){state.clearWaiting();notice("App info is unavailable on this device. You can continue or choose Later.");}
        postWake();
    }
    private void request(Step step){
        int code=step==Step.HOME?HOME:step==Step.FILES?StorageAccess.REQUEST:step==Step.USAGE?USAGE:step==Step.NOTIFICATIONS?NOTIFICATIONS:step==Step.LOCATION?LOCATION:BRIGHTNESS;
        launchedAt=SystemClock.uptimeMillis();state.waitFor(code,null);
        try{
            switch(step){
                case HOME:if(!host.requestHome()){state.clearWaiting();completedStep();}break;
                case FILES:
                    if(Build.VERSION.SDK_INT<30)activity.requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE},StorageAccess.REQUEST);
                    else try{activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+activity.getPackageName())),StorageAccess.REQUEST);}catch(ActivityNotFoundException|SecurityException unavailable){activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),StorageAccess.REQUEST);}break;
                case USAGE:
                    store.usagePrefs.edit().remove(DesktopStore.TASKBAR_USAGE).apply();
                    try{activity.startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,Uri.parse("package:"+activity.getPackageName())),USAGE);}catch(ActivityNotFoundException|SecurityException unavailable){activity.startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),USAGE);}break;
                case NOTIFICATIONS:activity.startActivityForResult(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),NOTIFICATIONS);break;
                case BRIGHTNESS:activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+activity.getPackageName())),BRIGHTNESS);break;
                case LOCATION:LocationAccess.request(activity,LOCATION,true);break;
                default:break;
            }
        }catch(ActivityNotFoundException|SecurityException unavailable){state.clearWaiting();notice("This settings screen is unavailable on this device. You can continue setup.");completedStep();}
        postWake();
    }
    private void requestApproximateLocation(){launchedAt=SystemClock.uptimeMillis();state.waitFor(LOCATION,null);try{LocationAccess.request(activity,LOCATION,false);}catch(SecurityException unavailable){state.clearWaiting();completedStep();}postWake();}
    private void completedStep(){
        Step step=state.step();state.clearWaiting();
        if(step==Step.FILES)activity.getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).apply();
        if(step==Step.BRIGHTNESS&&Settings.System.canWrite(activity))store.prefs.edit().putBoolean("brightness_system",true).apply();
        state.advance();host.changed();postWake();
    }
    private void repair(){
        if(missing==null){working=true;worker.execute(()->{
            List<FolderRepair.Target> found=FolderRepair.missing(activity,store);FolderRepair.Plan automatic=null;
            // Each Nth desktop has its own provider authority. Use this desktop's existing grant.
            for(FolderRepair.Target target:found){Uri local=FolderRepair.localTree(target.uri);if(local==null||target.uri.equals(local.toString())||!StorageAccess.has(activity))continue;try{if(FolderRepair.readable(activity,DocumentFolders.rootPage(local.toString(),target.name))){FolderRepair.Plan plan=FolderRepair.reconnect(store,target.uri,local,(page,tree)->FolderRepair.resolve(activity,page,tree));if(plan.changes()){automatic=plan;break;}}}catch(org.json.JSONException ignored){}}
            FolderRepair.Plan prepared=automatic;
            main.post(()->{if(closed)return;working=false;if(prepared!=null&&prepared.apply(store)){missing=null;host.changed();}else missing=found;postWake();});
        });return;}
        Set<String> deferred=state.deferred();FolderRepair.Target target=null;for(FolderRepair.Target candidate:missing)if(!deferred.contains(candidate.uri)){target=candidate;break;}
        if(target==null){state.advance();postWake();return;}
        FolderRepair.Target selected=target;
        show(Aero.dialog(activity).setTitle("Fix folders from restored backup").setMessage("Reconnect “"+selected.name+"” so its saved folders and shortcuts can be opened on this desktop. Choose the original folder in Android's folder picker. Missing subfolders and their shortcuts will be kept for a later retry.")
                .setPositiveButton("Reconnect folder",(d,w)->pick(REPAIR,selected.uri))
                .setNegativeButton("Later",(d,w)->{state.defer(selected.uri);postWake();}),()->{state.defer(selected.uri);postWake();});
    }
    private void pick(int request,String uri){
        state.waitFor(request,uri);launchedAt=SystemClock.uptimeMillis();Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);if(uri!=null)intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,Uri.parse(uri));
        try{activity.startActivityForResult(intent,request);}catch(ActivityNotFoundException|SecurityException unavailable){state.clearWaiting();notice("A folder picker is unavailable. Your saved folders have been kept.");if(request==REPAIR)state.defer(uri);else state.advance();postWake();}
    }
    boolean result(int request,int result,Intent data){
        if(closed||state.waiting()==0||request!=state.waiting())return false;
        if(request!=REPAIR&&request!=FOLDER){state.prefs.edit().putBoolean("returned",true).commit();postWake();return true;}
        String original=state.prefs.getString("repair_uri",null);
        if(result!=Activity.RESULT_OK||data==null||data.getData()==null){state.clearWaiting();postWake();return true;}
        Uri tree=data.getData();int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try{if(!"content".equals(tree.getScheme())||!DocumentsContract.isTreeUri(tree)||(flags&Intent.FLAG_GRANT_READ_URI_PERMISSION)==0)throw new SecurityException();activity.getContentResolver().takePersistableUriPermission(tree,flags);}catch(RuntimeException invalid){state.clearWaiting();notice("This provider could not keep folder access. Your saved links are unchanged; choose another folder.");postWake();return true;}
        working=true;state.prefs.edit().putBoolean("processing",true).commit();
        worker.execute(()->{
            String error=null,name=null;FolderRepair.Plan plan=null;
            try{if(!FolderRepair.readable(activity,DocumentFolders.rootPage(tree.toString(),"Linked folder")))throw new IllegalStateException("This folder is unavailable. Your saved links have been kept.");
                if(request==REPAIR)plan=FolderRepair.reconnect(store,original,tree,(page,chosen)->FolderRepair.resolve(activity,page,chosen));
                else name=DocumentFolders.rootName(activity,tree);
            }catch(Exception failure){error=failure.getMessage()==null?"The folder could not be connected. Your saved links were kept.":failure.getMessage();}
            String problem=error,folderName=name;FolderRepair.Plan prepared=plan;
            main.post(()->{if(closed)return;working=false;String message=problem;
                // Only a live flow may apply the worker's plan; recreation retries interrupted work.
                if(message==null){if(request==REPAIR&&!prepared.apply(store))message="The saved folders changed. Please reconnect again.";else if(request==FOLDER&&!DocumentFolders.add(store,tree,folderName))message="Up to 16 folders can be linked. Remove an unused link before adding another.";}
                state.clearWaiting();missing=null;
                if(message!=null)notice(message);else{host.changed();if(request==FOLDER)completedStep();if(prepared!=null&&prepared.unresolved>0){state.defer(original);notice("Folder connected. Some saved subfolders were not found; their shortcuts have been kept. Use Fix linked folders in Backup and Restore to retry.");}}
                postWake();
            });
        });return true;
    }
    void permissionResult(int request){if(request==state.waiting())result(request,Activity.RESULT_CANCELED,null);}
    private void notice(String message){Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
    private void postWake(){main.removeCallbacks(wake);main.post(wake);}
    void retryFolders(){InitializationState.restored(activity);missing=null;postWake();}
    void close(){closed=true;activity.getWindow().getDecorView().removeOnLayoutChangeListener(bounds);main.removeCallbacksAndMessages(null);if(dialog!=null){dialog.setOnCancelListener(null);dialog.setOnDismissListener(null);dialog.dismiss();dialog=null;}}
}
