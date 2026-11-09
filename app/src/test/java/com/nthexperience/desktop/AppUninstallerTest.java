package com.nthexperience.desktop;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class AppUninstallerTest {
    private static final String PKG="example.removable",ID=PKG+"/.Main",SECOND=PKG+"/.Other",KEEP="example.keep/.Main";
    private ActivityController<Activity> controller;private Activity activity;private DesktopStore store;
    @Before public void setup(){controller=Robolectric.buildActivity(Activity.class).setup();activity=controller.get();store=new DesktopStore(activity);store.prefs.edit().clear().commit();activity.getSharedPreferences("launcher_uninstall",0).edit().clear().commit();install(PKG,0);resetCache();}
    @After public void close(){controller.pause().stop().destroy();resetCache();}
    private void resetCache(){ReflectionHelpers.setStaticField(DesktopCache.class,"memory",null);ReflectionHelpers.setStaticField(DesktopCache.class,"refreshed",0L);ReflectionHelpers.setStaticField(DesktopCache.class,"configuration","");new File(activity.getCacheDir(),"desktop-apps-v1.json").delete();new File(activity.getCacheDir(),"desktop-apps-v1.json.bak").delete();}
    private ApplicationInfo install(String pkg,int flags){ApplicationInfo app=new ApplicationInfo();app.packageName=pkg;app.flags=flags;app.nonLocalizedLabel="Removable app";PackageInfo info=new PackageInfo();info.packageName=pkg;info.applicationInfo=app;shadowOf(activity.getPackageManager()).installPackage(info);return app;}
    private void saved(){store.startPins(Arrays.asList(ID,SECOND,KEEP,"@files"));store.prefs.edit().putString("note","Keep my note").putString("desktop_positions_portrait","{\"saved\":true}").commit();}
    @Test public void onlyRemovableInstalledAppsGetTheAction(){assertTrue(AppUninstaller.available(activity,ID));assertFalse(AppUninstaller.available(activity,"@files"));assertFalse(AppUninstaller.available(activity,"folder:123"));assertFalse(AppUninstaller.available(activity,"missing.app/.Main"));assertFalse(AppUninstaller.available(activity,activity.getPackageName()+"/.Main"));install("example.system",ApplicationInfo.FLAG_SYSTEM);assertFalse(AppUninstaller.available(activity,"example.system/.Main"));install("example.updated",ApplicationInfo.FLAG_UPDATED_SYSTEM_APP);assertFalse(AppUninstaller.available(activity,"example.updated/.Main"));}
    @Test public void requestOpensAndroidUninstallerAndKeepsLauncherData(){saved();Map<String,?> before=new HashMap<>(store.prefs.getAll());assertTrue(AppUninstaller.request(activity,ID));org.robolectric.shadows.ShadowActivity.IntentForResult result=shadowOf(activity).getNextStartedActivityForResult();assertEquals(Intent.ACTION_DELETE,result.intent.getAction());assertEquals("package:"+PKG,result.intent.getDataString());assertEquals(AppUninstaller.REQUEST,result.requestCode);assertEquals(before,store.prefs.getAll());}
    @Test public void cancellationPreservesAppAndPins(){saved();Map<String,?> before=new HashMap<>(store.prefs.getAll());AppUninstaller.request(activity,ID);assertNull(AppUninstaller.returned(activity,store));assertEquals(before,store.prefs.getAll());assertTrue(AppUninstaller.available(activity,ID));}
    @Test public void confirmedRemovalPrunesOnlyThatPackagesStartEntries()throws Exception{
        saved();String bank="{\"version\":1,\"groups\":[{\"name\":\"My group\",\"items\":[{\"id\":\""+ID+"\",\"size\":\"Medium\"},{\"id\":\""+KEEP+"\",\"size\":\"Wide\"}]}]}";store.prefs.edit().putString("eight_start_8",bank).putString("eight_start_81",bank).commit();AppUninstaller.request(activity,ID);shadowOf(activity.getPackageManager()).removePackage(PKG);assertEquals(PKG,AppUninstaller.returned(activity,store));assertEquals(Arrays.asList(KEEP,"@files"),store.startPins());assertEquals("Keep my note",store.prefs.getString("note",""));assertEquals("{\"saved\":true}",store.prefs.getString("desktop_positions_portrait",""));
        for(String key:new String[]{"eight_start_8","eight_start_81"}){JSONObject group=new JSONObject(store.prefs.getString(key,"")).getJSONArray("groups").getJSONObject(0);assertEquals("My group",group.getString("name"));assertEquals(1,group.getJSONArray("items").length());assertEquals(KEEP,group.getJSONArray("items").getJSONObject(0).getString("id"));}
    }
    @Test public void packageReplacementIsNotTreatedAsUninstall(){Intent event=new Intent(Intent.ACTION_PACKAGE_REMOVED,Uri.parse("package:"+PKG)).putExtra(Intent.EXTRA_REPLACING,true);assertNull(AppUninstaller.removedPackage(event));event.putExtra(Intent.EXTRA_REPLACING,false);assertEquals(PKG,AppUninstaller.removedPackage(event));assertNull(AppUninstaller.removedPackage(new Intent(Intent.ACTION_PACKAGE_CHANGED,Uri.parse("package:"+PKG))));}
    @Test public void popupOffersUninstallAsADirectCommand(){PopupMenu menu=new PopupMenu(activity,new View(activity));AppUninstaller.add(menu,activity,ID);assertEquals("Uninstall",menu.getMenu().findItem(AppUninstaller.MENU_ID).getTitle());assertTrue(menu.getMenu().performIdentifierAction(AppUninstaller.MENU_ID,0));assertEquals(Intent.ACTION_DELETE,shadowOf(activity).getNextStartedActivityForResult().intent.getAction());PopupMenu builtIn=new PopupMenu(activity,new View(activity));AppUninstaller.add(builtIn,activity,"@files");assertNull(builtIn.getMenu().findItem(AppUninstaller.MENU_ID));}
    @Test public void desktopContextMenuUsesTheSameUninstallAction(){DesktopContextMenu menu=new DesktopContextMenu(activity);AppUninstaller.add(menu,activity,ID);LinearLayout items=ReflectionHelpers.getField(menu,"items");View action=null;for(int i=0;i<items.getChildCount();i++)if("Uninstall".contentEquals(items.getChildAt(i).getContentDescription()))action=items.getChildAt(i);assertNotNull(action);action.performClick();assertEquals("package:"+PKG,shadowOf(activity).getNextStartedActivityForResult().intent.getDataString());}
    public static class BlockedActivity extends Activity {@Override public void startActivityForResult(Intent intent,int request){throw new SecurityException("Blocked by device policy");}}
    @Test public void blockedUninstallKeepsLauncherData(){saved();Map<String,?> before=new HashMap<>(store.prefs.getAll());ActivityController<BlockedActivity> blocked=Robolectric.buildActivity(BlockedActivity.class).setup();try{assertFalse(AppUninstaller.request(blocked.get(),ID));assertEquals(before,store.prefs.getAll());assertNull(AppUninstaller.returned(activity,store));}finally{blocked.pause().stop().destroy();}}
    @Test public void removedLastAppDoesNotReturnFromDiskCache(){
        ApplicationInfo app=install(PKG,0);ActivityInfo info=new ActivityInfo();info.packageName=PKG;info.name=PKG+".Main";info.applicationInfo=app;ResolveInfo resolve=new ResolveInfo();resolve.activityInfo=info;Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);shadowOf(activity.getPackageManager()).addResolveInfoForIntent(launcher,resolve);assertTrue(DesktopCache.load(activity).stream().anyMatch(a->PKG.equals(AppUninstaller.packageName(a.id))));shadowOf(activity.getPackageManager()).removeResolveInfosForIntent(launcher,PKG);shadowOf(activity.getPackageManager()).removePackage(PKG);DesktopCache.forgetPackage(activity,PKG);assertTrue(DesktopCache.read(activity).stream().noneMatch(a->PKG.equals(AppUninstaller.packageName(a.id))));ReflectionHelpers.setStaticField(DesktopCache.class,"memory",null);assertTrue(DesktopCache.read(activity).stream().noneMatch(a->PKG.equals(AppUninstaller.packageName(a.id))));assertTrue(DesktopCache.load(activity).stream().noneMatch(a->PKG.equals(AppUninstaller.packageName(a.id))));
    }
    @Test public void manifestDeclaresUninstallRequestPermission()throws Exception{assertTrue(Arrays.asList(activity.getPackageManager().getPackageInfo(activity.getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions).contains("android.permission.REQUEST_DELETE_PACKAGES"));}
}
