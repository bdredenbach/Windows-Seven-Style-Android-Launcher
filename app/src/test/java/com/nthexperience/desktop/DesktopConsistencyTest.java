package com.nthexperience.desktop;

import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class DesktopConsistencyTest {
    private Context context; private DesktopStore store;
    @Before public void setup() throws Exception {
        context=RuntimeEnvironment.getApplication(); store=new DesktopStore(context);store.prefs.edit().clear().commit();
        resetCache(); new File(context.getCacheDir(),"desktop-apps-v1.json").delete();
    }
    private static void resetCache() throws Exception {
        for(String name:new String[]{"memory","refreshed","configuration"}){java.lang.reflect.Field field=DesktopCache.class.getDeclaredField(name);field.setAccessible(true);field.set(null,name.equals("memory")?null:name.equals("configuration")?"":0L);}
    }
    private static String fixture(String variant) throws Exception {
        try(InputStream in=DesktopConsistencyTest.class.getResourceAsStream("/backups/"+variant+".json")){assertNotNull(in);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}
    }
    @Test public void importsEveryDesktopVariantAndReportsUnsupportedItems() throws Exception {
        for(String variant:new String[]{"vista","seven","eight","ten","eleven"}){
            DesktopBackup.Plan plan=DesktopBackup.parse(fixture(variant));
            assertEquals("A portable note from "+variant,plan.string("note",""));
            assertTrue(plan.string("shortcuts","").contains("@notes"));
            assertTrue(plan.string("quick_launch","").contains("@notes"));
            assertEquals(1,plan.folders());
            if(!variant.equals(DesktopBackup.VARIANT)&&!Arrays.asList(BackupCompatibility.WALLPAPERS).contains(new JSONObject(fixture(variant)).getJSONArray("wallpapers").getString(new JSONObject(fixture(variant)).getJSONObject("settings").getJSONObject("wallpaper").getInt("value"))))assertFalse(plan.values.containsKey("wallpaper"));
        }
    }
    @Test public void legacyBackupsSkipForeignQuickLaunchItemsAndLargeWallpaperOrdinal() throws Exception {
        JSONObject backup=new JSONObject(fixture("vista")); backup.remove("variant");backup.remove("wallpapers");
        JSONObject settings=backup.getJSONObject("settings");settings.getJSONObject("quick_launch").put("value","[\"@notes\",\"@future-tool\"]");settings.getJSONObject("wallpaper").put("value",999);
        DesktopBackup.Plan plan=DesktopBackup.parse(backup.toString());
        assertEquals("[\"@notes\"]",plan.string("quick_launch",""));assertFalse(plan.values.containsKey("wallpaper"));assertTrue(plan.skipped>=2);
        assertTrue(plan.skippedItems.toString().contains("quick_launch"));assertTrue(plan.skippedItems.toString().contains("wallpaper"));
    }
    @Test public void supportsOriginalEightBackupFormat() throws Exception {
        JSONObject backup=new JSONObject(fixture("vista"));backup.put("format","nth-desktop-eight-backup");backup.remove("variant");
        // Eight's historical format includes its two shell profiles; its migration requires them.
        
        assertEquals("A portable note from vista",DesktopBackup.parse(backup.toString()).string("note",""));
    }
    @Test public void partialRestorePreservesSkippedSettingsAndUndoRecoversPreviousDesktop() throws Exception {
        store.prefs.edit().putInt("wallpaper",1).putString("note","Before restore").commit();
        JSONObject backup=new JSONObject(fixture("vista"));backup.remove("variant");backup.getJSONObject("settings").getJSONObject("wallpaper").put("value",999);backup.remove("wallpapers");
        DesktopBackup.apply(context,store,DesktopBackup.parse(backup.toString()),false);
        assertEquals(1,store.prefs.getInt("wallpaper",-1));assertEquals("A portable note from vista",store.prefs.getString("note",""));
        DesktopBackup.apply(context,store,DesktopBackup.staged(context,DesktopBackup.UNDO),true);
        assertEquals("Before restore",store.prefs.getString("note",""));assertEquals(1,store.prefs.getInt("wallpaper",-1));
    }
    @Test public void nestedSavedThemeBackgroundsUseNamesInsteadOfForeignOrdinals() throws Exception {
        JSONObject backup=new JSONObject(fixture("vista"));Map<String,Object> values=new LinkedHashMap<>();JSONObject fields=backup.getJSONObject("settings");for(Iterator<String> keys=fields.keys();keys.hasNext();){String key=keys.next();values.put(key,fields.getJSONObject(key).get("value"));}
        backup.put("wallpapers",new JSONArray().put("Meadow").put("Foreign background"));
        values.put("seven_saved_themes","[{\"backgrounds\":[\"#0\",\"#1\"]}]");Set<String> skipped=new LinkedHashSet<>();BackupCompatibility.normalize(values,backup,skipped);
        assertEquals("[\"#2\"]",new JSONArray((String)values.get("seven_saved_themes")).getJSONObject(0).getJSONArray("backgrounds").toString());assertFalse(skipped.isEmpty());
    }
    @Test public void corruptAndIncompleteBackupsStillFailBeforePreferencesChange() throws Exception {
        store.prefs.edit().putString("note","Keep me").commit();
        for(String invalid:new String[]{"{broken",fixture("vista")+" trailing"}){try{DesktopBackup.parse(invalid);fail("Invalid backup accepted");}catch(JSONException expected){}}
        JSONObject backup=new JSONObject(fixture("vista"));backup.getJSONObject("settings").remove("note");
        try{DesktopBackup.parse(backup.toString());fail("Incomplete backup accepted");}catch(JSONException expected){}
        backup=new JSONObject(fixture("vista"));backup.getJSONObject("settings").getJSONObject("note").put("type","int");
        try{DesktopBackup.parse(backup.toString());fail("Invalid type accepted");}catch(JSONException expected){}
        assertEquals("Keep me",store.prefs.getString("note",""));
    }
    @Test public void newSnapshotsKeepStrictValidationForTheSameVariant() throws Exception {
        JSONObject backup=DesktopBackup.snapshot(store);backup.getJSONObject("settings").put("wallpaper",new JSONObject().put("type","int").put("value",999));
        try{DesktopBackup.parse(backup.toString());fail("Invalid local wallpaper accepted");}catch(JSONException expected){}
    }
    @Test public void inventoryCacheSurvivesColdReloadAndFailedRefresh() throws Exception {
        PackageInfo pkg=new PackageInfo();pkg.packageName="com.example.cached";pkg.applicationInfo=new ApplicationInfo();pkg.applicationInfo.packageName=pkg.packageName;pkg.applicationInfo.flags=ApplicationInfo.FLAG_INSTALLED;pkg.applicationInfo.enabled=true;
        ActivityInfo activity=new ActivityInfo();activity.packageName=pkg.packageName;activity.name="com.example.cached.Main";activity.applicationInfo=pkg.applicationInfo;activity.enabled=true;activity.exported=true;activity.nonLocalizedLabel="Cached App";pkg.activities=new ActivityInfo[]{activity};
        Shadows.shadowOf(context.getPackageManager()).installPackage(pkg);
        ResolveInfo resolve=new ResolveInfo();resolve.activityInfo=activity;resolve.nonLocalizedLabel="Cached App";
        Shadows.shadowOf(context.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),resolve);
        assertEquals(1,DesktopCache.load(context).size());
        resetCache();assertEquals("Cached App",DesktopCache.read(context).get(0).label);
        DesktopCache.invalidate();Context failing=new ContextWrapper(context){@Override public PackageManager getPackageManager(){throw new IllegalStateException("Package service unavailable");}};
        assertEquals(1,DesktopCache.load(failing).size());
    }
    @Test public void damagedCacheIsRebuildable() throws Exception {
        try(FileOutputStream out=new FileOutputStream(new File(context.getCacheDir(),"desktop-apps-v1.json"))){out.write("{broken".getBytes(StandardCharsets.UTF_8));}
        assertNull(DesktopCache.read(context));assertNotNull(DesktopCache.load(context));
    }
    @Test public void taskManagerOffersTheCorrespondingWindowsNavigation() throws Exception {
        TaskManagerPanel panel=new TaskManagerPanel(context,new TaskManagerPanel.Host(){
            public List<TaskManagerPanel.Task> tasks(){return Collections.emptyList();}public View icon(String id){return new View(context);}public void switchTo(String id){}public void end(String id){}public void appInfo(String id){}public void newTask(){}public void close(){}
        });
        for(String page:new String[]{"Applications","Processes","Services","Performance","Networking","Users"})assertNotNull(page,find(panel,"Task Manager "+page));
        panel.close();
    }
    private static View find(View view,String description){if(description.equals(view.getContentDescription()==null?null:view.getContentDescription().toString()))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),description);if(found!=null)return found;}}return null;}
}
