package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.json.*;

/** Exercises actual row controls, native measurement and finger-driven window/slot movement. */
public class SevenTaskbarRowsSmoke extends Instrumentation {
    MainActivity home;View root;DesktopStore store;long down;
    static final String LABEL="Expand taskbar to two rows";
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();boolean passed=false;try{
        store=new DesktopStore(getTargetContext());store.prefs.edit().clear().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("taskbar_apps",false).putBoolean("sidebar_visible",false).commit();
        store.quickLaunch(Arrays.asList("@notes","@files","@calendar","@photos","@computer","@browser"));
        store.desktop(Arrays.asList("@computer","@recycle","@notes","@calendar","@photos","@control","@browser","@weather","@taskmanager","@files"));
        getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();
        getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();
        getTargetContext().getSharedPreferences(InitializationState.PREFS,0).edit().putInt("flow_version",2).putInt("step",InitializationState.Step.DONE.ordinal()).commit();
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();
        await(()->find(root,v->"Start menu".equals(v.getContentDescription()))!=null,"Desktop starts");settle();
        check(!store.prefs.getBoolean("taskbar_expanded",false),"Fresh Seven uses one row");
        for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}){
            boolean portrait=orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            ui(()->{home.setRequestedOrientation(orientation);store.prefs.edit().putBoolean("taskbar_expanded",false).putInt("desktop_rows_portrait",8).putInt("desktop_rows_landscape",3).commit();refresh();find(root,v->v instanceof DesktopIconGrid).requestLayout();});
            await(()->portrait?root.getHeight()>root.getWidth():root.getWidth()>root.getHeight(),"Orientation");settle();
            ui(()->home.open("setting:page:taskbar"));settle();DesktopWindow control=window("@control");check(control!=null,"Taskbar Control Panel opens");FrameLayout workspace=(FrameLayout)control.getParent();
            Map<String,Rect> icons=icons();String positionKey="desktop_positions_"+(portrait?"portrait":"landscape"),savedPositions=store.prefs.getString(positionKey,"{}");List<String> order=keys();Map<Integer,Rect> controls=systemControls();int height=workspace.getHeight();
            for(int pass=0;pass<3;pass++){
                toggle(true);check(height-workspace.getHeight()==dp(48),"Expansion adds exactly one app row");check(controls.equals(systemControls()),"Start, separator, tray, clock and Show desktop stay at exactly the same bottom-row pixels");
                // Expanded work areas temporarily fit labels; the stored desktop layout stays intact.
                Map<String,Rect> expandedIcons=icons();check(savedPositions.equals(store.prefs.getString(positionKey,"{}")),"Temporary fitting does not rewrite saved coordinates");checkDesktopLabels();check(order.equals(keys()),"Expansion preserves slot order");
                ui(()->{TaskbarItems items=items();check(items.getHeight()==dp(96),"Two complete rows");boolean top=false,bottom=false;for(int i=0;i<items.getChildCount();i++){View child=items.getChildAt(i);check(child.getTop()==TaskbarItems.row(child)*dp(48),"Button uses its selected row");top|=child.getTop()==0;bottom|=child.getTop()==dp(48);}check(top&&bottom,"Running tools and idle pins occupy both rows");});
                for(int repeat=0;repeat<4;repeat++){ui(()->{root.requestApplyInsets();workspace.requestLayout();});settle();check(expandedIcons.equals(icons()),"Repeated layout does not bounce fitted desktop icons");check(controls.equals(systemControls()),"Repeated layouts keep system controls bottom anchored");}
                toggle(false);check(height==workspace.getHeight(),"Normal workspace returns");check(icons.equals(icons()),"Collapse preserves desktop icon geometry");check(controls.equals(systemControls()),"Collapse preserves all bottom-row system bounds");
            }
            View arrow=find(root,v->"Device controls".equals(v.getContentDescription()));ui(arrow::performClick);settle();Map<Integer,Rect> expandedTrayControls=systemControls();
            toggle(true);check(expandedTrayControls.equals(systemControls()),"Expanded notification icons stay bottom anchored");
            SystemTray expandedTray=(SystemTray)find(root,v->v instanceof SystemTray);for(int i=0;i<expandedTray.getChildCount();i++){View icon=expandedTray.getChildAt(i);if(icon.isShown())check(screen(icon).bottom<=screen(expandedTray).bottom&&screen(icon).top>=screen(expandedTray).top,"Notification button fits the bottom row");}
            toggle(false);check(expandedTrayControls.equals(systemControls()),"Expanded tray stays fixed while taskbar collapses");ui(arrow::performClick);settle();
            toggle(true);Map<String,Rect> positions=taskPositions();ui(()->home.open("@taskmanager"));settle();Map<String,Rect> added=taskPositions();for(String id:positions.keySet())check(positions.get(id).equals(added.get(id)),"Opening another task keeps existing row assignments: "+id);
            ui(()->window("@taskmanager").closeWindow());settle();
            // Real hold on Seven's taskbar grip, then a real context-menu action.
            View grip=find(root,v->"Taskbar commands".equals(v.getContentDescription()));Rect g=screen(grip);down=SystemClock.uptimeMillis();input(MotionEvent.ACTION_DOWN,g.centerX(),g.centerY());Thread.sleep(650);input(MotionEvent.ACTION_UP,g.centerX(),g.centerY());
            clickMenu();settle();check(!store.prefs.getBoolean("taskbar_expanded",true),"Hold-menu item collapses the bar");
            // Opening the properties page again must show the same value.
            ui(()->home.open("setting:page:taskbar"));settle();check(!((CheckBox)find(control.body,v->v instanceof CheckBox&&LABEL.contentEquals(((CheckBox)v).getText()))).isChecked(),"Control Panel mirrors the hold-menu switch");
            ui(()->control.arrange(dp(12),workspace.getHeight()-dp(portrait?230:195),Math.min(dp(310),workspace.getWidth()-dp(24)),dp(portrait?200:180)));settle();
            Rect before=screen(control);dragUp(control,dp(portrait?135:40));Rect moved=screen(control);check(moved.top<before.top-dp(portrait?105:20),"Finger moves caption upward");
            ui(()->{root.requestApplyInsets();workspace.requestLayout();});settle();check(moved.equals(screen(control)),"Released window survives repeated layouts");
            toggle(true);check(moved.equals(screen(control)),"Expanding does not reset the floating window");toggle(false);check(moved.equals(screen(control)),"Collapsing does not reset the floating window");
            Thread.sleep(350);Rect title=title(control);down=SystemClock.uptimeMillis();input(MotionEvent.ACTION_DOWN,title.centerX(),title.centerY());for(int move=0;move<3;move++){Thread.sleep(40);input(MotionEvent.ACTION_MOVE,title.centerX(),title.centerY()-dp(20));}settle();Rect live=screen(control);check(live.top<moved.top-dp(12),"Caption is actively moving before workspace resize");trace(control,"Before resize");
            toggle(true);trace(control,"After resize");check(live.equals(screen(control)),"Workspace resize preserves active caption drag");input(MotionEvent.ACTION_UP,title.centerX(),title.centerY()-dp(20));settle();trace(control,"After release");check(live.equals(screen(control)),"Release after workspace resize keeps the new position: expected="+live+" actual="+screen(control));
            Rect saved=screen(control);ui(()->{control.minimize();control.restore();control.maximize();});settle();check(control.getHeight()==workspace.getHeight(),"Maximized window fits extended taskbar");toggle(false);check(control.getHeight()==workspace.getHeight(),"Maximized window fits normal taskbar");ui(control::restoreDown);settle();check(saved.equals(screen(control)),"Restore remembers floating geometry");
            toggle(true);ui(()->{store.prefs.edit().putBoolean("taskbar_static",true).putBoolean("taskbar_show_labels",true).commit();refresh();});settle();
            ui(()->check(items().getWidth()<=((View)items().getParent()).getWidth(),"Static two-row labels fit without clipping"));
            ui(()->{store.prefs.edit().putBoolean("taskbar_static",false).putBoolean("taskbar_show_labels",false).commit();refresh();});settle();
            shot("seven-rows-"+(portrait?"portrait":"landscape"));ui(control::closeWindow);settle();
        }
        // Expanded physical drop across rows, then keyboard/accessibility and saved settings.
        ui(()->{store.quickLaunch(Arrays.asList("@notes","@files","@calendar","@photos"));store.taskbarOrder(Arrays.asList("@notes","@files","@calendar","@photos"));refresh();});settle();
        View first=slot(keys().get(0)),last=slot(keys().get(3));List<String> previous=keys();Rect a=screen(first),b=screen(last);
        down=SystemClock.uptimeMillis();input(MotionEvent.ACTION_DOWN,a.centerX(),a.centerY());Thread.sleep(210);input(MotionEvent.ACTION_MOVE,a.centerX()+dp(18),a.centerY());Thread.sleep(120);for(int i=1;i<=8;i++){input(MotionEvent.ACTION_MOVE,a.centerX()+(b.right-2-a.centerX())*i/8f,a.centerY()+(b.centerY()-a.centerY())*i/8f);Thread.sleep(35);}input(MotionEvent.ACTION_UP,b.right-2,b.centerY());settle();check(!previous.equals(keys()),"Physical drag can reorder across rows");
        ui(()->check(slot(keys().get(1)).performAccessibilityAction(R.id.taskbar_move_left,null),"Accessibility reorder works in expanded bar"));settle();
        List<String> explicitPins=Arrays.asList("@photos","@calendar","@files","@notes");
        ui(()->{store.quickLaunch(explicitPins);refresh();});settle();check(explicitPins.equals(keys()),"Explicit Quick Launch edits replace cached row ordering");
        JSONObject snapshot=DesktopBackup.snapshot(store);DesktopBackup.Plan plan=DesktopBackup.parse(snapshot.toString());check(Boolean.TRUE.equals(plan.values.get("taskbar_expanded")),"Backup carries second-row preference");snapshot.getJSONObject("settings").remove("taskbar_expanded");DesktopBackup.parse(snapshot.toString());
        MainActivity old=home;BlockingQueue<MainActivity> resumed=new LinkedBlockingQueue<>();Application app=(Application)getTargetContext().getApplicationContext();Application.ActivityLifecycleCallbacks callbacks=new Application.ActivityLifecycleCallbacks(){public void onActivityResumed(Activity a){if(a instanceof MainActivity&&a!=old)resumed.offer((MainActivity)a);}public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivityPaused(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}};app.registerActivityLifecycleCallbacks(callbacks);List<String> saved=keys();ui(home::recreate);home=resumed.poll(10,TimeUnit.SECONDS);app.unregisterActivityLifecycleCallbacks(callbacks);check(home!=null,"Recreated activity resumes");root=home.getWindow().getDecorView();settle();check(items().expanded&&saved.equals(keys()),"Rows and reordered slots survive recreation");
        result.putString("stream","PASS: one-row default; Control Panel and physical hold-menu switches; true two-row layout; stable desktop/custom rows and task order; repeated inset passes; finger-driven window movement before/during taskbar resize; maximize/restore; static labels; cross-row physical reorder; accessibility; backup and recreation; portrait/landscape.\n");passed=true;
    }catch(Throwable failure){try{shot("seven-rows-failure");}catch(Throwable ignored){}result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(failure));}finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
    void trace(DesktopWindow window,String label)throws Exception{ui(()->{String value=label+" bounds="+screen(window)+" geometry="+window.geometry();for(String name:new String[]{"captionTracking","captionDragging","gripX","gripY","floatingTop"})try{Field f=DesktopWindow.class.getDeclaredField(name);f.setAccessible(true);value+=" "+name+"="+f.get(window);}catch(Exception e){throw new RuntimeException(e);}Bundle b=new Bundle();b.putString("stream",value+"\n");sendStatus(1,b);});}
    void refresh(){try{Method method=MainActivity.class.getDeclaredMethod("vistaAction",String.class);method.setAccessible(true);method.invoke(home,"taskbar-refresh");}catch(Exception e){throw new RuntimeException(e);}}
    void toggle(boolean extended)throws Exception{ui(()->{DesktopWindow w=window("@control");CheckBox box=(CheckBox)find(w.body,v->v instanceof CheckBox&&LABEL.contentEquals(((CheckBox)v).getText()));check(box!=null&&box.isChecked()!=extended,"Control Panel switch ready");box.performClick();});settle();}
    DesktopWindow window(String id){return (DesktopWindow)find(root,v->v instanceof DesktopWindow&&id.equals(((DesktopWindow)v).toolId));}
    TaskbarItems items(){return (TaskbarItems)find(root,v->v instanceof TaskbarItems);}
    List<String> keys(){List<String> ids=new ArrayList<>();TaskbarItems items=items();for(int i=0;i<items.getChildCount();i++){String key=TaskbarReorder.key(items.getChildAt(i));if(key!=null)ids.add(key);}return ids;}
    View slot(String id){return find(items(),v->id.equals(TaskbarReorder.key(v)));}
    Map<Integer,Rect> systemControls(){Map<Integer,Rect> out=new LinkedHashMap<>();ViewGroup bar=(ViewGroup)find(root,v->"Desktop taskbar".equals(v.getContentDescription()));for(int i=0;i<bar.getChildCount();i++){View child=bar.getChildAt(i);if(find(child,v->v instanceof TaskbarItems)==null)out.put(i,screen(child));}return out;}
    Map<String,Rect> taskPositions(){Map<String,Rect> out=new LinkedHashMap<>();for(String id:keys()){View v=slot(id);out.put(id,new Rect(v.getLeft(),v.getTop(),v.getRight(),v.getBottom()));}return out;}
    Map<String,Rect> icons(){Map<String,Rect> out=new LinkedHashMap<>();DesktopIconGrid grid=(DesktopIconGrid)find(root,v->v instanceof DesktopIconGrid);for(int i=0;i<grid.getChildCount();i++){View v=grid.getChildAt(i);out.put(String.valueOf(v.getTag()),new Rect(v.getLeft(),v.getTop(),v.getRight(),v.getBottom()));}return out;}
    void checkDesktopLabels()throws Exception{ui(()->{try{DesktopIconGrid grid=(DesktopIconGrid)find(root,v->v instanceof DesktopIconGrid);Field field=DesktopIconGrid.class.getDeclaredField("viewportHeight");field.setAccessible(true);int viewport=field.getInt(grid);for(int i=0;i<grid.getChildCount();i++){ViewGroup tile=(ViewGroup)grid.getChildAt(i);if(tile.getTop()>=viewport)continue;check(tile.getBottom()<=viewport,"Visible desktop rows fit above the taskbar");if(tile.getChildCount()>1){View label=tile.getChildAt(1);check(label.getBottom()+tile.getPaddingBottom()<=tile.getHeight(),"Complete measured folder labels fit inside their row");}}}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}});}
    View find(View root,Predicate<View> match){if(root==null)return null;if(match.test(root))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View found=find(((ViewGroup)root).getChildAt(i),match);if(found!=null)return found;}return null;}
    AccessibilityNodeInfo menuNode(AccessibilityNodeInfo node){if(node==null)return null;if(LABEL.contentEquals(node.getContentDescription()==null?"":node.getContentDescription())&&node.isClickable())return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=menuNode(node.getChild(i));if(found!=null)return found;}return null;}
    void clickMenu()throws Exception{long end=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<end){AccessibilityNodeInfo node=menuNode(getUiAutomation().getRootInActiveWindow());if(node!=null){check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Menu control clicks");return;}Thread.sleep(100);}throw new AssertionError("Held taskbar menu appears");}
    void await(BooleanSupplier test,String label)throws Exception{long end=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<end){boolean[] done={false};ui(()->done[0]=test.getAsBoolean());if(done[0])return;Thread.sleep(80);}throw new AssertionError(label);}
    void ui(Runnable action)throws Exception{FutureTask<Void> task=new FutureTask<>(action,null);new Handler(Looper.getMainLooper()).post(task);try{task.get(8,TimeUnit.SECONDS);}catch(ExecutionException e){throw new RuntimeException(e.getCause());}}
    void settle()throws Exception{waitForIdleSync();Thread.sleep(240);waitForIdleSync();}
    Rect screen(View view){int[] at=new int[2];view.getLocationOnScreen(at);return new Rect(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight());}
    Rect title(DesktopWindow w){return screen(find(w,v->(w.caption()+". Drag to move window.").equals(v.getContentDescription())));}
    void dragUp(DesktopWindow w,int distance)throws Exception{Thread.sleep(350);Rect t=title(w);down=SystemClock.uptimeMillis();input(MotionEvent.ACTION_DOWN,t.centerX(),t.centerY());for(int i=1;i<=8;i++){Thread.sleep(20);input(MotionEvent.ACTION_MOVE,t.centerX(),t.centerY()-distance*i/8);}input(MotionEvent.ACTION_UP,t.centerX(),t.centerY()-distance);settle();}
    void input(int action,float x,float y){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(getUiAutomation().injectInputEvent(event,true),"Native pointer input including system drag handoff");}finally{event.recycle();}}
    int dp(float value){return Aero.dp(home,value);}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    void shot(String name)throws Exception{Bitmap[] image={null};ui(()->{image[0]=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(image[0]));});try(OutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name+".png"))){image[0].compress(Bitmap.CompressFormat.PNG,100,out);}finally{image[0].recycle();}}
}
