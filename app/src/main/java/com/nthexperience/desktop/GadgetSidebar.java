package com.nthexperience.desktop;

import android.app.ActivityManager;
import android.content.*;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class GadgetSidebar extends FrameLayout {
    interface Actions {void gadgetAction(String id);void sidebarChanged();void readingsChanged();}
    private final DesktopStore store;
    private final Actions actions;
    final WeatherClient weather;
    final FeedClient feeds;
    final GadgetHardware hardware;
    GadgetLayout layout;
    private final Map<String,FrameLayout> floating=new LinkedHashMap<>();
    private int hostWidth,hostHeight,floatingTop;
    static final class GadgetDrag {final String id;final float offsetX,offsetY;GadgetDrag(String id,View face){this.id=id;offsetX=face.getWidth()/2f;offsetY=face.getHeight()/2f;}}
    private final Map<String,GadgetView> faces=new LinkedHashMap<>();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService photos=Executors.newSingleThreadExecutor();
    private Bitmap photo;
    private boolean active,closed,weatherDetails,feedDetails,networkDetails,compassDetails;
    private String currentPhoto="";
    private long lastPhotoTime,lastMinute=-1;
    private int photoIndex;
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!active||closed||!anyVisible())return;
        long now=System.currentTimeMillis();GadgetView clock=faces.get("clock");if(clock!=null&&visible("clock"))clock.invalidate();
        for(String kind:new String[]{"worldclock","timer","stopwatch"}){GadgetView face=faces.get(kind);if(face!=null&&visible(kind))face.invalidate();}
        long minute=now/60000;if(minute!=lastMinute){lastMinute=minute;refreshReadings();weather.refresh(false);feeds.refresh(false);for(GadgetView v:faces.values())if(!v.kind.equals("clock"))v.invalidate();}
        if(visible("photo")&&now-lastPhotoTime>20000){lastPhotoTime=now;loadNextPhoto();}
        boolean seconds=(visible("clock")&&store.prefs.getBoolean("clock_seconds",true))||visible("worldclock")||visible("timer")||visible("stopwatch");main.postDelayed(this,seconds?1000-now%1000:60000-now%60000);
    }};
    GadgetSidebar(Context context,DesktopStore store,Actions actions,FrameLayout workspace,String layoutKey){
        super(context);
        this.store=store;this.actions=actions;
        layout=new GadgetLayout(store,layoutKey);
        setClipChildren(false);
        setContentDescription("Desktop floating gadgets");
        workspace.setOnDragListener((view,event)->desktopDrop(event));
        hardware=new GadgetHardware(context,this::refresh);
        weather=new WeatherClient(store,this::refresh);
        feeds=new FeedClient(store,this::refresh);
        rebuild();visibilityFromStore();
    }
    void rebuild(){
        if(closed)return;
        removeAllViews();faces.clear();floating.clear();
        for(String id:layout.ordered()){
            if(!GadgetCatalog.valid(id))continue;
            GadgetView face=new GadgetView(getContext(),id,store,weather);face.photo=photo;face.hardware=hardware;
            face.setOnClickListener(v->actions.gadgetAction(id));
            face.setOnLongClickListener(v->beginDrag(face,id));
            face.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){menu(face,id);return true;}return false;});
            FrameLayout holder=new FrameLayout(getContext());holder.setContentDescription(GadgetView.title(id)+" floating gadget");
            holder.addView(face,new FrameLayout.LayoutParams(dp(132),dp(GadgetView.height(id))));
            TextView options=Aero.text(getContext(),"⋮",20,0xffedf6ff);options.setGravity(Gravity.CENTER);options.setBackground(Aero.panel(getContext(),0x88415a6b,0x88304454,3,0x77b0c7d2));options.setContentDescription("Options for "+GadgetView.title(id)+" gadget");options.setOnClickListener(v->menu(face,id));
            holder.addView(options,new FrameLayout.LayoutParams(dp(24),dp(32),Gravity.TOP|Gravity.END));
            TextView close=Aero.text(getContext(),"×",18,0xffedf6ff);close.setGravity(Gravity.CENTER);close.setBackground(Aero.panel(getContext(),0x883f4d57,0x8827343f,2,0x7795acbb));close.setContentDescription("Close "+GadgetView.title(id)+" gadget");close.setFocusable(true);close.setOnClickListener(v->{List<String> ids=store.gadgets();ids.remove(id);store.gadgets(ids);rebuild();});holder.addView(close,new FrameLayout.LayoutParams(dp(24),dp(28),Gravity.TOP|Gravity.END));FrameLayout.LayoutParams tools=(FrameLayout.LayoutParams)options.getLayoutParams();tools.topMargin=dp(28);options.setLayoutParams(tools);options.setText("⚙");options.setTextSize(15);options.setFocusable(true);
            TextView move=Aero.text(getContext(),"⠿",18,0xffedf6ff);move.setGravity(Gravity.CENTER);move.setBackground(Aero.panel(getContext(),0x883f4d57,0x8827343f,2,0x7795acbb));move.setContentDescription("Move "+GadgetView.title(id)+" gadget");move.setFocusable(true);move.setOnLongClickListener(v->beginDrag(face,id));move.setOnClickListener(v->menu(face,id));FrameLayout.LayoutParams gripTools=new FrameLayout.LayoutParams(dp(24),dp(28),Gravity.TOP|Gravity.END);gripTools.topMargin=dp(60);holder.addView(move,gripTools);
            close.setVisibility(GONE);move.setVisibility(GONE);tools.topMargin=0;options.setLayoutParams(tools);Runnable showTools=()->{boolean show=close.getVisibility()!=VISIBLE;close.setVisibility(show?VISIBLE:GONE);move.setVisibility(show?VISIBLE:GONE);FrameLayout.LayoutParams place=(FrameLayout.LayoutParams)options.getLayoutParams();place.topMargin=show?dp(28):0;options.setLayoutParams(place);};options.setOnLongClickListener(v->{showTools.run();return true;});holder.setOnHoverListener((v,event)->{boolean show=event.getActionMasked()!=MotionEvent.ACTION_HOVER_EXIT;close.setVisibility(show?VISIBLE:GONE);move.setVisibility(show?VISIBLE:GONE);FrameLayout.LayoutParams place=(FrameLayout.LayoutParams)options.getLayoutParams();place.topMargin=show?dp(28):0;options.setLayoutParams(place);return false;});
            faces.put(id,face);
            floating.put(id,holder);
            addView(holder,new FrameLayout.LayoutParams(dp(156),dp(GadgetView.height(id))));
            TextView grip=Aero.text(getContext(),"◢",18,0xffe7f6ff);grip.setGravity(Gravity.CENTER);grip.setBackground(Aero.panel(getContext(),0x88415a6b,0x88304454,3,0x77b0c7d2));grip.setContentDescription("Resize "+GadgetView.title(id)+" gadget");
            holder.addView(grip,new FrameLayout.LayoutParams(dp(24),dp(32),Gravity.BOTTOM|Gravity.END));
            grip.setOnTouchListener(new View.OnTouchListener(){float downX,downY,initialScale,anchorX,anchorY;int initialW,initialH;
                public boolean onTouch(View v,MotionEvent event){
                    if(locked())return false;
                    if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getRawX();downY=event.getRawY();initialScale=(holder.getWidth()-dp(24))/(float)dp(132);anchorX=holder.getX();anchorY=holder.getY();initialW=holder.getWidth()-dp(24);initialH=holder.getHeight();holder.bringToFront();return true;}
                    if(event.getActionMasked()==MotionEvent.ACTION_MOVE){float factor=Math.max((initialW+event.getRawX()-downX)/Math.max(1,initialW),(initialH+event.getRawY()-downY)/Math.max(1,initialH));float size=initialScale*factor;int[] bounds=DesktopGeometry.gadget(hostWidth,hostHeight,dp(132),dp(GadgetView.height(id)),dp(24),size,0,0);applyBounds(id,size,DesktopGeometry.fraction(anchorX,hostWidth-bounds[2]),DesktopGeometry.fraction(anchorY,hostHeight-bounds[3]));return true;}
                    if(event.getActionMasked()==MotionEvent.ACTION_UP){float scale=(holder.getLayoutParams().width-dp(24))/(float)dp(132);layout.position(id,DesktopGeometry.fraction(holder.getX(),hostWidth-holder.getLayoutParams().width),DesktopGeometry.fraction(holder.getY(),hostHeight-holder.getLayoutParams().height),scale);v.performClick();return true;}
                    if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){arrange();return true;}return false;
                }
            });
        }
        arrange();refreshReadings();updateActivity();
    }
    void alignAdded(String id){
        if(!locked())layout.dock(id);
        rebuild();reveal();
    }
    void raise(){bringToFront();}
    void visible(boolean value){layout.visible(value);visibilityFromStore();actions.sidebarChanged();}
    void orientation(String key){
        layout.save();layout=new GadgetLayout(store,key);rebuild();visibilityFromStore();actions.sidebarChanged();
    }
    private int dp(int value){return Aero.dp(getContext(),value);}
    boolean locked(){return store.prefs.getBoolean("layout_locked",false);}
    private boolean beginDrag(GadgetView face,String id){if(locked()){Toast.makeText(getContext(),"Unlock the desktop layout in Gadgets to arrange it",Toast.LENGTH_SHORT).show();return true;}return face.startDragAndDrop(ClipData.newPlainText("gadget",id),new View.DragShadowBuilder(face),new GadgetDrag(id,face),0);}
    private boolean desktopDrop(DragEvent event){
        if(!(event.getLocalState() instanceof GadgetDrag)||locked())return false;
        GadgetDrag drag=(GadgetDrag)event.getLocalState();
        if(event.getAction()==DragEvent.ACTION_DROP){
            FrameLayout holder=floating.get(drag.id);if(holder==null)return false;
            float scale=(holder.getWidth()-dp(24))/(float)dp(132);
            int[] bounds=DesktopGeometry.gadget(hostWidth,hostHeight,dp(132),dp(GadgetView.height(drag.id)),dp(24),scale,0,0);
            float grid=store.prefs.getBoolean("snap_gadgets",true)?dp(8):0;
            float x=DesktopGeometry.snap(event.getX()-drag.offsetX,grid),y=DesktopGeometry.snap(event.getY()-floatingTop-drag.offsetY,grid);
            layout.position(drag.id,DesktopGeometry.fraction(x,hostWidth-bounds[2]),DesktopGeometry.fraction(y,hostHeight-bounds[3]),scale);main.post(this::arrange);
        }return true;
    }
    void workspaceSize(int width,int height,int top){
        if(width==hostWidth&&height==hostHeight&&top==floatingTop)return;
        hostWidth=width;hostHeight=height;floatingTop=top;arrange();
    }
    private void arrange(){
        if(hostWidth<1||hostHeight<1)return;
        List<String> automatic=new ArrayList<>();
        for(String id:floating.keySet()){
            if(layout.docked(id))automatic.add(id);
            else applyBounds(id,layout.scale(id),layout.x(id),layout.y(id));
        }
        if(automatic.isEmpty())return;
        int gap=dp(7), margin=dp(4);
        int total=0;
        for(String id:automatic)total+=dp(GadgetView.height(id));
        float scale=Math.max(.68f,Math.min(1,(hostHeight-2f*margin-gap*(automatic.size()-1))/Math.max(1,total)));
        int right=hostWidth-margin,y=margin,columns=1,used=margin;
        int cardWidth=Math.round(dp(132)*scale)+dp(24);
        for(String id:automatic){
            int height=Math.round(dp(GadgetView.height(id))*scale);
            if(used>margin&&used+height>hostHeight-margin){columns++;used=margin;}
            used+=height+gap;
        }
        int columnWidth=Math.min(cardWidth+gap,Math.max(1,(hostWidth-cardWidth-2*margin)/Math.max(1,columns-1)));
        for(String id:automatic){
            int[] bounds=DesktopGeometry.gadget(hostWidth,hostHeight,dp(132),dp(GadgetView.height(id)),dp(24),scale,0,0);
            if(y>margin&&y+bounds[3]>hostHeight-margin){right-=columnWidth;y=margin;}
            int x=Math.max(0,right-bounds[2]);
            applyBounds(id,scale,DesktopGeometry.fraction(x,hostWidth-bounds[2]),DesktopGeometry.fraction(y,hostHeight-bounds[3]));
            y+=bounds[3]+gap;
        }
    }
    private void applyBounds(String id,float scale,float x,float y){
        FrameLayout holder=floating.get(id);if(holder==null||hostWidth<1||hostHeight<1)return;
        int[] b=DesktopGeometry.gadget(hostWidth,hostHeight,dp(132),dp(GadgetView.height(id)),dp(24),scale,x,y);
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)holder.getLayoutParams();if(lp.width!=b[2]||lp.height!=b[3]){lp.width=b[2];lp.height=b[3];holder.setLayoutParams(lp);}holder.setX(b[0]);holder.setY(b[1]);
        GadgetView face=faces.get(id);FrameLayout.LayoutParams fp=(FrameLayout.LayoutParams)face.getLayoutParams();int fw=Math.max(1,b[2]-dp(24));if(fp.width!=fw||fp.height!=b[3]){fp.width=fw;fp.height=b[3];face.setLayoutParams(fp);}
    }
    private boolean visible(String id){return faces.containsKey(id)&&getVisibility()==VISIBLE;}
    private boolean anyVisible(){for(String id:faces.keySet())if(visible(id))return true;return false;}
    private void updateActivity(){hardware.active(active&&(visible("network")||visible("compass")||networkDetails||compassDetails),visible("network")||networkDetails,visible("compass")||compassDetails);weather.active(active&&(visible("weather")||weatherDetails));feeds.active(active&&(visible("feeds")||feedDetails));restartTick();}
    void details(boolean weatherOpen,boolean feedOpen,boolean networkOpen,boolean compassOpen){if(weatherDetails==weatherOpen&&feedDetails==feedOpen&&networkDetails==networkOpen&&compassDetails==compassOpen)return;weatherDetails=weatherOpen;feedDetails=feedOpen;networkDetails=networkOpen;compassDetails=compassOpen;updateActivity();}
    void refresh(){for(GadgetView v:faces.values())v.invalidate();actions.readingsChanged();}
    void setActive(boolean value){active=value;updateActivity();}
    private void restartTick(){main.removeCallbacks(tick);if(active&&!closed&&anyVisible())main.post(tick);}
    void visibilityFromStore(){setVisibility(layout.visible()?VISIBLE:GONE);updateActivity();}
    void reveal(){visible(true);}
    boolean ticking(){return active&&anyVisible();}
    void menu(GadgetView face,String id){
        DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Gadget menu");menu.item("Open "+GadgetView.title(id),"programs",()->actions.gadgetAction(id));
        if(!locked()){
            menu.item("Align at desktop edge","computer",!layout.docked(id),()->{layout.dock(id);arrange();});
            menu.submenu("Gadget size","appearance",()->{DesktopContextMenu sizes=new DesktopContextMenu(getContext(),"Gadget size");String[] labels={"Small","Normal","Large","Extra large"};float[] scales={.8f,1,1.35f,1.75f};for(int i=0;i<labels.length;i++){float scale=scales[i];sizes.checked(labels[i],Math.abs(layout.scale(id)-scale)<.01f,()->{FrameLayout holder=floating.get(id);layout.position(id,DesktopGeometry.fraction(holder.getX(),hostWidth-holder.getWidth()),DesktopGeometry.fraction(holder.getY(),hostHeight-holder.getHeight()),scale);arrange();});}ContextMenus.centered(face,(x,y)->ContextMenus.show(sizes,face,x,y));});
            menu.item("Drag to arrange","computer",()->beginDrag(face,id));
        }
        menu.separator();menu.item("Close gadget","delete",()->{List<String> ids=store.gadgets();ids.remove(id);store.gadgets(ids);rebuild();});ContextMenus.centered(face,(x,y)->ContextMenus.show(menu,face,x,y));
    }
    private void refreshReadings(){
        Intent battery=getContext().registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));int level=battery==null?-1:battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=battery==null?-1:battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        int percent=level<0||scale<=0?-1:Math.round(level*100f/scale);int used=-1;
        ActivityManager manager=(ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);if(manager!=null){ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();manager.getMemoryInfo(info);if(info.totalMem>0)used=Math.round((info.totalMem-info.availMem)*100f/info.totalMem);}
        for(GadgetView face:faces.values()){face.battery=percent;face.memory=used;}
    }
    void photosChanged(){currentPhoto="";photoIndex=0;loadNextPhoto();}
    private void loadNextPhoto(){
        List<String> uris=store.photos();if(uris.isEmpty())return;String uri=uris.get(photoIndex++%uris.size());if(uri.equals(currentPhoto))return;currentPhoto=uri;
        photos.execute(()->{Bitmap decoded=null;try{
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream in=getContext().getContentResolver().openInputStream(Uri.parse(uri))){BitmapFactory.decodeStream(in,null,options);}options.inSampleSize=1;while(options.outWidth/options.inSampleSize>900||options.outHeight/options.inSampleSize>900)options.inSampleSize*=2;options.inJustDecodeBounds=false;
            try(InputStream in=getContext().getContentResolver().openInputStream(Uri.parse(uri))){decoded=BitmapFactory.decodeStream(in,null,options);}
            if(decoded!=null){try(InputStream in=getContext().getContentResolver().openInputStream(Uri.parse(uri))){int orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);Matrix m=new Matrix();if(orientation==3)m.postRotate(180);if(orientation==6)m.postRotate(90);if(orientation==8)m.postRotate(270);if(orientation==2)m.postScale(-1,1);if(orientation==4)m.postScale(1,-1);if(orientation==5){m.postRotate(90);m.postScale(-1,1);}if(orientation==7){m.postRotate(270);m.postScale(-1,1);}if(!m.isIdentity()){Bitmap rotated=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),m,true);decoded=rotated;}}}
        }catch(IOException|RuntimeException ignored){}
            Bitmap result=decoded;main.post(()->{if(closed||!currentPhoto.equals(uri))return;photo=result;GadgetView face=faces.get("photo");if(face!=null){face.photo=result;face.invalidate();}});
        });
    }
    void close(){closed=true;active=false;main.removeCallbacksAndMessages(null);hardware.close();weather.close();feeds.close();photos.shutdownNow();}
}
