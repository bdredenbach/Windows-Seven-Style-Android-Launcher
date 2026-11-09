package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.pm.ProviderInfo;
import android.os.Process;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.*;
import org.robolectric.shadows.ShadowContentResolver;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Implementation;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import static com.nthexperience.desktop.InitializationState.Step;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class InitializationFlowTest {
    private ActivityController<Activity> controller;private Activity activity;private DesktopStore store;private TestHost host;private TestFlow flow;
    private final Set<Step> granted=EnumSet.noneOf(Step.class);
    private class TestHost implements InitializationFlow.Host {boolean ready=true;int changes,wakes;public boolean ready(){return ready;}public boolean home(){return granted.contains(Step.HOME);}public boolean requestHome(){return true;}public void changed(){changes++;}public void wake(){wakes++;}}
    private class TestFlow extends InitializationFlow {TestFlow(){super(activity,store,host,Runnable::run);}@Override boolean granted(Step step){return granted.contains(step);}}
    @Before public void setup(){controller=Robolectric.buildActivity(Activity.class).setup();activity=controller.get();store=new DesktopStore(activity);store.prefs.edit().clear().commit();activity.getSharedPreferences(InitializationState.PREFS,0).edit().clear().commit();host=new TestHost();flow=new TestFlow();flow.resumed();}
    @After public void close(){flow.close();controller.pause().stop().destroy();}
    private AlertDialog dialog(){AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());return dialog;}
    private void assertTitle(String title){assertTrue("Dialog contains title: "+title,contains(dialog().getWindow().getDecorView(),title));}
    private boolean contains(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(contains(((ViewGroup)view).getChildAt(i),text))return true;return false;}
    private void later(){dialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();}
    private void enable(){dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();}
    private void finishEmptyFolderCheck(){flow.pending();shadowOf(Looper.getMainLooper()).idle();flow.pending();assertFalse(flow.pending());}
    @Test public void allSixStepsUseRequestedOrderAndDeclinesContinue(){
        String[] titles={"Default Launcher","Enable File Browsing","Enable Usage Access","Allow Notifications Access","Modify System Settings (Brightness)"};
        for(int i=0;i<titles.length;i++){assertTrue(flow.pending());assertEquals(Step.values()[i],flow.state.step());assertTitle(titles[i]);later();}
        assertEquals(Step.FOLDERS,flow.state.step());finishEmptyFolderCheck();assertEquals(Step.DONE,flow.state.step());assertFalse(flow.pending());
    }
    @Test public void existingGrantsAreSkippedAndCompletionIsDeviceLocal()throws Exception{
        granted.addAll(Arrays.asList(Step.HOME,Step.FILES,Step.USAGE,Step.NOTIFICATIONS,Step.BRIGHTNESS));finishEmptyFolderCheck();assertEquals(Step.DONE,new InitializationState(activity).step());assertFalse(DesktopBackup.snapshot(store).toString().contains(InitializationState.PREFS));assertFalse(store.prefs.contains("step"));
    }
    @Test public void settingsReturnWaitsForFocusAndThenOffersNotifications(){
        granted.addAll(Arrays.asList(Step.HOME,Step.FILES));assertTrue(flow.pending());assertEquals(Step.USAGE,flow.state.step());enable();assertEquals(InitializationFlow.USAGE,flow.state.waiting());Intent launched=shadowOf(activity).getNextStartedActivityForResult().intent;assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS,launched.getAction());
        flow.result(InitializationFlow.USAGE,Activity.RESULT_CANCELED,null);assertTrue(flow.pending());assertEquals(Step.USAGE,flow.state.step());flow.paused();host.ready=false;flow.resumed();flow.pending();assertEquals(Step.USAGE,flow.state.step());host.ready=true;flow.pending();assertEquals(Step.NOTIFICATIONS,flow.state.step());assertTitle("Allow Notifications Access");enable();assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,shadowOf(activity).getNextStartedActivityForResult().intent.getAction());
    }
    @Test public void brightnessUsesAppSpecificSystemSettings(){
        granted.addAll(Arrays.asList(Step.HOME,Step.FILES,Step.USAGE,Step.NOTIFICATIONS));flow.pending();enable();Intent launched=shadowOf(activity).getNextStartedActivityForResult().intent;assertEquals(Settings.ACTION_MANAGE_WRITE_SETTINGS,launched.getAction());assertEquals("package:"+activity.getPackageName(),launched.getDataString());assertEquals(InitializationFlow.BRIGHTNESS,flow.state.waiting());
    }
    @Test public void closingForRecreationKeepsCurrentStep(){
        flow.pending();later();flow.pending();assertEquals(Step.FILES,flow.state.step());flow.close();flow=new TestFlow();flow.resumed();flow.pending();assertEquals(Step.FILES,flow.state.step());assertTitle("Enable File Browsing");
    }
    @Test public void pendingSettingsSurviveRecreation(){
        granted.addAll(Arrays.asList(Step.HOME,Step.FILES));flow.pending();enable();flow.paused();flow.close();flow=new TestFlow();flow.resumed();flow.pending();assertEquals(Step.NOTIFICATIONS,flow.state.step());assertTitle("Allow Notifications Access");
    }
    @Test public void restoredBackupQueuesOnlyFolderRepairAndClearsDeferrals(){
        for(int i=0;i<6;i++)flow.state.advance();flow.state.defer("old");InitializationState.restored(activity);assertEquals(Step.FOLDERS,flow.state.step());assertTrue(flow.state.deferred().isEmpty());flow.state.prefs.edit().putInt("step",Step.USAGE.ordinal()).commit();InitializationState.restored(activity);assertEquals(Step.USAGE,flow.state.step());
    }
    @Test public void interruptedFolderWorkCanBeRetriedAfterRecreation(){flow.state.prefs.edit().putInt("step",Step.FOLDERS.ordinal()).commit();flow.state.waitFor(InitializationFlow.REPAIR,"content://example.documents/tree/old");flow.state.prefs.edit().putBoolean("processing",true).commit();flow.close();flow=new TestFlow();flow.resumed();assertEquals(0,flow.state.waiting());finishEmptyFolderCheck();}
    @Test public void freshLaunchClearsOrphanedPickerRequest(){flow.state.prefs.edit().putInt("step",Step.FOLDERS.ordinal()).commit();flow.state.waitFor(InitializationFlow.REPAIR,"content://example.documents/tree/old");flow.close();InitializationFlow fresh=new InitializationFlow(activity,store,host,Runnable::run,false);assertEquals(0,fresh.state.waiting());assertEquals(Step.FOLDERS,fresh.state.step());fresh.close();}
    @Test public void listenerIsDeclaredWithAndroidBindingPermission()throws Exception{
        ServiceInfo service=activity.getPackageManager().getServiceInfo(new ComponentName(activity,TenNotifications.class),0);assertEquals("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",service.permission);assertTrue(service.exported);
    }
    @Test public void missingRestoredFolderIsLastAndLaterPreservesItsReferences()throws Exception{
        store.prefs.edit().putString("linked_folders","[{\"uri\":\"content://example.documents/tree/old\",\"name\":\"Documents\"}]").commit();String before=store.prefs.getString("linked_folders","");flow.state.prefs.edit().putInt("step",Step.FOLDERS.ordinal()).commit();flow.pending();shadowOf(Looper.getMainLooper()).idle();flow.pending();assertTitle("Fix folders from restored backup");later();flow.pending();assertFalse(flow.pending());assertEquals(before,store.prefs.getString("linked_folders",""));
    }
    @Implements(android.os.Environment.class)
    public static class GrantedFileEnvironment extends ShadowEnvironment {@Implementation(minSdk=30) protected static boolean isExternalStorageManager(){return true;}}
    @Test @Config(shadows=GrantedFileEnvironment.class) public void trustedDesktopStorageReconnectsUsingAlreadyGrantedFileAccess()throws Exception{
        assertTrue(StorageAccess.has(activity));
        FolderRepairTest.Folders provider=new FolderRepairTest.Folders();ProviderInfo info=new ProviderInfo();info.authority=StorageAccess.AUTHORITY;provider.attachInfo(activity,info);ShadowContentResolver.registerProviderInternal(info.authority,provider);
        String foreign=StorageAccess.AUTHORITY.equals("com.nthexperience.desktop.storage")?"com.nthexperience.desktop.ten.storage":"com.nthexperience.desktop.storage";String old=DocumentsContract.buildTreeDocumentUri(foreign,"primary:").toString();store.prefs.edit().putString("linked_folders",new JSONArray().put(new JSONObject().put("uri",old).put("name","Internal storage")).toString()).commit();flow.state.prefs.edit().putInt("step",Step.FOLDERS.ordinal()).commit();
        for(int i=0;i<6&&flow.state.step()!=Step.DONE;i++){flow.pending();shadowOf(Looper.getMainLooper()).idle();}assertEquals(Step.DONE,flow.state.step());assertEquals(StorageAccess.AUTHORITY,Uri.parse(DocumentFolders.links(store).getJSONObject(0).getString("uri")).getAuthority());assertNull(shadowOf(activity).getNextStartedActivityForResult());
    }

    public static class UnavailableSettings extends Activity {@Override public void startActivityForResult(Intent intent,int request){throw new ActivityNotFoundException("Unavailable test settings");}}
    @Test public void unavailableSettingsAdvanceWithoutLooping(){
        flow.close();ActivityController<UnavailableSettings> unavailableController=Robolectric.buildActivity(UnavailableSettings.class).setup();Activity unavailableActivity=unavailableController.get();granted.addAll(Arrays.asList(Step.HOME,Step.FILES));InitializationFlow unavailable=new InitializationFlow(unavailableActivity,store,host,Runnable::run){@Override boolean granted(Step step){return granted.contains(step);}};
        try{unavailable.resumed();unavailable.pending();dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(Step.NOTIFICATIONS,unavailable.state.step());assertEquals(0,unavailable.state.waiting());}finally{unavailable.close();unavailableController.pause().stop().destroy();}
    }

}
