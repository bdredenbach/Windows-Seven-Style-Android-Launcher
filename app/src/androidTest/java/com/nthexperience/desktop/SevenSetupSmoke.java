package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.TextView;
import java.io.*;
import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;

/** Real-device setup dialogs and upgrade progress, independent of desktop UI fixtures. */
public final class SevenSetupSmoke extends Instrumentation {
    private MainActivity home;
    private InitializationFlow flow;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle out=new Bundle();boolean passed=false;try{
        Context context=getTargetContext();
        new DesktopStore(context).prefs.edit().clear().putInt("startup_mode",2).putBoolean("welcome_on_start",false).commit();
        context.getSharedPreferences(StartupExperience.SESSION,0).edit().clear().commit();
        context.getSharedPreferences(InitializationState.PREFS,0).edit().clear().commit();
        launch();int previous=-1,prompts=0;
        String[] titles={"Default Launcher","Enable File Browsing","Enable Usage Access","Allow Notifications Access","Modify System Settings (Brightness)","Allow Location"};
        while(flow.state.step()!=InitializationState.Step.DONE){
            await(()->flow.state.step()==InitializationState.Step.DONE||dialog()!=null,"Setup advances or shows its next dialog");
            if(flow.state.step()==InitializationState.Step.DONE)break;
            int step=flow.state.step().ordinal();check(step>previous&&step<titles.length,"Permission prompts retain the requested order");previous=step;
            AlertDialog shown=dialog();check(hasText(shown.getWindow().getDecorView(),titles[step]),"Native dialog title: "+titles[step]);
            runOnMainSync(()->shown.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());prompts++;
            final int left=step;await(()->flow.state.step().ordinal()>left,"Later advances optional setup");
        }
        check(prompts>=3,"Fresh installation offers ungranted optional permissions");
        await(()->context.getSharedPreferences(StartupExperience.SESSION,0).getBoolean("welcome_seen",false),"Existing Welcome follows setup");
        runOnMainSync(home::finish);waitForIdleSync();
        // Model the existing 0.7.1 startup session, with no new-flow preferences.
        context.getSharedPreferences(InitializationState.PREFS,0).edit().clear().commit();
        context.getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).commit();
        launch();await(()->dialog()!=null,"Legacy update offers the new optional location choice");
        check(flow.state.step()==InitializationState.Step.LOCATION,"Legacy update skips earlier completed setup");
        AlertDialog location=dialog();check(hasText(location.getWindow().getDecorView(),"Allow Location"),"Location is the only upgrade prompt");
        runOnMainSync(()->location.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        await(()->flow.state.step()==InitializationState.Step.DONE&&home.hasWindowFocus(),"Declining location completes upgrade without blocking the desktop");
        runOnMainSync(home::finish);waitForIdleSync();launch();
        await(home::hasWindowFocus,"Completed desktop regains focus");
        check(flow.state.step()==InitializationState.Step.DONE&&dialog()==null,"Completed setup is not replayed on reopen");
        passed=true;out.putString("stream","PASS: native ordered optional setup, Later, Welcome, legacy 0.7.1 location-only migration and no repeated prompts after reopen.\n");
    }catch(Throwable failure){StringWriter trace=new StringWriter();failure.printStackTrace(new PrintWriter(trace));out.putString("stream","FAIL: "+trace);}finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,out);}
    private void launch()throws Exception{
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Field field=MainActivity.class.getDeclaredField("initialization");field.setAccessible(true);flow=(InitializationFlow)field.get(home);
    }
    private AlertDialog dialog(){try{Field field=InitializationFlow.class.getDeclaredField("dialog");field.setAccessible(true);AlertDialog shown=(AlertDialog)field.get(flow);return shown!=null&&shown.isShowing()?shown:null;}catch(Exception failure){throw new AssertionError(failure);}}
    private static boolean hasText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasText(((ViewGroup)view).getChildAt(i),text))return true;return false;}
    private void await(BooleanSupplier condition,String message)throws Exception{long end=SystemClock.uptimeMillis()+20000;while(SystemClock.uptimeMillis()<end){waitForIdleSync();if(condition.getAsBoolean())return;Thread.sleep(100);}throw new AssertionError(message);}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
