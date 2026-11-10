package com.nthexperience.desktop;

import android.app.*;import android.content.*;import android.content.pm.ActivityInfo;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;

/** Real swipe/drag regression for the owner's independently scrolling taskbar rows. */
public final class SevenIndependentRowsSmoke extends SevenTaskbarRowsSmoke {
    @Override public void onStart(){Bundle out=new Bundle();try{
        getUiAutomation().executeShellCommand("wm size 1080x2400").close();store=new DesktopStore(getTargetContext());
        store.prefs.edit().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("taskbar_static",false).putBoolean("taskbar_expanded",true).putBoolean("taskbar_show_labels",false).remove(TaskbarRows.KEY).commit();
        getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();
        getTargetContext().getSharedPreferences(InitializationState.PREFS,0).edit().putInt("flow_version",2).putInt("step",7).commit();
        getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).commit();
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();settle();
        for(String alignment:new String[]{"Aero","Light"}){
            ui(()->{closeAll();for(String id:ExternalTasks.list())ExternalTasks.remove(id);store.prefs.edit().remove(TaskbarRows.KEY).putBoolean("aero_transparency",alignment.equals("Aero")).putBoolean("taskbar_static",false).putBoolean("taskbar_show_labels",true).putBoolean("taskbar_expanded",true).commit();store.quickLaunch(Arrays.asList("@photos","@browser","@recycle","@calendar","@welcome","@clock"));refresh();for(String id:new String[]{"@notes","@system","@recent","@backup","@taskmanager"})home.open(id);});settle();
            ui(()->{TaskbarItems items=items();items.scrollRowTo(0,0);items.scrollRowTo(1,0);check(items.rowRange(0)>Aero.dp(home,60)&&items.rowRange(1)>Aero.dp(home,60),"Both rows actually overflow");});settle();
            Map<String,String> fixed=new LinkedHashMap<>();for(Map.Entry<String,View> e:fixedViews().entrySet())fixed.put(e.getKey(),bounds(e.getValue()));
            List<String> top=geometryRow(0),bottom=geometryRow(1);swipeRow(0);waitFling();
            check(items().rowScroll(0)>0,"Upper swipe moves upper row");check(geometryRow(1).equals(bottom),"Upper swipe leaves every bottom app stationary");
            top=geometryRow(0);swipeRow(1);waitFling();check(items().rowScroll(1)>0,"Lower swipe moves lower row");check(geometryRow(0).equals(top),"Lower swipe leaves every upper app stationary");
            for(Map.Entry<String,View> e:fixedViews().entrySet())check(fixed.get(e.getKey()).equals(bounds(e.getValue())),"Fixed controls never scroll: "+e.getKey());
            ui(()->{TaskbarItems items=items();items.stopScrolling();items.scrollRowTo(0,0);items.scrollRowTo(1,0);store.prefs.edit().putBoolean("taskbar_show_labels",false).commit();refresh();});settle();
            dragAcross("@photos",0);checkRow("@photos",0,"Idle pin dragged to top");
            dragAcross("@notes",1);checkRow("@notes",1,"Running app dragged to bottom");
            String saved=store.prefs.getString(TaskbarRows.KEY,"");
            ui(()->{home.open("@photos");try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("windows");f.setAccessible(true);Map<?,?> windows=(Map<?,?>)f.get(home);((DesktopWindow)windows.get("@notes")).closeWindow();}catch(Exception e){throw new RuntimeException(e);}});settle();
            checkRow("@photos",0,"Pin retains chosen row after launch");ui(()->home.open("@notes"));settle();checkRow("@notes",1,"Running app retains chosen bottom row after reopen");
            ui(this::toggleRows);settle();check(!items().expanded,"Collapsed mode remains one strip");ui(this::toggleRows);settle();checkRow("@photos",0,"Expand restores saved top choice");checkRow("@notes",1,"Expand restores saved bottom choice");
            ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();checkRow("@photos",0,"Top choice survives rotation");ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));settle();checkRow("@notes",1,"Bottom choice survives rotation");
            DesktopBackup.Plan plan=DesktopBackup.parse(DesktopBackup.snapshot(store).toString());check(saved.equals(plan.values.get(TaskbarRows.KEY)),"Portable backup retains row choices");
            ui(()->{try{store.prefs.edit().remove(TaskbarRows.KEY).commit();DesktopBackup.apply(home,store,plan,false);refresh();}catch(Exception e){throw new RuntimeException(e);}});settle();checkRow("@photos",0,"Actual backup restore recovers top choice");checkRow("@notes",1,"Actual backup restore recovers bottom choice");
            ui(()->{store.prefs.edit().putBoolean("taskbar_static",true).putBoolean("taskbar_show_labels",true).commit();refresh();});settle();
            ui(()->{TaskbarItems items=items();for(int i=0;i<items.getChildCount();i++){View child=items.getChildAt(i);int row=TaskbarItems.row(child);check(child.getLeft()>=items.rowStart(row)&&child.getRight()<=items.rowEnd(row),"Static fit respects each manually assigned row");}check(items.rowScroll(0)==0&&items.rowScroll(1)==0,"Static mode resets both scroll offsets");});
            shot("seven-independent-rows-"+alignment.toLowerCase(Locale.ROOT));
        }
        ui(()->home.finish());waitForIdleSync();home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();store=new DesktopStore(home);settle();checkRow("@photos",0,"Fresh Activity retains top choice");ui(()->home.open("@notes"));settle();checkRow("@notes",1,"Fresh Activity retains bottom choice");
        ui(()->{View notes=slot("@notes");check(notes.performAccessibilityAction(R.id.taskbar_move_top,null),"Accessible move-to-top action accepted");});settle();checkRow("@notes",0,"Accessibility row move persisted");
        ui(()->{View notes=slot("@notes");check(notes.requestFocusFromTouch(),"Task button receives keyboard focus");check(notes.dispatchKeyEvent(new KeyEvent(0,0,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN,0,KeyEvent.META_CTRL_ON|KeyEvent.META_SHIFT_ON)),"Keyboard row move accepted");});settle();checkRow("@notes",1,"Keyboard row move persisted");
        out.putString("stream","PASS: independent-row assertions; real upper/lower swipes leave all apps in the opposite row stationary, native drag/drop in both directions, default and saved placement through launch/close/reopen, collapse/expand, rotation, portable backup, static fitting, fixed controls and accessible/keyboard row moves.\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));try{shot("seven-independent-rows-failure");}catch(Exception ignored){}finish(Activity.RESULT_CANCELED,out);}}
    static void check(boolean ok,String message){SevenTaskbarRowsSmoke.check(ok,message);}
    void closeAll(){List<DesktopWindow> list=new ArrayList<>();collectWindows(root,list);for(DesktopWindow w:list)w.closeWindow();}
    void collectWindows(View v,List<DesktopWindow> list){if(v instanceof DesktopWindow)list.add((DesktopWindow)v);else if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collectWindows(((ViewGroup)v).getChildAt(i),list);}
    Map<String,View> fixedViews(){Map<String,View> out=new LinkedHashMap<>();ViewGroup bar=(ViewGroup)find(root,v->"Desktop taskbar".equals(v.getContentDescription()));for(int i=0;i<bar.getChildCount();i++){View v=bar.getChildAt(i);if(find(v,c->c instanceof TaskbarItems)==null)out.put("control"+i,v);}return out;}
    String bounds(View v){return screen(v).toShortString();}
    void toggleRows(){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("setTaskbarExpanded",boolean.class);m.setAccessible(true);m.invoke(home,!items().expanded);}catch(Exception e){throw new RuntimeException(e);}}
    List<String> geometryRow(int row){List<String> result=new ArrayList<>();TaskbarItems items=items();for(int i=0;i<items.getChildCount();i++){View c=items.getChildAt(i);if(TaskbarItems.row(c)==row)result.add(TaskbarReorder.key(c)+":"+bounds(c));}return result;}
    void checkRow(String id,int row,String message)throws Exception{ui(()->{View child=slot(id);check(child!=null&&TaskbarItems.row(child)==row&&child.getTop()==row*Aero.dp(home,48),message);});}
    void touch(int action,float x,float y,long down){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"Native row input accepted");event.recycle();}
    void timedTouch(int action,float x,float y,long down,long time){MotionEvent event=MotionEvent.obtain(down,time,action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"Timed native row swipe accepted");event.recycle();}
    void swipeRow(int row)throws Exception{TaskbarItems items=items();int[] at=new int[2];items.getLocationOnScreen(at);float start=at[0]+items.rowEnd(row)-Aero.dp(home,8),end=at[0]+items.rowStart(row)+Aero.dp(home,8),y=at[1]+Aero.dp(home,row*48+24);long now=SystemClock.uptimeMillis();timedTouch(MotionEvent.ACTION_DOWN,start,y,now,now);for(int i=1;i<=6;i++)timedTouch(MotionEvent.ACTION_MOVE,start+(end-start)*i/6,y,now,now+i*14);timedTouch(MotionEvent.ACTION_UP,end,y,now,now+98);}

    void waitFling()throws Exception{Thread.sleep(700);ui(()->items().stopScrolling());settle();}
    void dragAcross(String id,int row)throws Exception{
        ui(()->{TaskbarItems items=items();items.stopScrolling();items.scrollRowTo(0,0);items.scrollRowTo(1,0);});settle();
        View source=slot(id);int[] at=new int[2],origin=new int[2];source.getLocationOnScreen(origin);TaskbarItems items=items();items.getLocationOnScreen(at);float x=origin[0]+source.getWidth()/2f,y=origin[1]+source.getHeight()/2f,targetX=at[0]+items.rowStart(row)+Aero.dp(home,16),targetY=at[1]+Aero.dp(home,row*48+24);long down=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,x,y,down);Thread.sleep(185);touch(MotionEvent.ACTION_MOVE,x+Aero.dp(home,14),y,down);Thread.sleep(120);
        for(int i=1;i<=5;i++){touch(MotionEvent.ACTION_MOVE,x+(targetX-x)*i/5,y+(targetY-y)*i/5,down);Thread.sleep(35);}touch(MotionEvent.ACTION_UP,targetX,targetY,down);Thread.sleep(150);settle();
    }
}
