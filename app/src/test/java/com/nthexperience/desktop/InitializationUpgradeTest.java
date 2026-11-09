package com.nthexperience.desktop;

import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class InitializationUpgradeTest {
    private Context context;
    @Before public void reset(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences(InitializationState.PREFS,0).edit().clear().commit();context.getSharedPreferences(StartupExperience.SESSION,0).edit().clear().commit();}
    @Test public void freshInstallStartsWithHome(){assertEquals(InitializationState.Step.HOME,new InitializationState(context).step());}
    @Test public void completedLegacyInstallOnlyOffersNewLocationOnce(){
        context.getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("welcome_seen",true).commit();
        InitializationState state=new InitializationState(context);assertEquals(InitializationState.Step.LOCATION,state.step());
        state.advance();state.advance();assertEquals(InitializationState.Step.DONE,new InitializationState(context).step());
    }
    @Test public void legacyIntroAloneDoesNotSkipSetup(){
        context.getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).commit();
        assertEquals(InitializationState.Step.HOME,new InitializationState(context).step());
    }
    @Test public void pendingFlowIsNotReplacedByLegacyWelcome(){
        context.getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("welcome_seen",true).commit();
        context.getSharedPreferences(InitializationState.PREFS,0).edit().putInt("step",2).putInt("waiting",InitializationFlow.USAGE).putBoolean("left",true).commit();
        InitializationState state=new InitializationState(context);assertEquals(InitializationState.Step.USAGE,state.step());assertEquals(InitializationFlow.USAGE,state.waiting());assertTrue(state.prefs.getBoolean("left",false));
    }
}
