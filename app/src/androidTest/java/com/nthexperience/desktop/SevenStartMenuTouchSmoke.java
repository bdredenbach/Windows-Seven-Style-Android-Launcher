package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Sends real pointer events through the actual Seven Start menu and a disposable Android app. */
public final class SevenStartMenuTouchSmoke extends Instrumentation {
    private static final String PKG="com.nthexperience.desktop.fixture",ID=PKG+"/"+PKG+".TaskActivity";
    private MainActivity home;private View root;private StartMenu menu;private ListView list;private DesktopStore store;
    private List<AppCatalog.Entry> catalog;private ActivityMonitor launches;private long down;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle out=new Bundle();boolean passed=false;try{
        Context c=getTargetContext();store=new DesktopStore(c);store.prefs.edit().clear().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("sidebar_visible",false).putBoolean("taskbar_apps",false).commit();
        store.startPins(Arrays.asList(ID,"@notes","@files"));c.getSharedPreferences(InitializationState.PREFS,0).edit().putInt("flow_version",2).putInt("step",InitializationState.Step.DONE.ordinal()).commit();
        c.getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();c.getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(c,MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();menu=find(root,StartMenu.class);list=find(menu,ListView.class);
        await(()->home.hasWindowFocus()&&!scanning(),"Desktop and initial catalog settle");catalog=AppCatalog.load(home);check(catalog.stream().anyMatch(e->e.id.equals(ID)),"Disposable Android launcher app is installed");
        IntentFilter filter=new IntentFilter(Intent.ACTION_MAIN);filter.addCategory(Intent.CATEGORY_LAUNCHER);launches=addMonitor(filter,null,false);
        for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}){
            boolean portrait=orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;ui(()->home.setRequestedOrientation(orientation));await(()->portrait?root.getHeight()>root.getWidth():root.getWidth()>root.getHeight(),"Desktop rotation");
            for(int area=0;area<3;area++){
                show();View row=row(ID);Rect rect=bounds(row);float x=area==0?rect.left+dp(14):area==1?rect.centerX():rect.right-dp(7);int before=launches.getHits();tap(x,rect.centerY(),false,MotionEvent.BUTTON_PRIMARY);
                awaitPackage(PKG,area==0?"Tap on pinned app icon opens installed app":"Tap on app text or row padding opens installed app");check(launches.getHits()==before+1,"One Android launch per short tap");backToHome();
            }
            show();View row=row(ID);int before=launches.getHits();hold(row);context(true);check(launches.getHits()==before,"Holding opens options without launching");shot("start-touch-"+(portrait?"portrait":"landscape")+"-menu");dismiss();
            show();row=row(ID);Rect rect=bounds(row);tap(rect.centerX(),rect.centerY(),true,MotionEvent.BUTTON_SECONDARY);context(true);check(launches.getHits()==before,"Secondary mouse button opens options without launching");dismiss();
            show();row=row(ID);rect=bounds(row);tap(rect.centerX(),rect.centerY(),true,MotionEvent.BUTTON_PRIMARY);awaitPackage(PKG,"Primary mouse click opens app");check(launches.getHits()==before+1,"Primary mouse launch occurs once");backToHome();
            show();row=row(ID);rect=bounds(row);before=launches.getHits();down=SystemClock.uptimeMillis();pointer(MotionEvent.ACTION_DOWN,rect.centerX(),rect.centerY(),false,0);Thread.sleep(120);pointer(MotionEvent.ACTION_CANCEL,rect.centerX(),rect.centerY(),false,0);Thread.sleep(ViewConfiguration.getLongPressTimeout()+150);check(home.hasWindowFocus()&&menu.getVisibility()==View.VISIBLE&&launches.getHits()==before,"Canceled press neither launches nor opens options");
            // Exercise the All Programs group rows and built-in shortcut rows too.
            show();tapView(text(menu,"All Programs  ›"));tapView(row("start-group:Accessories"));tapView(row("@notes"));await(()->window("@notes")!=null,"Accessories Notepad opens from its row");ui(()->window("@notes").closeWindow());
            show();hold(row("@notes"));context(false);dismiss();
            show();EditText search=find(menu,EditText.class);ui(()->search.setText("Task switch probe"));hold(row(ID));context(true);check(launches.getHits()==before,"Search-result hold does not launch");clickSystem("Open");awaitPackage(PKG,"Context menu Open launches the selected app");check(launches.getHits()==before+1,"Context Open starts the selected app exactly once");backToHome();
            scrollWithoutActivation();
        }
        passed=true;out.putString("stream","PASS: real finger taps on pinned app icons, labels and row padding launch once; touch-hold and secondary mouse options do not launch; primary mouse opens once; canceled press and list scrolling do not activate apps; All Programs/Accessories, built-in Notepad and search-result rows work; context Open targets the selected app; portrait and landscape.\n");
    }catch(Throwable error){try{shot("start-touch-failure");}catch(Throwable ignored){}out.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));}finally{if(launches!=null)removeMonitor(launches);}finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,out);}
    private void scrollWithoutActivation()throws Exception{
        List<AppCatalog.Entry> overflow=new ArrayList<>(catalog);for(int i=0;i<60;i++)overflow.add(new AppCatalog.Entry("scroll.probe"+i+"/.Main",String.format(Locale.ROOT,"Scroll probe %02d",i),new ColorDrawable(Color.BLUE)));
        show();ui(()->menu.entries(overflow));tapView(text(menu,"All Programs  ›"));await(()->list.getChildCount()>0,"Scrollable app list");int before=launches.getHits();Rect rect=bounds(list);float x=rect.centerX(),start=rect.bottom-dp(35),end=rect.top+dp(35);down=SystemClock.uptimeMillis();pointer(0,x,start,false,0);Thread.sleep(50);for(int i=1;i<=10;i++){pointer(2,x,start+(end-start)*i/10f,false,0);Thread.sleep(25);}pointer(1,x,end,false,0);Thread.sleep(350);check(list.getFirstVisiblePosition()>0,"Finger swipe scrolls program rows");check(home.hasWindowFocus()&&menu.getVisibility()==View.VISIBLE&&launches.getHits()==before,"Scrolling cancels row activation and hold");ui(()->{menu.hide();menu.entries(catalog);});
    }
    private boolean scanning(){try{Field f=MainActivity.class.getDeclaredField("scanPending");f.setAccessible(true);return f.getBoolean(home);}catch(Exception e){throw new AssertionError(e);}}
    private void show()throws Exception{ui(()->{menu.hide();menu.entries(catalog);});await(home::hasWindowFocus,"Desktop is ready for Start input");waitForIdleSync();Thread.sleep(250);tapView(description(root,"Start menu"));await(()->menu.getVisibility()==View.VISIBLE&&list.getChildCount()>0,"Start menu opens");}
    private View row(String id)throws Exception{int[] index={-1};ui(()->{for(int i=0;i<list.getAdapter().getCount();i++)if(id.equals(((StartSearch.Result)list.getAdapter().getItem(i)).id)){index[0]=i;break;}check(index[0]>=0,"Start entry exists: "+id);list.setSelection(index[0]);});await(()->index[0]>=list.getFirstVisiblePosition()&&index[0]<list.getFirstVisiblePosition()+list.getChildCount(),"Program row becomes visible");waitForIdleSync();return list.getChildAt(index[0]-list.getFirstVisiblePosition());}
    private void hold(View view)throws Exception{Rect r=bounds(view);down=SystemClock.uptimeMillis();pointer(0,r.centerX(),r.centerY(),false,0);Thread.sleep(ViewConfiguration.getLongPressTimeout()+180);pointer(1,r.centerX(),r.centerY(),false,0);Thread.sleep(200);}
    private void tapView(View view)throws Exception{check(view!=null,"Visible control exists");Rect r=bounds(view);tap(r.centerX(),r.centerY(),false,MotionEvent.BUTTON_PRIMARY);}
    private void tap(float x,float y,boolean mouse,int button)throws Exception{down=SystemClock.uptimeMillis();pointer(0,x,y,mouse,button);Thread.sleep(65);pointer(1,x,y,mouse,0);waitForIdleSync();Thread.sleep(150);}
    private void pointer(int action,float x,float y,boolean mouse,int buttons){MotionEvent.PointerProperties pp=new MotionEvent.PointerProperties();pp.id=0;pp.toolType=mouse?MotionEvent.TOOL_TYPE_MOUSE:MotionEvent.TOOL_TYPE_FINGER;MotionEvent.PointerCoords pc=new MotionEvent.PointerCoords();pc.x=x;pc.y=y;pc.pressure=1;pc.size=1;MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,new MotionEvent.PointerProperties[]{pp},new MotionEvent.PointerCoords[]{pc},0,buttons,1,1,0,0,mouse?InputDevice.SOURCE_MOUSE:InputDevice.SOURCE_TOUCHSCREEN,0);try{check(getUiAutomation().injectInputEvent(event,true),"Physical pointer injection");}finally{event.recycle();}}
    private void context(boolean installed)throws Exception{awaitSystem("Open");check((node(getUiAutomation().getRootInActiveWindow(),"Uninstall")!=null)==installed,"Correct app options and protected built-in filtering");}
    private void dismiss()throws Exception{check(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK),"Dismiss context menu");await(home::hasWindowFocus,"One Back dismisses the single options menu");}
    private void backToHome()throws Exception{check(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK),"Return from disposable Android app");await(home::hasWindowFocus,"Return to Seven");}
    private void awaitPackage(String pkg,String message)throws Exception{await(()->{AccessibilityNodeInfo n=getUiAutomation().getRootInActiveWindow();return n!=null&&pkg.contentEquals(n.getPackageName());},message);}
    private void await(BooleanSupplier condition,String message)throws Exception{long until=SystemClock.uptimeMillis()+15000;while(SystemClock.uptimeMillis()<until){waitForIdleSync();if(condition.getAsBoolean())return;Thread.sleep(100);}throw new AssertionError(message);}
    private AccessibilityNodeInfo node(AccessibilityNodeInfo n,String text){if(n==null)return null;if(text.contentEquals(n.getText()==null?"":n.getText())||text.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=node(n.getChild(i),text);if(found!=null)return found;}return null;}
    private AccessibilityNodeInfo awaitSystem(String label)throws Exception{AccessibilityNodeInfo[] result={null};await(()->(result[0]=node(getUiAutomation().getRootInActiveWindow(),label))!=null,"Context command: "+label);return result[0];}
    private void clickSystem(String label)throws Exception{AccessibilityNodeInfo n=awaitSystem(label);for(int i=0;n!=null&&i<6;i++,n=n.getParent())if(n.isClickable()&&n.performAction(AccessibilityNodeInfo.ACTION_CLICK)){waitForIdleSync();return;}throw new AssertionError("Context command clicks: "+label);}
    private DesktopWindow window(String id){return (DesktopWindow)findWhere(root,v->v instanceof DesktopWindow&&id.equals(((DesktopWindow)v).toolId));}
    private View description(View view,String value){return findWhere(view,v->value.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()));}
    private View text(View view,String value){return findWhere(view,v->v instanceof TextView&&value.contentEquals(((TextView)v).getText()));}
    private static View findWhere(View v,java.util.function.Predicate<View> test){if(v==null)return null;if(test.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=findWhere(((ViewGroup)v).getChildAt(i),test);if(found!=null)return found;}return null;}
    private static <T> T find(View v,Class<T> type){return type.cast(findWhere(v,type::isInstance));}
    private Rect bounds(View view){int[] p=new int[2];view.getLocationOnScreen(p);return new Rect(p[0],p[1],p[0]+view.getWidth(),p[1]+view.getHeight());}
    private int dp(float value){return Aero.dp(home,value);}
    private void ui(Runnable runnable){runOnMainSync(runnable);waitForIdleSync();}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private void shot(String name)throws Exception{Bitmap[] bitmap={null};ui(()->{bitmap[0]=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap[0]));});try(OutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name+".png"))){bitmap[0].compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap[0].recycle();}if(Build.VERSION.SDK_INT==30){Bitmap screen=getUiAutomation().takeScreenshot();if(screen!=null)try(OutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name+"-system.png"))){screen.compress(Bitmap.CompressFormat.PNG,100,out);}finally{screen.recycle();}}}
}
