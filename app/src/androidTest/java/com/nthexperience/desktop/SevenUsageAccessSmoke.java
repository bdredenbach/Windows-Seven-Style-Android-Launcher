package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Uses actual Android Settings and App info in an isolated full-system emulator. */
public final class SevenUsageAccessSmoke extends Instrumentation {
    private MainActivity home;private View root;private DesktopStore store;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle out=new Bundle();boolean passed=false;try{
        Context context=getTargetContext();store=new DesktopStore(context);
        store.prefs.edit().clear().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("taskbar_apps",false).putString("weather_city","Saved city").commit();
        context.getSharedPreferences(InitializationState.PREFS,0).edit().clear().commit();
        context.getSharedPreferences(StorageAccess.PREFS,0).edit().clear().commit();
        context.getSharedPreferences(StartupExperience.SESSION,0).edit().clear().putBoolean("seen",true).putBoolean("welcome_seen",false).commit();
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(context,MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();
        for(String title:new String[]{"Default Launcher","Set up Nth Explorer","Set up Usage Access","Allow Notifications Access","Modify System Settings (Brightness)"}){
            awaitSystem(title);check(find(root,WelcomeCenter.class)!=null,"Seven Welcome Center remains behind ordered setup");
            if(title.equals("Set up Nth Explorer")){shot("seven-usage-files-portrait");recreateHome();awaitSystem(title);}
            if(title.equals("Set up Usage Access")){
                settingsReturn(false);awaitSystem(title);check(step()==InitializationState.Step.USAGE,"Ungranted Android Settings return retains Usage Access");
                shot("seven-usage-return-portrait");recreateHome();awaitSystem(title);check(step()==InitializationState.Step.USAGE,"Returned guide survives recreation");
                // Repeat the actual ungranted visit; it must wait for another user choice.
                settingsReturn(false);awaitSystem(title);check(step()==InitializationState.Step.USAGE,"Repeated denial does not advance setup");
                appInfoReturn();awaitSystem(title);check(step()==InitializationState.Step.USAGE,"App info return does not advance or relaunch Usage Access");
                rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,false);awaitSystem(title);buttonsFit();shot("seven-usage-return-landscape");
                rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,true);awaitSystem(title);buttonsFit();
            }
            click("Later");
        }
        awaitSystem("Allow Location");click("Later");await(()->step()==InitializationState.Step.DONE&&home.hasWindowFocus(),"Explicit Later completes optional setup");
        check(!StorageAccess.has(home)&&!ExternalTasks.permitted(home),"Declines leave file and Usage Access off");
        check("Saved city".equals(store.prefs.getString("weather_city","")),"Setup preserves the saved weather city");
        recreateHome();await(home::hasWindowFocus,"Completed setup stays completed after recreation");check(step()==InitializationState.Step.DONE,"No repeated initialization");
        // Exercise the existing manual Welcome Center entry for already initialized users.
        ui(()->home.open("@welcome"));View action=findText(find(root,WelcomeCenter.class),"Set up Usage Access");check(action!=null,"Welcome Center keeps the manual Usage Access route");
        ui(()->{View current=action;while(current!=null&&!current.isClickable())current=current.getParent() instanceof View?(View)current.getParent():null;check(current!=null&&current.performClick(),"Manual setup action opens guide");});
        awaitSystem("Set up Usage Access");settingsReturn(false);awaitSystem("Set up Usage Access");appInfoReturn();awaitSystem("Set up Usage Access");
        recreateHome();awaitSystem("Set up Usage Access");check(step()==InitializationState.Step.DONE,"Manual guide recreation preserves completed initialization");
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,false);awaitSystem("Set up Usage Access");buttonsFit();shot("seven-usage-manual-landscape");
        click("Cancel");await(home::hasWindowFocus,"Manual guide can be canceled");check(step()==InitializationState.Step.DONE,"Cancel does not restart setup");
        // Android AppOps changes the actual permission; continuation must use the real helper.
        context.getSharedPreferences(InitializationState.PREFS,0).edit().putInt("step",InitializationState.Step.USAGE.ordinal()).commit();recreateHome();awaitSystem("Set up Usage Access");
        settingsReturn(true);awaitSystem("Allow Notifications Access");check(ExternalTasks.permitted(home)&&ExternalTasks.includesExternalApps(store,true),"A real Android grant refreshes taskbar inclusion and continues setup");
        for(String title:new String[]{"Allow Notifications Access","Modify System Settings (Brightness)","Allow Location"}){awaitSystem(title);click("Later");}
        await(()->step()==InitializationState.Step.DONE,"Granted Usage Access continues through remaining setup");
        passed=true;out.putString("stream","PASS: actual Android Usage Access Back without a grant twice; illustrated Seven guide and App info recovery retained; correct Android App info identity; portrait/landscape controls; recreation; explicit Later; completed/manual setup route and Cancel; Android AppOps grant recognized by the real permission helper and taskbar inclusion; saved city preserved.\n");
    }catch(Throwable failure){try{shot("seven-usage-failure");}catch(Throwable ignored){}out.putString("stream","FAIL: "+android.util.Log.getStackTraceString(failure));}finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,out);}
    private InitializationState.Step step(){return new InitializationState(getTargetContext()).step();}
    private void settingsReturn(boolean grant)throws Exception{click("OK");awaitPackage("com.android.settings");check(!ExternalTasks.permitted(home),"Usage Access is off before visiting Settings");if(grant)shell("appops set "+getTargetContext().getPackageName()+" GET_USAGE_STATS allow");check(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK),"Back from actual Usage Access screen");}
    private void appInfoReturn()throws Exception{if(Build.VERSION.SDK_INT<33)return;click("Open App info");awaitPackage("com.android.settings");awaitSystem(home.getString(R.string.app_name));shot("seven-usage-app-info");check(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK),"Back from this app's Android App info");}
    private void rotate(int orientation,boolean portrait)throws Exception{ui(()->home.setRequestedOrientation(orientation));await(()->portrait?root.getHeight()>root.getWidth():root.getWidth()>root.getHeight(),"Guide rotates with desktop");Thread.sleep(350);}
    private void buttonsFit()throws Exception{int[] at=new int[2];root.getLocationOnScreen(at);Rect screen=new Rect(at[0],at[1],at[0]+root.getWidth(),at[1]+root.getHeight());for(String label:new String[]{"OK","Open App info","Later","Cancel"}){AccessibilityNodeInfo control=node(getUiAutomation().getRootInActiveWindow(),label);if(control==null)continue;Rect bounds=new Rect();control.getBoundsInScreen(bounds);check(screen.contains(bounds)&&!bounds.isEmpty(),"Guide control stays inside visible screen: "+label);}}
    private AccessibilityNodeInfo node(AccessibilityNodeInfo current,String text){if(current==null)return null;if(text.equalsIgnoreCase(String.valueOf(current.getText()))||text.equalsIgnoreCase(String.valueOf(current.getContentDescription())))return current;for(int i=0;i<current.getChildCount();i++){AccessibilityNodeInfo found=node(current.getChild(i),text);if(found!=null)return found;}return null;}
    private AccessibilityNodeInfo awaitSystem(String label)throws Exception{long end=SystemClock.uptimeMillis()+20000;while(SystemClock.uptimeMillis()<end){AccessibilityNodeInfo found=node(getUiAutomation().getRootInActiveWindow(),label);if(found!=null)return found;Thread.sleep(100);}throw new AssertionError("Visible native control: "+label);}
    private void awaitPackage(String pkg)throws Exception{long end=SystemClock.uptimeMillis()+20000;while(SystemClock.uptimeMillis()<end){AccessibilityNodeInfo window=getUiAutomation().getRootInActiveWindow();if(window!=null&&pkg.contentEquals(window.getPackageName()))return;Thread.sleep(100);}throw new AssertionError("Actual Android activity: "+pkg);}
    private void click(String label)throws Exception{AccessibilityNodeInfo item=awaitSystem(label);for(int i=0;item!=null&&i<6;i++,item=item.getParent())if(item.isClickable()&&item.performAction(AccessibilityNodeInfo.ACTION_CLICK)){waitForIdleSync();Thread.sleep(200);return;}throw new AssertionError("Native control is clickable: "+label);}
    private void recreateHome()throws Exception{MainActivity previous=home;BlockingQueue<MainActivity> resumed=new LinkedBlockingQueue<>();Application app=(Application)getTargetContext().getApplicationContext();Application.ActivityLifecycleCallbacks listener=new Application.ActivityLifecycleCallbacks(){public void onActivityResumed(Activity a){if(a instanceof MainActivity&&a!=previous)resumed.offer((MainActivity)a);}public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivityPaused(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}};app.registerActivityLifecycleCallbacks(listener);try{ui(home::recreate);home=resumed.poll(20,TimeUnit.SECONDS);check(home!=null,"Main activity recreates");root=home.getWindow().getDecorView();}finally{app.unregisterActivityLifecycleCallbacks(listener);}}
    private void ui(Runnable runnable){runOnMainSync(runnable);waitForIdleSync();}
    private void await(BooleanSupplier condition,String message)throws Exception{long end=SystemClock.uptimeMillis()+20000;while(SystemClock.uptimeMillis()<end){waitForIdleSync();if(condition.getAsBoolean())return;Thread.sleep(100);}throw new AssertionError(message);}
    private static <T> T find(View view,Class<T> type){if(view==null)return null;if(type.isInstance(view))return type.cast(view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){T value=find(((ViewGroup)view).getChildAt(i),type);if(value!=null)return value;}return null;}
    private static View findText(View view,String text){if(view==null)return null;if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findText(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    private void shell(String command)throws Exception{try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){byte[] buffer=new byte[4096];while(in.read(buffer)!=-1){}}}
    private void shot(String name)throws Exception{Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"Full-system screenshot is available");try(OutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
