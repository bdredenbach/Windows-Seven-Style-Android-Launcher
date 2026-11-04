package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.nio.charset.StandardCharsets;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.json.*;

/** Native regression for the Vista fidelity pass, with real drag/drop bytes and measured graphs. */
public final class FidelitySmoke extends Instrumentation {
    private MainActivity home;private View root;private DesktopStore store;private final Map<String,Object> original=new HashMap<>();private File folder;private String recycleFixture;
    private final BlockingQueue<MainActivity> resumed=new LinkedBlockingQueue<>();private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){public void onActivityResumed(Activity a){if(a instanceof MainActivity)resumed.offer((MainActivity)a);}public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivityPaused(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}};
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();boolean success=false;Application app=(Application)getTargetContext().getApplicationContext();app.registerActivityLifecycleCallbacks(lifecycle);try{
        store=new DesktopStore(getTargetContext());original.putAll(store.prefs.getAll());store.prefs.edit().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("sidebar_always_top",false).putBoolean("tray_expanded",false).putBoolean("tray_battery",true).putBoolean("tray_network",true).putBoolean("tray_volume",true).putBoolean("tray_brightness",true).putString("taskbar_grouping","Always").putString("explorer_view_mode","Details").putBoolean("explorer_details",true).putBoolean("desktop_sounds",false).commit();getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();
        store.prefs.edit().putBoolean("layout_locked",false).remove("desktop_positions_landscape").apply();
        recycleFixture=store.createFolder("Recycle drag fixture",Collections.singletonList("@notes"));
        List<String> icons=store.desktop();icons.remove(recycleFixture);icons.remove("@recycle");icons.add(0,"@recycle");icons.add(0,recycleFixture);store.desktop(icons);
        android.accessibilityservice.AccessibilityServiceInfo service=getUiAutomation().getServiceInfo();
        service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        getUiAutomation().setServiceInfo(service);
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();await(()->home.hasWindowFocus(),"Focused desktop");sevenDesktop();tray();taskbarModes();binProperties();shortcutRecycle();backgrounds();graphs();folders();
        result.putString("stream","PASS: Seven reuses pinned icon slots and exposes real Explorer Libraries; task buttons preserve 0/50/100 glass opacity; equal Quick Launch separators; inline tray arrow and real device icons; draft-only background thumbnails, custom picture decode, Save changes and position; real PID/CPU/RAM/network graph sampling and pause/reduced motion; separate Explorer windows, taskbar family grouping, physical icon drag copy/move with exact bytes and original preservation; real folder desktop links and validated backups.\n");success=true;
    }catch(Throwable error){try{shot("fidelity-failure.png");StringBuilder tree=new StringBuilder();dump(getUiAutomation().getRootInActiveWindow(),tree,0);try(FileOutputStream out=getTargetContext().openFileOutput("fidelity-tree.txt",0)){out.write(tree.toString().getBytes(StandardCharsets.UTF_8));}}catch(Exception ignored){}StringWriter text=new StringWriter();error.printStackTrace(new PrintWriter(text));result.putString("stream","FAIL: "+text);
    }finally{try{shell("settings put global animator_duration_scale 0");if(home!=null)ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));if(folder!=null)delete(folder);for(JSONObject item:RecycleBin.list(getTargetContext()))if(recycleFixture!=null&&recycleFixture.equals(item.optString("shortcut")))RecycleBin.remove(getTargetContext(),item);if(store!=null){SharedPreferences.Editor edit=store.prefs.edit().clear();for(Map.Entry<String,Object> item:original.entrySet()){Object value=item.getValue();String key=item.getKey();if(value instanceof String)edit.putString(key,(String)value);else if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);else if(value instanceof Long)edit.putLong(key,(Long)value);else if(value instanceof Float)edit.putFloat(key,(Float)value);}edit.commit();}}catch(Exception ignored){}app.unregisterActivityLifecycleCallbacks(lifecycle);}
        finish(success?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private void sevenDesktop() throws Exception {
        progress("Seven icon taskbar and Explorer Libraries");
        TextView clock=(TextView)find(v->"Clock and calendar".equals(v.getContentDescription()));
        await(()->clock!=null&&clock.getText().toString().contains("\n"),"Taskbar stacks time above the date");
        ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());
        View portrait=find(v->"Change profile picture".equals(v.getContentDescription()));
        View programs=find(v->"Installed programs. Hold an app for desktop options.".equals(v.getContentDescription()));
        int[] portraitAt=new int[2],programsAt=new int[2];
        await(()->{
            if(portrait==null||programs==null)return false;
            portrait.getLocationOnScreen(portraitAt);programs.getLocationOnScreen(programsAt);
            return portraitAt[1]<programsAt[1];
        },"The profile picture projects above the program pane");
        ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());
        List<String> saved = store.quickLaunch();
        ui(() -> {
            store.quickLaunch(Arrays.asList("@notes", "@files", MediaLibrary.PLAYER));
            store.prefs.edit().putBoolean("taskbar_show_labels", false).apply();
            home.open("@notes");
            window("@notes").closeWindow();
        });
        View strip = find(v -> "taskbar-items".equals(v.getTag()));
        ViewGroup buttons = (ViewGroup) ((ViewGroup) strip).getChildAt(0);
        await(() -> buttons.getChildCount() == 3
                && "Open Notepad".equals(buttons.getChildAt(0).getContentDescription()), "Idle pins retain their order");
        ui(() -> home.open("@notes"));
        await(() -> buttons.getChildCount() == 3
                && "Minimize Notepad".equals(buttons.getChildAt(0).getContentDescription()), "Pinned Notepad keeps one running slot");
        ViewGroup noteButton = (ViewGroup) buttons.getChildAt(0);
        check(noteButton.getChildAt(1).getVisibility() == View.GONE, "Seven uses icon-only buttons");
        tap(noteButton, 60);
        await(() -> "Restore Notepad".equals(buttons.getChildAt(0).getContentDescription()), "Pinned slot restores its running window");
        ui(() -> {window("@notes").closeWindow(); home.open("@files");});
        DesktopWindow files = window("@files");
        await(() -> find(files, v -> "Navigation: Libraries".equals(v.getContentDescription())) != null, "Libraries is discoverable in the navigation pane");
        ui(() -> find(files, v -> "Navigation: Libraries".equals(v.getContentDescription())).performClick());
        VistaExplorer explorer = (VistaExplorer) files.body;
        await(() -> explorer.page().equals("libraries")
                && find(files, v -> "Videos".equals(v.getContentDescription())) != null, "Explorer displays the four native library folders");
        SystemClock.sleep(250);
        shot("seven-desktop-libraries.png");
        ui(() -> {
            files.closeWindow();
            store.quickLaunch(saved);
            store.prefs.edit().putBoolean("taskbar_show_labels", true).apply();
            home.open("@notes");
            window("@notes").closeWindow();
        });
    }
    private void tray()throws Exception{
        progress("task opacity, matching separators and inline tray expansion");ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));await(()->root.getWidth()>root.getHeight(),"Wide tray ready");AeroWallpaper wallpaper=(AeroWallpaper)find(v->v instanceof AeroWallpaper);for(int opacity:new int[]{0,50,100}){store.prefs.edit().putBoolean("aero_transparency",true).putInt("aero_opacity",opacity).commit();for(boolean active:new boolean[]{false,true}){Bitmap bitmap=Bitmap.createBitmap(80,48,Bitmap.Config.ARGB_8888);AeroGlass surface=new AeroGlass(wallpaper,AeroGlass.TASKBUTTON);surface.active(active);surface.setBounds(0,0,80,48);ui(()->surface.draw(new Canvas(bitmap)));check(Math.abs(Color.alpha(bitmap.getPixel(40,24))-Math.round(opacity*255/100f))<=1,"Task button honors opacity for active="+active);if(opacity==0)for(int y=0;y<48;y++)for(int x=0;x<80;x++)check(bitmap.getPixel(x,y)==0,"No opaque task edges");bitmap.recycle();}}
        store.prefs.edit().putInt("aero_opacity",85).commit();View a=find(v->"Quick Launch separator".equals(v.getContentDescription())),b=find(v->"Taskbar commands".equals(v.getContentDescription()));check(a instanceof TaskbarSeparator&&b instanceof TaskbarSeparator&&a.getWidth()==b.getWidth(),"Identical separator drawing and width");
        Predicate<View> trayIcon = v -> {
            CharSequence label = v.getContentDescription();
            return label != null && (label.toString().startsWith("Battery status")
                    || Arrays.asList("Network status", "Volume control", "Brightness control", "Task Manager").contains(label.toString()));
        };
        check(find(trayIcon) == null, "Collapsed tray hides all icons even with legacy pin settings");
        tap(find(v -> "Device controls".equals(v.getContentDescription())), 60);
        for (String label : Arrays.asList("Battery status", "Network status", "Volume control", "Brightness control", "Task Manager")) {
            await(() -> find(v -> v.getContentDescription() != null
                    && v.getContentDescription().toString().startsWith(label)) != null, "Arrow reveals " + label);
        }
        ViewGroup tray = (ViewGroup) find(v -> v instanceof SystemTray);
        for (int i = 1; i < tray.getChildCount(); i++) {
            View previous = tray.getChildAt(i - 1);
            View button = tray.getChildAt(i);
            check(button.getWidth() == previous.getWidth() && button.getLeft() == previous.getRight()
                    && button.getTop() == previous.getTop(), "Tray buttons have even spacing");
        }
        check(store.prefs.getBoolean("tray_expanded", false), "Expansion persists");
        shot("vista-tray-expanded.png");
        tap(find(v -> "Device controls".equals(v.getContentDescription())), 60);
        await(() -> find(trayIcon) == null && !store.prefs.getBoolean("tray_expanded", true), "Arrow collapses every tray icon");
        check(find(v -> "Clock and calendar".equals(v.getContentDescription())) != null, "Clock remains visible");

        ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));await(()->root.getWidth()<root.getHeight(),"Portrait restored");
        View startButton = find(v -> "Start menu".equals(v.getContentDescription()));
        tap(startButton, 60);
        StartMenu menu = (StartMenu) find(v -> v instanceof StartMenu);
        check(menu != null, "Portrait Start opens");
        View workspace = (View) menu.getParent();
        if (workspace.getHeight() >= Aero.dp(home, 600)) {
            check(menu.getHeight() > Aero.dp(home, 540), "Portrait Start uses the extra vertical space");
        }
        tap(startButton, 60);

    }
    private void taskbarModes() throws Exception {
        progress("taskbar Scrollable and Static modes");
        String[] ids={"@notes","@calendar","@control","@backup"};
        ui(()->{
            store.prefs.edit().putBoolean("taskbar_static",false).apply();
            for(String id:ids)home.open(id);
        });
        HorizontalScrollView strip=(HorizontalScrollView)find(v->"taskbar-items".equals(v.getTag()));
        await(()->strip.canScrollHorizontally(1),"Scrollable mode allows overflow");
        ui(()->strip.scrollTo(strip.getWidth(),0));
        check(strip.getScrollX()>0,"Task strip can move to later buttons");
        selectTaskbarMode("Static");
        await(()->store.prefs.getBoolean("taskbar_static",false)&&strip.getScrollX()==0
                && !strip.canScrollHorizontally(1),"Static resets scrolling and fits the task buttons");
        ViewGroup buttons=(ViewGroup)strip.getChildAt(0);
        check(buttons.getChildCount()==ids.length,"All open task buttons remain visible in portrait");
        for(int i=0;i<buttons.getChildCount();i++){
            View button=buttons.getChildAt(i);
            check(button.getWidth()>0&&button.getRight()<=strip.getWidth(),"Task button fits in the viewport");
        }
        check(Boolean.TRUE.equals(DesktopBackup.parse(DesktopBackup.snapshot(store).toString()).values.get("taskbar_static")),
                "Desktop backups preserve Static mode");
        selectTaskbarMode("Scrollable");
        await(()->!store.prefs.getBoolean("taskbar_static",true)&&strip.canScrollHorizontally(1),
                "Switching back restores the scrollable strip");
        ui(()->{for(String id:ids)window(id).closeWindow();});
    }
    private void selectTaskbarMode(String mode)throws Exception{
        for(int attempt=0;attempt<2;attempt++){
            tap(find(v->"Taskbar commands".equals(v.getContentDescription())),700);
            try{awaitNode(mode);break;}
            catch(AssertionError failure){if(attempt==1)throw failure;}
        }
        menuTap(mode);
    }
    private void backgrounds()throws Exception{
        progress("background preview, save, custom picture and position");int saved=store.prefs.getInt("wallpaper",0);ui(()->home.open("setting:page:background"));VistaControlPanel control=(VistaControlPanel)window("@control").body;int choice=saved==1?2:1;tap(find(control,v->("Preview background: "+(choice==1?"Twilight":"Meadow")).equals(v.getContentDescription())),60);check(store.prefs.getInt("wallpaper",0)==saved,"Thumbnail only previews");tap(text(control,"Save changes"),60);check(store.prefs.getInt("wallpaper",0)==choice,"Save applies");shot("vista-background-picker.png");
        tap(find(control,v->"Include background: Harmony".equals(v.getContentDescription())),60);
        tap(find(control,v->"Shuffle".equals(v.getContentDescription())),60);
        tap(text(control,"Save changes"),60);
        check(DesktopBackgrounds.current(store.prefs).size()==2,"Two checked pictures form a slideshow");
        DesktopBackup.Plan slideshow=DesktopBackup.parse(DesktopBackup.snapshot(store).toString());
        check(Boolean.TRUE.equals(slideshow.values.get(DesktopBackgrounds.SHUFFLE)),"Slideshow settings survive backup validation");
        folder=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"Nth-Fidelity-"+SystemClock.uptimeMillis());check(folder.mkdirs(),"Isolated fixture directory");File picture=new File(folder,"Background.png");Bitmap image=Bitmap.createBitmap(160,80,Bitmap.Config.ARGB_8888);image.eraseColor(0xffc64e46);try(FileOutputStream out=new FileOutputStream(picture)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();String uri=StorageAccess.document("Documents/"+folder.getName()+"/Background.png").toString();ui(()->control.wallpaperChosen(uri));await(()->{View save=text(control,"Save changes");return save!=null&&save.isEnabled();},"Actual selected image decodes");tap(find(control,v->"Picture position".equals(v.getContentDescription())),60);menuTap("Fit");
        await(()->{View current=find(control,v->"Picture position".equals(v.getContentDescription()));return current instanceof Spinner&&"Fit".equals(((Spinner)current).getSelectedItem());},"Native picture position selection settles");
        for(int attempt=0;attempt<3&&!uri.equals(store.prefs.getString("wallpaper_photo",""));attempt++)tap(text(control,"Save changes"),60);
        await(()->uri.equals(store.prefs.getString("wallpaper_photo",""))&&store.prefs.getString("wallpaper_position","").equals("Fit"),"Selected content URI and position applied");close("@control");
    }
    private void graphs()throws Exception{
        progress("real monitor metrics, animated interpolation, reduced motion and pause");ui(()->home.open("@taskmanager"));TaskManagerPanel panel=(TaskManagerPanel)window("@taskmanager").body;tap(find(panel,v->"Task Manager Performance".equals(v.getContentDescription())),60);int count=panel.sampleCount();await(()->panel.sampleCount()>count,"Actual CPU/memory sampling advances");check(panel.animatedGraphCount()==0,"Reduced motion leaves graph static");shell("settings put global animator_duration_scale 1");store.prefs.edit().putBoolean("reduce_motion",false).commit();await(()->panel.animatedGraphCount()>0,"Real samples animate with motion enabled");shot("vista-taskmanager-graphs.png");tap(text(panel,"View"),60);menuTap("Pause updates");int paused=panel.sampleCount();Thread.sleep(2300);check(paused==panel.sampleCount()&&panel.animatedGraphCount()==0,"Paused counters and animations stop");tap(find(panel,v->"Task Manager Processes".equals(v.getContentDescription())),60);check(find(panel,v->v instanceof TextView&&((TextView)v).getText().toString().contains("PID "+android.os.Process.myPid()))!=null,"Real process PID is shown");tap(find(panel,v->"Task Manager Networking".equals(v.getContentDescription())),60);check(find(panel,v->"Device received traffic history".equals(v.getContentDescription()))!=null,"Native network graph exists");close("@taskmanager");store.prefs.edit().putBoolean("reduce_motion",true).commit();shell("settings put global animator_duration_scale 0");
    }
    private void binProperties()throws Exception{progress("shared Recycle Bin properties");ui(()->home.open("@recycle"));RecyclePanel panel=(RecyclePanel)window("@recycle").body;tap(text(panel,"Organize ▾"),60);menuTap("Properties");awaitNode("Recycle Bin Properties");awaitNode("Show Recycle Bin on desktop");menuTap("Recycle Bin maximum size");menuTap("500 MB");menuTap("Apply");check(store.prefs.getInt("recycle_capacity_mb",0)==500&&RecycleBin.capacity(home)==500L*1024*1024,"Native Bin Properties applies shared capacity");close("@recycle");}
    private void shortcutRecycle() throws Exception {
        progress("physical desktop folder drag into Recycle Bin and restoration");
        View source=find(v->recycleFixture.equals(v.getTag()));
        View bin=find(v->"@recycle".equals(v.getTag()));
        drag(source,bin);
        await(()->!store.desktop().contains(recycleFixture),"Desktop folder moved off the desktop");
        check(!store.isFolder(recycleFixture),"Recycling removes the actual app folder");
        ui(()->home.open("@recycle"));
        RecyclePanel panel=(RecyclePanel)window("@recycle").body;
        await(()->find(panel,v->"Recycle item: Recycle drag fixture".equals(v.getContentDescription()))!=null,"Folder appears in the Bin");
        View recycled=find(panel,v->"Recycle item: Recycle drag fixture".equals(v.getContentDescription()));
        check(recycled.findViewWithTag("recycle-item-icon") instanceof AeroIcon,"A recycled folder displays its own icon");
        check(text(panel,"Restore all items")!=null&&find(panel,v->"File menu".equals(v.getContentDescription()))==null,"Single adaptive Restore and hidden classic menus");
        shot("vista-recycle-details.png");
        tap(text(panel,"Views ▾"),60);menuTap("Large icons");
        await(()->find(panel,v->"Sort Recycle Bin by Name".equals(v.getContentDescription()))==null,"Icon view replaces details columns");
        tap(find(panel,v->"Recycle item: Recycle drag fixture".equals(v.getContentDescription())),60);
        shot("vista-recycle-icons.png");
        tap(text(panel,"Restore this item"),60);
        await(()->store.desktop().contains(recycleFixture),"Folder restores to the desktop");
        close("@recycle");
        check(store.folderItems(recycleFixture).equals(Collections.singletonList("@notes")),"Restore returns the folder and its shortcuts");

        ui(()->home.open("@organize"));
        DesktopOrganizer organizer=(DesktopOrganizer)window("@organize").body;
        String navigation="Navigation: Recycle drag fixture";
        await(()->find(organizer,v->navigation.equals(v.getContentDescription()))!=null,"App folder is listed in the navigation pane");
        View folderLink=find(organizer,v->navigation.equals(v.getContentDescription()));
        tap(folderLink,60);
        await(()->organizer.page().equals("contents:"+recycleFixture),"Folder opens in the same window");
        check(window(recycleFixture)==null,"Ordinary folder navigation opens no second window");
        ui(organizer::goBack);
        check(organizer.page().equals("desktop"),"Back returns to the original folder view");
        tap(find(organizer,v->navigation.equals(v.getContentDescription())),700);
        menuTap("Open in new window");
        Predicate<View> separateOrganizer=v->v instanceof DesktopWindow&&((DesktopWindow)v).toolId.startsWith("@organizer:");
        await(()->find(separateOrganizer)!=null,"The organizer opens its independent window");
        DesktopWindow separate=(DesktopWindow)find(separateOrganizer);
        check(((DesktopOrganizer)separate.body).page().equals("contents:"+recycleFixture)
                &&organizer.page().equals("desktop"),"Explicit open creates an independent window");
        close(separate.toolId);
        tap(find(organizer,v->navigation.equals(v.getContentDescription())),700);
        menuTap("Delete folder");
        await(()->!store.isFolder(recycleFixture)&&find(organizer,v->navigation.equals(v.getContentDescription()))==null,
                "Delete folder clears the navigation pane");
        for(JSONObject item:RecycleBin.list(home))if(recycleFixture.equals(item.optString("shortcut")))RecycleBin.remove(home,item);
        check(!store.isFolder(recycleFixture)&&!store.desktop().contains(recycleFixture),"Permanent removal leaves no live folder");
        close("@organize");
    }

    private void folders()throws Exception{
        progress("separate folder windows and physical cross-window copy/move");File destination=new File(folder,"Destination");check(destination.mkdir(),"Destination exists");byte[] bytes="Drag preserves real bytes — café".getBytes(StandardCharsets.UTF_8);File source=new File(folder,"Source.txt");try(FileOutputStream out=new FileOutputStream(source)){out.write(bytes);}String page=DocumentFolders.rootPage(StorageAccess.tree().toString(),"Internal storage");page=DocumentFolders.childPage(page,DocumentOps.stat(home,StorageAccess.document("Documents"),new DocumentOps.Cancel()));page=DocumentFolders.childPage(page,DocumentOps.stat(home,StorageAccess.document("Documents/"+folder.getName()),new DocumentOps.Cancel()));String sourcePage=page;ui(()->home.open("@files"));VistaExplorer explorer=(VistaExplorer)window("@files").body;ui(()->explorer.navigate(sourcePage));await(()->find(explorer,v->"Destination".equals(v.getContentDescription()))!=null,"Real folder listing");tap(find(explorer,v->"Destination".equals(v.getContentDescription())),700);menuTap("Open in new window");await(()->find(v->v instanceof DesktopWindow&&((DesktopWindow)v).toolId.startsWith("@explorer:"))!=null,"Creates separate Explorer");DesktopWindow second=(DesktopWindow)find(v->v instanceof DesktopWindow&&((DesktopWindow)v).toolId.startsWith("@explorer:"));VistaExplorer target=(VistaExplorer)second.body;await(()->find(target,v->"Empty folder".equals(v.getContentDescription()))!=null,"Independent destination path");await(()->find(v->"Folder windows, 2 windows".equals(v.getContentDescription()))!=null,"Explorer family grouped");tap(find(v->"Folder windows, 2 windows".equals(v.getContentDescription())),700);
        String firstCaption=window("@files").caption();
        awaitNode("Switch to "+firstCaption);awaitNode("Switch to Destination");
        Thread.sleep(650);
        check(node("Switch to "+firstCaption)!=null&&node("Switch to Destination")!=null,"Both previews remain available through a refresh");
        shot("seven-folder-previews.png");menuTap("Switch to Destination");check(second.active(),"Group switches to actual selected folder window");
        ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));await(()->root.getWidth()>root.getHeight(),"Landscape ready");ui(()->{ViewGroup area=(ViewGroup)second.getParent();int width=area.getWidth()/2,height=area.getHeight();window("@files").arrange(0,0,width,height);second.arrange(width,0,width,height);});Thread.sleep(300);await(()->find(explorer,v->"Drag Source.txt".equals(v.getContentDescription()))!=null,"Source icon supports drag");drag(find(explorer,v->"Drag Source.txt".equals(v.getContentDescription())),find(target,v->"Folder contents".equals(v.getContentDescription())));menuTap("Copy here");File copied=new File(destination,"Source.txt");await(()->copied.isFile()&&copied.length()==bytes.length,"Actual drag writes the full copy");check(source.isFile()&&Arrays.equals(bytes,readFile(copied)),"Copy keeps source and exact bytes");shot("vista-folder-windows.png");
        File moveSource=new File(folder,"Move.txt");try(FileOutputStream out=new FileOutputStream(moveSource)){out.write(bytes);}ui(explorer::refreshFiles);await(()->find(explorer,v->"Drag Move.txt".equals(v.getContentDescription()))!=null,"Move source visible");drag(find(explorer,v->"Drag Move.txt".equals(v.getContentDescription())),find(target,v->"Folder contents".equals(v.getContentDescription())));menuTap("Move here");File moved=new File(destination,"Move.txt");await(()->moved.isFile()&&!moveSource.exists(),"Verified drag move completes");check(Arrays.equals(bytes,readFile(moved)),"Move preserves exact bytes");
        ui(()->{home.open("@files");explorer.navigate(sourcePage);});await(()->find(explorer,v->"Destination".equals(v.getContentDescription()))!=null,"Source folder refreshed");tap(find(explorer,v->"Destination".equals(v.getContentDescription())),700);menuTap("Add desktop shortcut");await(()->FolderLinks.list(store).length()>0,"Real folder shortcut saved");check(destination.isDirectory(),"Adding shortcut leaves source intact");DesktopBackup.Plan backup=DesktopBackup.parse(DesktopBackup.snapshot(store).toString());check(backup.string("folder_links","{}").contains("Destination")&&backup.string("taskbar_grouping","").equals("Always"),"Validated backup retains folder links and grouping");String secondId=second.toolId,destinationPage=target.page();MainActivity old=home;resumed.clear();ui(home::recreate);long until=SystemClock.uptimeMillis()+12000;MainActivity next=null;while(SystemClock.uptimeMillis()<until){MainActivity candidate=resumed.poll(300,TimeUnit.MILLISECONDS);if(candidate!=null&&candidate!=old){next=candidate;break;}}check(next!=null,"Real Activity recreation completed");home=next;root=home.getWindow().getDecorView();await(()->window("@files")!=null&&window(secondId)!=null,"Both separate folder windows restored");await(()->((VistaExplorer)window("@files").body).page().equals(sourcePage)&&((VistaExplorer)window(secondId).body).page().equals(destinationPage),"Folder windows restore independent navigation");
    }
    private void drag(View source,View destination)throws Exception{check(source!=null&&destination!=null,"Drag source and destination exist");ui(()->source.requestRectangleOnScreen(new Rect(0,0,source.getWidth(),source.getHeight()),true));Thread.sleep(200);Rect from=new Rect(),to=new Rect();check(source.getGlobalVisibleRect(from)&&destination.getGlobalVisibleRect(to),"Both drag windows have visible targets");long down=SystemClock.uptimeMillis();input(down,MotionEvent.ACTION_DOWN,from.exactCenterX(),from.exactCenterY());Thread.sleep(750);for(int i=1;i<=20;i++){input(down,MotionEvent.ACTION_MOVE,from.exactCenterX()+(to.exactCenterX()-from.exactCenterX())*i/20,from.exactCenterY()+(to.exactCenterY()-from.exactCenterY())*i/20);Thread.sleep(30);}input(down,MotionEvent.ACTION_UP,to.exactCenterX(),to.exactCenterY());Thread.sleep(250);}
    private byte[] readFile(File file)throws Exception{try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();}}
    private void delete(File path){if(path.isDirectory()){File[] children=path.listFiles();if(children!=null)for(File child:children)delete(child);}path.delete();}
    private DesktopWindow window(String id){return (DesktopWindow)findAll(root,v->v instanceof DesktopWindow&&id.equals(((DesktopWindow)v).toolId));}
    private void close(String id)throws Exception{DesktopWindow w=window(id);if(w!=null){if(!w.isShown())ui(()->home.open(id));tap(find(w,v->"Close window".equals(v.getContentDescription())),60);}}
    private View text(View base,String text){return find(base,v->v instanceof TextView&&text.contentEquals(((TextView)v).getText()));}
    private View find(Predicate<View> match){return find(root,match);}private View find(View v,Predicate<View> match){if(v==null||!v.isShown())return null;if(match.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View result=find(((ViewGroup)v).getChildAt(i),match);if(result!=null)return result;}return null;}
    private View findAll(View v,Predicate<View> match){if(v==null)return null;if(match.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View result=findAll(((ViewGroup)v).getChildAt(i),match);if(result!=null)return result;}return null;}
    private void tap(View initial,long hold)throws Exception{check(initial!=null,"Native target exists");String description=initial.getContentDescription()==null?null:initial.getContentDescription().toString(),label=initial instanceof TextView?((TextView)initial).getText().toString():null;View ancestor=initial;while(ancestor.getParent() instanceof View&&!(ancestor instanceof DesktopWindow))ancestor=(View)ancestor.getParent();String scope=ancestor instanceof DesktopWindow?((DesktopWindow)ancestor).toolId:null;View current=initial;
        for(int attempt=0;attempt<5;attempt++){await(()->home.hasWindowFocus(),"Native target focused");getUiAutomation().waitForIdle(150,2500);View base=scope==null?root:window(scope);if(description!=null)current=find(base,v->description.equals(v.getContentDescription()));else if(label!=null)current=text(base,label);if(current==null)continue;View target=current;ui(()->target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true));getUiAutomation().waitForIdle(150,2500);base=scope==null?root:window(scope);if(description!=null)current=find(base,v->description.equals(v.getContentDescription()));else if(label!=null)current=text(base,label);Rect rect=new Rect();if(current!=null&&current.isAttachedToWindow()&&current.isEnabled()&&current.getGlobalVisibleRect(rect)&&!rect.isEmpty()){long down=SystemClock.uptimeMillis();input(down,MotionEvent.ACTION_DOWN,rect.exactCenterX(),rect.exactCenterY());Thread.sleep(hold);input(down,MotionEvent.ACTION_UP,rect.exactCenterX(),rect.exactCenterY());Thread.sleep(100);return;}}throw new AssertionError("Native target has visible bounds: "+description+" / "+label);
    }
    private void doubleTap(View v)throws Exception{Rect r=new Rect();check(v.getGlobalVisibleRect(r),"Caption has visible bounds");for(int i=0;i<2;i++){long t=SystemClock.uptimeMillis();input(t,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());Thread.sleep(35);input(t,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());Thread.sleep(60);}}
    private void input(long down,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(e,true),"Native input injected");e.recycle();}
    private void menuTap(String text)throws Exception{awaitNode(text);getUiAutomation().waitForIdle(150,2500);AccessibilityNodeInfo node=node(text);check(node!=null&&node.isEnabled(),"Enabled menu command: "+text);Rect r=new Rect();node.getBoundsInScreen(r);long t=SystemClock.uptimeMillis();input(t,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());Thread.sleep(55);input(t,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());Thread.sleep(150);}
    private void awaitNode(String text)throws Exception{long end=SystemClock.uptimeMillis()+8000;while(SystemClock.uptimeMillis()<end){if(node(text)!=null)return;Thread.sleep(100);}throw new AssertionError("Native menu node: "+text);}
    private AccessibilityNodeInfo node(String text){
        AccessibilityNodeInfo found=node(getUiAutomation().getRootInActiveWindow(),text);if(found!=null)return found;
        for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()){
            found=node(window.getRoot(),text);if(found!=null)return found;
        }
        return null;
    }private AccessibilityNodeInfo node(AccessibilityNodeInfo n,String text){if(n==null)return null;if(text.contentEquals(n.getText()==null?"":n.getText())||text.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=node(n.getChild(i),text);if(r!=null)return r;}return null;}
    private void ui(Runnable r){runOnMainSync(r);try{Thread.sleep(80);}catch(InterruptedException e){throw new AssertionError(e);}}
    private void await(BooleanSupplier condition,String text)throws Exception{long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){boolean[] ready={false};runOnMainSync(()->ready[0]=condition.getAsBoolean());if(ready[0])return;Thread.sleep(100);}throw new AssertionError(text);}
    private String shell(String command)throws Exception{try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toString("UTF-8");}}
    private void shot(String name)throws Exception{getUiAutomation().waitForIdle(150,2500);Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"Screenshot available");try(FileOutputStream out=getTargetContext().openFileOutput(name,0)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private void dump(AccessibilityNodeInfo n,StringBuilder out,int depth){if(n==null||depth>24)return;out.append(depth).append(' ').append(n.getText()).append(" / ").append(n.getContentDescription()).append('\n');for(int i=0;i<n.getChildCount();i++)dump(n.getChild(i),out,depth+1);}
    private void progress(String text){Bundle result=new Bundle();result.putString("stream","CHECK: "+text+"\n");sendStatus(1,result);}private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
}
