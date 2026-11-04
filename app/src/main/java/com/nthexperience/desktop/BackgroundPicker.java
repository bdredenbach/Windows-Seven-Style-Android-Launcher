package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Edits a draft until Save changes is selected. */
final class BackgroundPicker extends LinearLayout {
    static final String[] POSITIONS={"Fill","Fit","Stretch","Tile","Center"};
    static final int[] COLORS={0xff0a263b,0xff000000,0xffe5edf2,0xff374d42,0xff40344d,0xff6a5135};
    private final DesktopStore store;
    private final JSONObject draft;
    private final Set<String> selected=new LinkedHashSet<>(),photos=new LinkedHashSet<>();
    private final Map<String,CheckBox> boxes=new LinkedHashMap<>();
    private final Map<String,Bitmap> thumbs=new LinkedHashMap<>();
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final Handler main=new Handler(Looper.getMainLooper());
    private final CancellationSignal thumbnails=new CancellationSignal();
    private CancellationSignal request;
    private final AeroWallpaper preview;
    private final LinearLayout gallery;
    private final TextView save,summary;
    private final Spinner interval;
    private final CheckBox shuffle;
    private String current;
    private boolean syncing,closed,pictureReady;

    BackgroundPicker(Context context,DesktopStore store,JSONObject draft,Runnable browse,Runnable folder,Runnable apply){
        super(context);this.store=store;this.draft=draft;
        setOrientation(VERTICAL);
        if(!draft.has("selected")){
            List<String> saved=DesktopBackgrounds.current(store.prefs);
            put("selected",new JSONArray(saved));put("photos",new JSONArray());
            put("position",store.prefs.getString("wallpaper_position","Fill"));
            put("background",store.prefs.getInt("wallpaper_bg",0));
            put("interval",store.prefs.getInt(DesktopBackgrounds.INTERVAL,DesktopBackgrounds.DEFAULT_INTERVAL));
            put("shuffle",store.prefs.getBoolean(DesktopBackgrounds.SHUFFLE,false));
        }
        selected.addAll(keys(draft.optJSONArray("selected")));
        photos.addAll(keys(draft.optJSONArray("photos")));
        for(String key:selected)if(DesktopBackgrounds.stockId(key)<0)photos.add(key);
        current=selected.isEmpty()?DesktopBackgrounds.stock(AeroWallpaper.SEVEN):selected.iterator().next();

        addView(Aero.text(context,"Choose your desktop background",21,0xff2e682e));
        TextView hint=Aero.text(context,"Choose a picture, or check several to create a slide show.",12,0xff4e6676);
        hint.setPadding(0,dp(5),0,dp(8));addView(hint);
        addView(Aero.text(context,"Picture location",12,Aero.INK));
        String[] locations={"Desktop backgrounds","Alpine and coastal landscapes","Architecture","Illustrations","My Pictures"};
        Spinner location=spinner(locations,"Background picture location");
        int start=photos.contains(current)?4:draft.optInt("location",0);
        location.setSelection(Math.max(0,Math.min(locations.length-1,start)));addView(location,new LayoutParams(-1,dp(42)));

        LinearLayout browseRow=new LinearLayout(context);
        button(browseRow,"Browse pictures…",browse);button(browseRow,"Browse folder…",folder);addView(browseRow);
        LinearLayout selectionRow=new LinearLayout(context);
        button(selectionRow,"Select all",()->{
            for(String key:boxes.keySet())if(selected.size()<DesktopBackgrounds.LIMIT)selected.add(key);
            if(!selected.isEmpty()&&!selected.contains(current))current=selected.iterator().next();
            syncSelection();
        });
        button(selectionRow,"Clear all",()->{selected.clear();syncSelection();});
        addView(selectionRow);
        summary=Aero.text(context,"",12,0xff4e6676);addView(summary);

        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setNestedScrollingEnabled(true);
        scroll.setBackground(Aero.panel(context,0xffffffff,0xfff7fbfe,1,0xffa9bfd0));
        gallery=Aero.column(context);Aero.pad(gallery,5);
        scroll.addView(gallery);addView(scroll,new LayoutParams(-1,dp(245)));

        preview=new AeroWallpaper(context,AeroWallpaper.SEVEN,false);
        preview.setContentDescription("Desktop background preview");
        preview.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        LayoutParams previewSize=new LayoutParams(-1,dp(104));previewSize.topMargin=dp(8);addView(preview,previewSize);
        addView(Aero.text(context,"Picture position",13,Aero.INK));
        Spinner position=spinner(POSITIONS,"Picture position");String savedPosition=draft.optString("position","Fill");
        for(int i=0;i<POSITIONS.length;i++)if(POSITIONS[i].equals(savedPosition))position.setSelection(i);
        addView(position,new LayoutParams(-1,dp(42)));
        listen(position,i->{String value=POSITIONS[i];if(!value.equals(draft.optString("position"))){put("position",value);showPreview();}});

        addView(Aero.text(context,"Change picture every",13,Aero.INK));
        interval=spinner(DesktopBackgrounds.INTERVAL_LABELS,"Change picture every");
        int seconds=draft.optInt("interval",DesktopBackgrounds.DEFAULT_INTERVAL);
        for(int i=0;i<DesktopBackgrounds.INTERVALS.length;i++)if(seconds==DesktopBackgrounds.INTERVALS[i])interval.setSelection(i);
        listen(interval,i->put("interval",DesktopBackgrounds.INTERVALS[i]));
        addView(interval,new LayoutParams(-1,dp(42)));
        shuffle=Aero.check(context,"Shuffle",draft.optBoolean("shuffle"));
        shuffle.setOnCheckedChangeListener((v,checked)->put("shuffle",checked));addView(shuffle);

        LinearLayout colors=new LinearLayout(context);
        for(int i=0;i<COLORS.length;i++){
            int index=i;
            TextView color=Aero.text(context,"",12,Aero.INK);
            color.setBackground(Aero.panel(context,COLORS[i],COLORS[i],2,0xff8da9b9));
            color.setContentDescription("Background colour "+(i+1));color.setFocusable(true);
            color.setOnClickListener(v->{put("background",index);showPreview();});
            LayoutParams size=new LayoutParams(0,dp(30),1);size.setMargins(dp(3),dp(3),dp(3),dp(6));colors.addView(color,size);
        }
        addView(colors);
        LinearLayout footer=new LinearLayout(context);
        save=button(footer,"Save changes",()->{
            if(selected.isEmpty()||!pictureReady)return;
            android.content.SharedPreferences.Editor edit=store.prefs.edit();
            DesktopBackgrounds.choose(edit,new ArrayList<>(selected),store.prefs.getInt("wallpaper",AeroWallpaper.SEVEN));
            edit.putString("wallpaper_position",draft.optString("position","Fill"))
                .putInt("wallpaper_bg",draft.optInt("background",0))
                .putInt(DesktopBackgrounds.INTERVAL,draft.optInt("interval",DesktopBackgrounds.DEFAULT_INTERVAL))
                .putBoolean(DesktopBackgrounds.SHUFFLE,draft.optBoolean("shuffle")).apply();
            clearDraft();apply.run();Toast.makeText(context,"Desktop background updated",Toast.LENGTH_SHORT).show();
        });
        button(footer,"Cancel changes",()->{clearDraft();apply.run();});addView(footer);
        listen(location,i->{put("location",i);populate(i);scroll.scrollTo(0,0);});
        populate(location.getSelectedItemPosition());syncSelection();
    }

    static void imported(JSONObject draft,List<String> incoming){
        put(draft,"location",4);put(draft,"photos",new JSONArray(incoming));put(draft,"selected",new JSONArray(incoming));
    }
    private static List<String> keys(JSONArray values){
        List<String> result=new ArrayList<>();
        if(values!=null)for(int i=0;i<Math.min(DesktopBackgrounds.LIMIT,values.length());i++){
            String key=values.optString(i);
            if(DesktopBackgrounds.valid(key)&&!result.contains(key))result.add(key);
        }
        return result;
    }
    private int dp(int value){return Aero.dp(getContext(),value);}
    private void put(String name,Object value){put(draft,name,value);}
    private static void put(JSONObject json,String name,Object value){GadgetLayout.put(json,name,value);}
    private void clearDraft(){java.util.Iterator<String> iterator=draft.keys();while(iterator.hasNext()){iterator.next();iterator.remove();}}
    private Spinner spinner(String[] values,String description){
        Spinner spinner=new Spinner(getContext());spinner.setContentDescription(description);
        spinner.setAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_dropdown_item,values));return spinner;
    }
    private void listen(Spinner spinner,java.util.function.IntConsumer select){
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(AdapterView<?> view){}
            @Override public void onItemSelected(AdapterView<?> view,View item,int position,long id){select.accept(position);}
        });
    }
    private TextView button(LinearLayout row,String name,Runnable action){
        TextView button=Aero.button(getContext(),name,action);button.setMinHeight(0);
        button.setPadding(dp(3),0,dp(3),0);button.setTextSize(12);
        LayoutParams bounds=new LayoutParams(0,dp(43),1);bounds.setMargins(dp(2),dp(2),dp(2),dp(2));row.addView(button,bounds);
        return button;
    }
    private void populate(int location){
        gallery.removeAllViews();boxes.clear();
        if(location==4){
            if(photos.isEmpty())gallery.addView(Aero.text(getContext(),"Browse for pictures or a folder to fill this collection.",13,Aero.INK));
            else group("My Pictures",new ArrayList<>(photos));
            return;
        }
        for(int group=0;group<DesktopBackgrounds.GROUPS.length;group++){
            if(location!=0&&group!=location)continue;
            List<String> pictures=new ArrayList<>();
            for(int id=0;id<DesktopBackgrounds.NAMES.length;id++)
                if(DesktopBackgrounds.group(id)==group)pictures.add(DesktopBackgrounds.stock(id));
            group(DesktopBackgrounds.GROUPS[group],pictures);
        }
    }
    private void group(String title,List<String> pictures){
        TextView label=Aero.text(getContext(),title+" ("+pictures.size()+")",13,0xff255980);
        label.setPadding(dp(4),dp(6),0,dp(5));gallery.addView(label);
        GridLayout grid=new GridLayout(getContext());grid.setColumnCount(getResources().getConfiguration().screenWidthDp>=600?4:3);
        gallery.addView(grid,new LayoutParams(-1,-2));
        for(String key:pictures){
            int id=DesktopBackgrounds.stockId(key);
            String name=id>=0?DesktopBackgrounds.NAMES[id]:"Picture "+(new ArrayList<>(photos).indexOf(key)+1);
            LinearLayout tile=Aero.column(getContext());tile.setBackground(Aero.selection(getContext()));
            FrameLayout art=new FrameLayout(getContext());art.setBackgroundColor(0xffdfeaf2);
            if(id>=0)art.addView(new AeroWallpaper(getContext(),id,false),new FrameLayout.LayoutParams(-1,-1));
            else{
                ImageView image=new ImageView(getContext());image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                art.addView(image,new FrameLayout.LayoutParams(-1,-1));loadThumbnail(key,image);
            }
            CheckBox check=new CheckBox(getContext());
            check.setContentDescription("Include background: "+name);
            check.setChecked(selected.contains(key));boxes.put(key,check);
            check.setBackgroundColor(0xc8f7fcff);
            FrameLayout.LayoutParams corner=new FrameLayout.LayoutParams(dp(32),dp(31),Gravity.TOP|Gravity.START);
            art.addView(check,corner);tile.addView(art,new LayoutParams(-1,dp(61)));
            TextView caption=Aero.text(getContext(),name,11,Aero.INK);caption.setGravity(Gravity.CENTER);
            caption.setSingleLine();caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(caption,new LayoutParams(-1,dp(25)));
            tile.setContentDescription("Preview background: "+name);tile.setFocusable(true);
            tile.setOnClickListener(v->{selected.clear();selected.add(key);current=key;syncSelection();});
            check.setOnCheckedChangeListener((v,checked)->{
                if(syncing)return;
                if(checked&&selected.size()<DesktopBackgrounds.LIMIT){selected.add(key);current=key;}
                else if(!checked)selected.remove(key);
                if(!selected.isEmpty()&&!selected.contains(current))current=selected.iterator().next();
                syncSelection();
            });
            GridLayout.LayoutParams size=new GridLayout.LayoutParams();size.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            size.width=0;size.setMargins(dp(3),0,dp(3),dp(5));grid.addView(tile,size);
        }
    }
    private void syncSelection(){
        syncing=true;for(Map.Entry<String,CheckBox> entry:boxes.entrySet())entry.getValue().setChecked(selected.contains(entry.getKey()));syncing=false;
        put("selected",new JSONArray(selected));
        summary.setText(selected.size()+" selected");
        interval.setEnabled(selected.size()>1);shuffle.setEnabled(selected.size()>1);
        showPreview();
    }
    private void showPreview(){
        if(closed)return;
        if(request!=null)request.cancel();
        int id=DesktopBackgrounds.stockId(current);
        String position=draft.optString("position","Fill");
        int color=COLORS[Math.max(0,Math.min(COLORS.length-1,draft.optInt("background",0)))];
        pictureReady=id>=0;save.setEnabled(pictureReady&&!selected.isEmpty());
        if(id>=0){preview.draft(id,null,0,0,position,color);return;}
        String key=current;
        CancellationSignal signal=new CancellationSignal();request=signal;
        worker.execute(()->{
            PhotoImage image=decode(key,signal,1024,800000);
            main.post(()->{
                if(closed||signal.isCanceled()){if(image!=null)image.bitmap.recycle();return;}
                pictureReady=image!=null;
                if(pictureReady)preview.draft(AeroWallpaper.SEVEN,image.bitmap,image.width,image.height,position,color);
                else summary.setText("Picture unavailable. Choose it again with Browse.");
                save.setEnabled(pictureReady&&!selected.isEmpty());
            });
        });
    }
    private void loadThumbnail(String key,ImageView target){
        Bitmap saved=thumbs.get(key);
        if(saved!=null){target.setImageBitmap(saved);return;}
        worker.execute(()->{
            PhotoImage image=decode(key,thumbnails,240,64000);
            if(image==null)return;
            main.post(()->{
                if(closed){image.bitmap.recycle();return;}
                thumbs.put(key,image.bitmap);target.setImageBitmap(image.bitmap);
            });
        });
    }
    private PhotoImage decode(String key,CancellationSignal signal,int side,int pixels){
        try{return PhotoImage.read(getContext().getApplicationContext(),Uri.parse(key),signal,side,pixels);}
        catch(Exception|OutOfMemoryError ignored){return null;}
    }
    @Override protected void onDetachedFromWindow(){
        closed=true;if(request!=null)request.cancel();thumbnails.cancel();worker.shutdownNow();
        main.removeCallbacksAndMessages(null);preview.close();thumbs.clear();super.onDetachedFromWindow();
    }
}
