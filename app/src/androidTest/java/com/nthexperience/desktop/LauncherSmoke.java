package com.nthexperience.desktop;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import java.util.concurrent.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import java.util.function.Predicate;

/** Dependency-free on-device smoke test. Run on a disposable emulator. */
public class LauncherSmoke extends Instrumentation {
    private MainActivity activity;
    private View root;
    private final BlockingQueue<MainActivity> resumed=new LinkedBlockingQueue<>();
    private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){
        public void onActivityResumed(Activity a){if(a instanceof MainActivity)resumed.offer((MainActivity)a);}
        public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
    };
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();String previous=null;String oldSticky=getTargetContext().getSharedPreferences("desktop",0).getString("gadget_note","");String oldGadgets=getTargetContext().getSharedPreferences("desktop",0).getString("gadgets",null);progress("runner started");
        ((Application)getTargetContext().getApplicationContext()).registerActivityLifecycleCallbacks(lifecycle);
        try {
            getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putInt("boot",StartupExperience.boot(getTargetContext())).commit();
            migrationChecks();
            Intent launch=new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            progress("starting launcher activity");activity=(MainActivity)startActivitySync(launch);progress("activity started; waiting for UI idle");waitForIdleSync();root=activity.getWindow().getDecorView();
            check(find(v->"Start menu".equals(v.getContentDescription()))!=null,"Desktop has a Start button");snapshot("desktop.png");
            ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());
            check(find(v->"Clock gadget".equals(v.getContentDescription()))!=null,"Clock gadget is visible");
            ListView list=(ListView)find(v->v instanceof ListView);
            for(int i=0;i<40&&list.getCount()==0;i++){Thread.sleep(250);waitForIdleSync();}
            check(list.getCount()>0,"Installed apps loaded");progress("app discovery passed");
            EditText search=(EditText)find(v->v instanceof EditText&&"Search apps, settings, notes and linked files".equals(v.getContentDescription()));
            ui(()->search.setText("__no_such_installed_program__"));check(list.getCount()==0,"Search filters unmatched apps");
            ui(()->search.setText(""));check(list.getCount()>0,"Clearing search restores apps");
            int recentCount=list.getCount();ui(()->find(v->v instanceof TextView&&"All Programs  ›".contentEquals(((TextView)v).getText())).performClick());check(list.getCount()>0&&find(v->v instanceof TextView&&"Accessories".contentEquals(((TextView)v).getText()))!=null,"All Programs exposes its actual Accessories folder");snapshot("start-menu.png");
            progress("opening Notepad");ui(()->activity.open("@notes"));
            EditText editor=(EditText)find(v->v instanceof EditText&&"Notepad text".equals(v.getContentDescription()));
            check(editor!=null,"Notepad opens");previous=editor.getText().toString();
            ui(()->editor.setText("Nth Desktop smoke test"));snapshot("notepad.png");
            ui(()->find(v->"Minimize window".equals(v.getContentDescription())).performClick());
            check(find(v->v instanceof EditText&&"Notepad text".equals(v.getContentDescription()))==null,"Minimize hides the editor");
            ui(()->find(v->"Restore Notepad".equals(v.getContentDescription())).performClick());
            check(find(v->v instanceof EditText&&"Notepad text".equals(v.getContentDescription()))!=null,"Taskbar restores Notepad");
            ui(()->find(v->"Maximize or restore window".equals(v.getContentDescription())).performClick());
            ui(()->find(v->"Maximize or restore window".equals(v.getContentDescription())).performClick());
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            ui(()->activity.open("@notes"));
            EditText reopened=(EditText)find(v->v instanceof EditText&&"Notepad text".equals(v.getContentDescription()));
            check("Nth Desktop smoke test".contentEquals(reopened.getText()),"Note survives closing and reopening its window");
            String original=previous;ui(()->reopened.setText(original));
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            progress("note and window checks passed");ui(()->activity.open("@appearance"));
            ui(()->find(v->"Desktop Background".equals(v.getContentDescription())).performClick());
            check(find(v->"Preview background: Twilight".equals(v.getContentDescription()))!=null,"Personalization opens");
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            ui(()->activity.open("@gadgets"));
            check(find(v->"Add Notes gadget".equals(v.getContentDescription()))!=null,"Gadget gallery offers Notes");
            ui(()->find(v->"Add Notes gadget".equals(v.getContentDescription())).performClick());
            check(new DesktopStore(getTargetContext()).gadgets().contains("notes"),"Adding a gadget is persisted");snapshot("gadget-gallery.png");
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            ui(()->activity.open("@sticky"));EditText sticky=(EditText)find(v->"Sticky note text".equals(v.getContentDescription()));
            check(sticky!=null,"Sticky Notes opens");ui(()->sticky.setText("Remember the little things."));
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            check("Remember the little things.".equals(new DesktopStore(getTargetContext()).prefs.getString("gadget_note","")),"Sticky note saves independently");
            ui(()->new DesktopStore(getTargetContext()).prefs.edit().putString("gadget_note",oldSticky).apply());
            ui(()->activity.sidebarChanged());
            WeatherClient.Snapshot forecast=WeatherClient.Snapshot.parse("{\"properties\":{\"timeseries\":[{\"time\":\"2026-10-31T12:00:00Z\",\"data\":{\"instant\":{\"details\":{\"air_temperature\":25}},\"next_1_hours\":{\"summary\":{\"symbol_code\":\"partlycloudy_day\"}}}}]}}");
            check(forecast.temperature(false)==25&&forecast.temperature(true)==77,"Weather unit conversion");check("Partly cloudy".equals(forecast.condition()),"Weather condition labels");
            check("12.345".equals(WeatherClient.coordinate(12.34567)),"Weather coordinates use limited precision");
            ui(()->activity.gadgetAction("weather"));check(find(v->"Weather city search".equals(v.getContentDescription()))!=null,"Weather can be configured without location permission");
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            ui(()->activity.gadgetAction("clock"));check(find(v->v instanceof android.widget.CheckBox)!=null,"Clock settings opens");
            ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            snapshot("gadgets-desktop.png");
            ui(()->activity.gadgetAction("calendar"));snapshot("calendar.png");ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
            layoutChecks();
            shellChecks();
            desktopFixChecks();
            organizationChecks();
            result.putString("stream","PASS: real desktop grouping and folder editing, folder rotation, Quick Launch ordering, native backup file export/import preview, restore and undo, corrupt backup rejection; desktop shortcut cleanup, real default-home prompts, Vista context menu, cutout alignment in both landscape rotations; Control Panel categories/classic/search, native personalization, Android settings dispatch, Explorer/file-manager folders, real picker grants, nested folder browsing and rotation state; landscape, gadget drag/dock/resize, independent saved layouts, layout lock, rotation and activity recreation with window/cursor state; app discovery, Start search/All Programs, autosave, window controls, wallpapers, gadget gallery, sticky notes, clock settings, weather parsing and configuration.\n");
            restoreGadgets(oldGadgets);((Application)getTargetContext().getApplicationContext()).unregisterActivityLifecycleCallbacks(lifecycle);finish(Activity.RESULT_OK,result);
        } catch(Throwable failure){
            if(previous!=null)getTargetContext().getSharedPreferences("desktop",0).edit().putString("note",previous).commit();
            getTargetContext().getSharedPreferences("desktop",0).edit().putString("gadget_note",oldSticky).commit();restoreGadgets(oldGadgets);
            failure.printStackTrace();try{snapshot("failure.png");StringBuilder dump=new StringBuilder();dumpSystem(getUiAutomation().getRootInActiveWindow(),dump,0);try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(getTargetContext().getFilesDir(),"picker-tree.txt"))){output.write(dump.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}}catch(Exception ignored){}
            ((Application)getTargetContext().getApplicationContext()).unregisterActivityLifecycleCallbacks(lifecycle);result.putString("stream","FAIL: "+failure+"\n");finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void layoutChecks() throws Exception {
        // Exercise first-visit landscape cloning independently of earlier rotation suites.
        ui(()->new DesktopStore(getTargetContext()).prefs.edit().remove("gadget_layout_landscape").commit());
        progress("dragging Clock out of the Sidebar");
        View clock=find(v->"Clock gadget".equals(v.getContentDescription()));check(clock!=null,"Clock available for drag");
        int[] rootLocation=new int[2];root.getLocationOnScreen(rootLocation);
        drag(clock,rootLocation[0]+Math.round(root.getWidth()*.32f),rootLocation[1]+Math.round(root.getHeight()*.28f));
        check(find(v->"Clock floating gadget".equals(v.getContentDescription()))!=null,"Long drag detaches a gadget");
        GadgetLayout portrait=new GadgetLayout(new DesktopStore(getTargetContext()),"portrait");check(!portrait.docked("clock"),"Detached position saved");
        View grip=find(v->"Resize Clock gadget".equals(v.getContentDescription()));check(grip!=null,"Floating gadget has resize grip");
        int[] loc=new int[2];grip.getLocationOnScreen(loc);gesture(grip,loc[0]+grip.getWidth()/2f+35*activity.getResources().getDisplayMetrics().density,loc[1]+grip.getHeight()/2f+35*activity.getResources().getDisplayMetrics().density,false);
        portrait=new GadgetLayout(new DesktopStore(getTargetContext()),"portrait");check(portrait.scale("clock")>1.1f,"Corner grip resizes and persists");String portraitSaved=new DesktopStore(getTargetContext()).prefs.getString("gadget_layout_portrait","");snapshot("floating-gadgets.png");
        ui(()->activity.open("@notes"));EditText editor=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));String oldNote=editor.getText().toString();ui(()->{editor.setText("Landscape remembers my place.");editor.requestFocus();editor.setSelection(9);});
        progress("rotating with Notepad open");rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,Configuration.ORIENTATION_LANDSCAPE);
        check(root.getWidth()>root.getHeight(),"Landscape uses a wide desktop");EditText wideEditor=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));check(wideEditor!=null&&"Landscape remembers my place.".contentEquals(wideEditor.getText()),"Open note survives rotation");check(wideEditor.getSelectionStart()==9,"Cursor survives rotation");snapshot("landscape-notepad.png");
        ui(()->find(v->"Minimize window".equals(v.getContentDescription())).performClick());snapshot("landscape.png");
        ui(()->activity.open("@calendar"));View wideCalendar=find(v->v instanceof MonthCalendarView);check(wideCalendar!=null&&wideCalendar.getHeight()>=Aero.dp(activity,240),"Landscape calendar keeps readable rows in a scrollable grid");snapshot("landscape-calendar.png");android.widget.ScrollView calendarScroll=(android.widget.ScrollView)((View)wideCalendar.getParent()).getParent();int[] calendarLocation=new int[2];calendarScroll.getLocationOnScreen(calendarLocation);gesture(calendarScroll,calendarLocation[0]+calendarScroll.getWidth()/2f,calendarLocation[1]+Aero.dp(activity,12),false);check(calendarScroll.getScrollY()>0,"Landscape calendar scrolls to its lower rows and controls");
        int calendarOffset=calendarScroll.getScrollY();ui(()->((MonthCalendarView)wideCalendar).offset(1));String calendarMonth=((MonthCalendarView)wideCalendar).title();MainActivity beforeCalendarRecreation=activity;resumed.clear();ui(()->activity.recreate());awaitReplacement(beforeCalendarRecreation,Configuration.ORIENTATION_LANDSCAPE);
        MonthCalendarView recreatedCalendar=(MonthCalendarView)find(v->v instanceof MonthCalendarView);android.widget.ScrollView recreatedScroll=(android.widget.ScrollView)((View)recreatedCalendar.getParent()).getParent();check(calendarMonth.equals(recreatedCalendar.title()),"Calendar month survives recreation");check(Math.abs(calendarOffset-recreatedScroll.getScrollY())<=1,"Calendar scroll position survives recreation after layout");ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
        ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());check(find(v->v instanceof TextView&&"Gadgets".contentEquals(((TextView)v).getText()))!=null,"Landscape navigation remains reachable");snapshot("landscape-start.png");
        EditText search=(EditText)find(v->"Search apps, settings, notes and linked files".equals(v.getContentDescription()));ui(()->search.setText("settings"));
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,Configuration.ORIENTATION_PORTRAIT);
        EditText restoredSearch=(EditText)find(v->"Search apps, settings, notes and linked files".equals(v.getContentDescription()));check(restoredSearch!=null&&"settings".contentEquals(restoredSearch.getText()),"Open Start query survives rotation");
        ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());
        check(find(v->"Notepad text".equals(v.getContentDescription()))==null,"Minimized state survives rotation");
        check(portraitSaved.equals(new DesktopStore(getTargetContext()).prefs.getString("gadget_layout_portrait","")),"Portrait layout is unchanged by landscape fitting");
        progress("checking docking, layout lock and recreation");
        View floatingClock=find(v->"Clock gadget".equals(v.getContentDescription()));View dock=find(v->"Desktop gadget sidebar".equals(v.getContentDescription()));int[] dockLoc=new int[2];dock.getLocationOnScreen(dockLoc);drag(floatingClock,dockLoc[0]+dock.getWidth()/2,dockLoc[1]+Math.min(dock.getHeight()-20,40));
        check(find(v->"Clock docked gadget".equals(v.getContentDescription()))!=null,"Dropping on Sidebar docks a gadget");
        check(!new GadgetLayout(new DesktopStore(getTargetContext()),"landscape").docked("clock"),"Docking in portrait leaves landscape placement independent");
        ui(()->new DesktopStore(getTargetContext()).prefs.edit().putBoolean("layout_locked",true).apply());
        View locked=find(v->"Clock gadget".equals(v.getContentDescription()));drag(locked,root.getWidth()*.3f,root.getHeight()*.25f);check(new GadgetLayout(new DesktopStore(getTargetContext()),"portrait").docked("clock"),"Lock prevents gadget moves");
        ui(()->new DesktopStore(getTargetContext()).prefs.edit().putBoolean("layout_locked",false).apply());
        ui(()->activity.open("@notes"));EditText before=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));ui(()->{before.requestFocus();before.setSelection(4);});
        MainActivity previous=activity;resumed.clear();ui(()->activity.recreate());awaitReplacement(previous,Configuration.ORIENTATION_PORTRAIT);
        EditText recreated=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));check(recreated!=null&&recreated.getSelectionStart()==4,"Activity recreation restores window and cursor");ui(()->recreated.setText(oldNote));
        ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());snapshot("restored-portrait.png");
    }
    private void rotate(int requested,int expected) throws Exception {ui(()->activity.setRequestedOrientation(requested));long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){refreshActivity();if(activity.getResources().getConfiguration().orientation==expected&&(expected==Configuration.ORIENTATION_LANDSCAPE?root.getWidth()>root.getHeight():root.getHeight()>root.getWidth())){Thread.sleep(300);waitForIdleSync();return;}Thread.sleep(100);}throw new AssertionError("Orientation did not settle");}
    private void awaitReplacement(MainActivity previous,int orientation) throws Exception {
        long deadline=SystemClock.uptimeMillis()+15000;MainActivity next=null;
        while(SystemClock.uptimeMillis()<deadline){MainActivity candidate=resumed.poll(1,TimeUnit.SECONDS);if(candidate!=null&&candidate!=previous&&candidate.getResources().getConfiguration().orientation==orientation){next=candidate;break;}}
        check(next!=null,"Activity recreated in requested orientation");activity=next;waitForIdleSync();Thread.sleep(300);waitForIdleSync();root=activity.getWindow().getDecorView();
    }
    private void drag(View source,float x,float y) throws Exception {gesture(source,x,y,true);}
    private void gesture(View source,float x,float y,boolean hold) throws Exception {
        check(source!=null,"Gesture source exists");int[] location=new int[2];source.getLocationOnScreen(location);float startX=location[0]+source.getWidth()/2f,startY=location[1]+source.getHeight()/2f;long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,startX,startY);Thread.sleep(hold?700:40);
        for(int i=1;i<=12;i++){inject(down,MotionEvent.ACTION_MOVE,startX+(x-startX)*i/12,startY+(y-startY)*i/12);Thread.sleep(20);}inject(down,MotionEvent.ACTION_UP,x,y);Thread.sleep(300);waitForIdleSync();
    }
    private void inject(long down,int action,float x,float y){MotionEvent.PointerProperties pointer=new MotionEvent.PointerProperties();pointer.id=0;pointer.toolType=MotionEvent.TOOL_TYPE_FINGER;MotionEvent.PointerCoords coordinates=new MotionEvent.PointerCoords();coordinates.x=x;coordinates.y=y;coordinates.pressure=action==MotionEvent.ACTION_UP?0:1;coordinates.size=1;MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,new MotionEvent.PointerProperties[]{pointer},new MotionEvent.PointerCoords[]{coordinates},0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);boolean injected=getUiAutomation().injectInputEvent(event,true);event.recycle();check(injected,"Touch injection accepted");}
    private void shellChecks() throws Exception {
        progress("checking Vista Control Panel and personalization");
        ui(()->activity.open("@control"));snapshot("control-panel.png");
        EditText search=(EditText)find(v->"Search Control Panel".equals(v.getContentDescription()));check(search!=null,"Control Panel has settings search");ui(()->search.setText("wireless"));
        check(find(v->"Wi-Fi".equals(v.getContentDescription()))!=null,"Settings search finds Wi-Fi");
        ActivityMonitor wifi=addMonitor(new android.content.IntentFilter(android.provider.Settings.ACTION_WIFI_SETTINGS),null,true);ui(()->find(v->"Wi-Fi".equals(v.getContentDescription())).performClick());check(wifi.getHits()==1,"Wi-Fi opens the matching Android settings action");removeMonitor(wifi);
        ui(()->search.setText(""));ui(()->((VistaControlPanel)find(v->v instanceof VistaControlPanel)).showFolders());clickSystemText("Classic View");waitForIdleSync();check(new DesktopStore(getTargetContext()).prefs.getBoolean("control_panel_classic",false),"Classic View selection persists");snapshot("control-panel-classic.png");
        ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
        ui(()->activity.open("@appearance"));snapshot("personalization.png");ui(()->find(v->"Desktop Background".equals(v.getContentDescription())).performClick());ui(()->find(v->"Preview background: Twilight".equals(v.getContentDescription())).performClick());ui(()->find(v->v instanceof TextView&&"Save changes".contentEquals(((TextView)v).getText())).performClick());check(new DesktopStore(getTargetContext()).prefs.getInt("wallpaper",0)==1,"Vista background page changes actual wallpaper");ui(()->find(v->"Preview background: Aero".equals(v.getContentDescription())).performClick());ui(()->find(v->v instanceof TextView&&"Save changes".contentEquals(((TextView)v).getText())).performClick());
        ui(()->find(v->"Back".equals(v.getContentDescription())).performClick());ui(()->find(v->"Taskbar and Start Menu".equals(v.getContentDescription())).performClick());android.widget.CheckBox status=(android.widget.CheckBox)find(v->"Show Android status bar".equals(v.getContentDescription()));boolean original=status.isChecked();ui(status::performClick);check(new DesktopStore(getTargetContext()).prefs.getBoolean("show_status_bar",false)!=original,"Native settings checkbox changes the desktop preference");ui(status::performClick);ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
        ui(()->activity.open("@system"));check(find(v->v instanceof TextView&&((TextView)v).getText().toString().contains(android.os.Build.MODEL))!=null,"System page shows real device information");snapshot("system.png");ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
        ui(()->activity.open("@computer"));snapshot("computer.png");rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,Configuration.ORIENTATION_LANDSCAPE);snapshot("computer-landscape.png");ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
        ui(()->activity.open("@filemanagers"));ui(()->find(v->"Add file manager apps".equals(v.getContentDescription())).performClick());
        android.widget.ListView choices=(android.widget.ListView)find(v->v instanceof android.widget.ListView);check(choices!=null&&choices.getCount()>1,"File Managers offers installed apps");
        View choice=find(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Choose file manager app: "));check(choice!=null,"An installed app can be chosen");int position=choices.getPositionForView(choice);ui(()->choices.performItemClick(choice,position,choices.getItemIdAtPosition(position)));check(!new DesktopStore(getTargetContext()).fileManagers().isEmpty(),"File manager shortcuts persist");
        ui(()->find(v->"Back".equals(v.getContentDescription())).performClick());snapshot("file-managers.png");ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
        progress("granting a real test folder through Android's picker");
        String testName="Nth-Desktop-Test-"+SystemClock.uptimeMillis();String testDirectory="/sdcard/Documents/"+testName;
        shell("mkdir -p "+testDirectory+"/Nested");shell("cp /system/etc/hosts "+testDirectory+"/Chapter.txt");shell("cp /system/etc/hosts "+testDirectory+"/Nested/Readme.txt");
        ui(()->activity.open("@files"));ui(()->find(v->"Link a folder".equals(v.getContentDescription())).performClick());
        awaitPicker();enterPickerFolder("Documents");enterPickerFolder(testName);clickPickerText("Use this folder");clickPickerText("Allow");
        awaitView(v->"Chapter.txt".equals(v.getContentDescription()));check(find(v->"Nested".equals(v.getContentDescription()))!=null,"Granted folder children load from the document provider");snapshot("explorer-folder.png");
        org.json.JSONObject link=DocumentFolders.links(new DesktopStore(getTargetContext())).getJSONObject(0);android.net.Uri tree=android.net.Uri.parse(link.getString("uri"));check(getTargetContext().getContentResolver().getPersistedUriPermissions().stream().anyMatch(p->p.getUri().equals(tree)&&p.isReadPermission()),"Folder read grant is retained");
        android.net.Uri chapter=android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,android.provider.DocumentsContract.getTreeDocumentId(tree)+"/Chapter.txt");try(java.io.InputStream input=getTargetContext().getContentResolver().openInputStream(chapter)){byte[] bytes=new byte[128];int count=input==null?0:input.read(bytes);check(count>0&&new String(bytes,0,count,java.nio.charset.StandardCharsets.UTF_8).contains("localhost"),"Selected document is readable with the folder grant");}
        boolean blocked=false;try(android.database.Cursor cursor=getTargetContext().getContentResolver().query(android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,"primary:"),null,null,null,null)){}catch(SecurityException expected){blocked=true;}check(blocked,"Folder grant cannot escape the selected tree");
        EditText fileSearch=(EditText)find(v->"Search this folder".equals(v.getContentDescription()));ui(()->fileSearch.setText("chapter"));check(((ListView)find(v->"Folder contents".equals(v.getContentDescription()))).getCount()==1,"Folder search filters the current directory");ui(()->fileSearch.setText(""));
        ListView listing=(ListView)find(v->"Folder contents".equals(v.getContentDescription()));View nested=find(v->"Nested".equals(v.getContentDescription()));int index=listing.getPositionForView(nested);ui(()->listing.performItemClick(nested,index,listing.getItemIdAtPosition(index)));awaitView(v->"Readme.txt".equals(v.getContentDescription()));
        EditText nestedSearch=(EditText)find(v->"Search this folder".equals(v.getContentDescription()));ui(()->nestedSearch.setText("Read"));rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,Configuration.ORIENTATION_PORTRAIT);awaitView(v->"Readme.txt".equals(v.getContentDescription()));EditText rotated=(EditText)find(v->"Search this folder".equals(v.getContentDescription()));check("Read".contentEquals(rotated.getText()),"Explorer keeps folder and search through rotation");
        ui(()->find(v->"Back".equals(v.getContentDescription())).performClick());awaitView(v->"Chapter.txt".equals(v.getContentDescription()));check(find(v->"Nested".equals(v.getContentDescription()))!=null,"Explorer history survives rotation");
        ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());DocumentFolders.remove(getTargetContext(),new DesktopStore(getTargetContext()),tree.toString());shell("rm -r "+testDirectory);
        progress("Vista shell and real document-provider checks passed");
    }
    private void migrationChecks(){
        DesktopStore store=new DesktopStore(getTargetContext());String shortcuts=store.prefs.getString("shortcuts",null),managers=store.prefs.getString("file_manager_apps",null);boolean had=store.prefs.contains("desktop_v41"),old=store.prefs.getBoolean("desktop_v41",false);
        store.prefs.edit().putString("shortcuts","[\"@files\",\"@computer\",\"example.app/Main\",\"@filemanagers\",\"@notes\"]").putString("file_manager_apps","[\"example.files/Main\"]").remove("desktop_v41").commit();store.migrateDesktop();
        check(store.desktop().equals(java.util.Arrays.asList("@computer","example.app/Main","@notes")),"Upgrade removes only the redundant file icons and preserves shortcut order");check(store.fileManagers().equals(java.util.Arrays.asList("example.files/Main")),"Upgrade preserves file manager selections");
        store.desktop(java.util.Arrays.asList("@computer","@files"));store.migrateDesktop();check(store.pinned("@files"),"Cleanup runs once; later explicit pinning is respected");
        android.content.SharedPreferences.Editor restore=store.prefs.edit().putString("shortcuts",shortcuts).putString("file_manager_apps",managers);if(had)restore.putBoolean("desktop_v41",old);else restore.remove("desktop_v41");restore.commit();
    }
    private void desktopFixChecks() throws Exception {
        progress("checking desktop cleanup, context menu and actual home role");
        check(!new DesktopStore(getTargetContext()).pinned("@files")&&!new DesktopStore(getTargetContext()).pinned("@filemanagers"),"Redundant desktop file icons are removed");
        check(find(v->"Open Files".equals(v.getContentDescription()))!=null,"Taskbar Files shortcut remains available");snapshot("desktop-clean.png");
        ui(()->find(v->"Open Files".equals(v.getContentDescription())).performClick());check(find(v->"Search this folder".equals(v.getContentDescription()))!=null,"Taskbar Files still opens Explorer");ui(()->find(v->"Close window".equals(v.getContentDescription())).performClick());
        check(!activity.isHome(),"Disposable emulator starts with its stock launcher as home");
        ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());ui(()->find(v->"Start menu options".equals(v.getContentDescription())).performClick());check(systemNode(getUiAutomation().getRootInActiveWindow(),node->"Make Nth Desktop home".contentEquals(node.getText()==null?"":node.getText()))!=null,"Start options offer home setup before selection");getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);Thread.sleep(100);ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());
        openDesktopContext();check(systemNode(getUiAutomation().getRootInActiveWindow(),node->node.getText()!=null&&"Make default home".contentEquals(node.getText()))!=null,"Desktop context menu offers setup before selection");clickSystemText("Refresh");
        shell("cmd role add-role-holder --user 0 android.app.role.HOME "+getTargetContext().getPackageName());long deadline=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<deadline){refreshActivity();if(activity.isHome())break;Thread.sleep(150);}check(activity.isHome(),"Android confirms Nth Desktop holds the actual home role");
        ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());ui(()->find(v->"Start menu options".equals(v.getContentDescription())).performClick());check(systemNode(getUiAutomation().getRootInActiveWindow(),node->"Make Nth Desktop home".contentEquals(node.getText()==null?"":node.getText()))==null,"Start options hide setup after default-home selection");getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);Thread.sleep(100);snapshot("default-home-start.png");ui(()->find(v->"Start menu".equals(v.getContentDescription())).performClick());
        openDesktopContext();check(systemNode(getUiAutomation().getRootInActiveWindow(),node->node.getText()!=null&&"Make default home".contentEquals(node.getText()))==null,"Desktop context menu hides setup after default-home selection");snapshot("desktop-context-menu.png");clickSystemText("Refresh");
        progress("checking three-button navigation and cutouts in both landscape directions");
        String overlays=shell("cmd overlay list");if(overlays.contains("com.android.internal.systemui.navbar.threebutton"))shell("cmd overlay enable-exclusive --user 0 --category com.android.internal.systemui.navbar.threebutton");
        String cutout=null;for(String candidate:new String[]{"tall","corner","double"})if(overlays.contains("com.android.internal.display.cutout.emulation."+candidate)){cutout="com.android.internal.display.cutout.emulation."+candidate;break;}check(cutout!=null,"Emulator exposes a real display-cutout overlay");shell("cmd overlay enable-exclusive --user 0 --category "+cutout);Thread.sleep(500);refreshActivity();
        landscapeAngle(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,android.view.Surface.ROTATION_90);edgeBounds();
        if(find(v->"Clock floating gadget".equals(v.getContentDescription()))!=null){ui(()->find(v->"Options for Clock gadget".equals(v.getContentDescription())).performClick());clickSystemText("Dock in Sidebar");Thread.sleep(250);waitForIdleSync();}
        edgeBounds();snapshot("landscape-cutout-left.png");landscapeAngle(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,android.view.Surface.ROTATION_270);edgeBounds();snapshot("landscape-cutout-right.png");shell("cmd overlay disable --user 0 "+cutout);
        progress("desktop cleanup, default home and landscape cutout checks passed");
    }
    private DesktopWindow tool(String id){return (DesktopWindow)find(v->v instanceof DesktopWindow&&id.equals(((DesktopWindow)v).toolId));}
    private void clickIn(String id,String description){View view=find(tool(id),v->description.equals(v.getContentDescription()));check(view!=null,"Tool action is available: "+description);ui(view::performClick);}
    private void organizerCommand(String id,String command)throws Exception{View button=find(tool(id),v->v instanceof TextView&&"Organize ▾".contentEquals(((TextView)v).getText()));check(button!=null,"Shortcut folder exposes its Organize menu");ui(button::performClick);clickSystemText(command);}
    private void organizationChecks() throws Exception {
        progress("grouping real desktop shortcuts with a touch drag");
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,Configuration.ORIENTATION_PORTRAIT);
        awaitView(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Notepad. Hold")&&v.getWidth()>0);
        View internet=find(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(BrowserPanel.NAME+". Hold")),notepad=find(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Notepad. Hold"));int[] target=new int[2];notepad.getLocationOnScreen(target);drag(internet,target[0]+notepad.getWidth()/2f,target[1]+notepad.getHeight()/2f);
        DesktopStore store=new DesktopStore(getTargetContext());check(store.folders().length()==1,"Dropping on an icon creates one app folder");String folder=store.folders().keys().next();check(store.folderItems(folder).equals(java.util.Arrays.asList("@notes","@browser")),"Grouped shortcuts retain their order and IDs");check(!store.pinned("@notes")&&!store.pinned("@browser")&&store.pinned(folder),"Grouping moves shortcuts into the new desktop folder");awaitView(v->v instanceof DesktopWindow&&folder.equals(((DesktopWindow)v).toolId));snapshot("app-folder.png");
        organizerCommand(folder,"Rename folder");EditText name=(EditText)find(tool(folder),v->"Folder name".equals(v.getContentDescription()));ui(()->name.setText("Tools"));clickIn(folder,"Save shortcut name");check("Tools".equals(store.name(folder,"")),"Folder rename is persisted");
        View add=find(tool(folder),v->v instanceof TextView&&"Add programs".contentEquals(((TextView)v).getText()));ui(add::performClick);EditText pickerSearch=(EditText)find(tool(folder),v->"Search shortcuts".equals(v.getContentDescription()));ui(()->pickerSearch.setText("Computer"));CheckBox include=(CheckBox)find(tool(folder),v->"Include Computer".equals(v.getContentDescription()));check(include!=null,"Folder picker offers installed and built-in shortcuts");ui(include::performClick);View done=find(tool(folder),v->v instanceof TextView&&"Done".contentEquals(((TextView)v).getText()));ui(done::performClick);check(store.folderItems(folder).contains("@computer"),"Picker adds a shortcut to the folder");clickIn(folder,"Options for "+BrowserPanel.NAME);clickSystemText("Move up");check(store.folderItems(folder).get(0).equals("@browser"),"Folder menu reorders shortcuts");snapshot("folder-portrait.png");
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,Configuration.ORIENTATION_LANDSCAPE);awaitView(v->v instanceof DesktopWindow&&folder.equals(((DesktopWindow)v).toolId));check("Tools".equals(new DesktopStore(getTargetContext()).name(folder,"")),"Named folder survives rotation");android.widget.AbsListView contents=(android.widget.AbsListView)find(tool(folder),v->"Organized shortcuts".equals(v.getContentDescription()));check(contents!=null,"Rotated folder exposes a native icon grid or list");ui(()->{if(contents instanceof ListView)((ListView)contents).setSelectionFromTop(1,0);else contents.setSelection(0);});check(find(tool(folder),v->("Shortcut: "+BrowserPanel.NAME).equals(v.getContentDescription()))!=null,"Open folder contents survive rotation");snapshot("folder-landscape.png");
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,Configuration.ORIENTATION_PORTRAIT);ui(()->activity.open("@quicklaunch"));clickIn("@quicklaunch","Options for Files");clickSystemText("Move up");check(store.quickLaunch().get(0).equals("@files"),"Quick Launch ordering is editable");snapshot("quick-launch.png");
        ui(()->{store.rename("@notes","Field Notes");store.prefs.edit().putString("note","Research notes — café 🦊").putString("gadget_note","Remember the aurora.").putString("weather_json","cached weather should not be exported").apply();new GadgetLayout(store,"portrait").position("notes",.14f,.22f,.9f);new GadgetLayout(store,"landscape").position("notes",.71f,.35f,1.3f);});
        String portrait=store.prefs.getString("gadget_layout_portrait",""),landscape=store.prefs.getString("gadget_layout_landscape","");org.json.JSONObject saved=DesktopBackup.snapshot(store);DesktopBackup.Plan valid=DesktopBackup.parse(saved.toString());check(valid.folders()==1&&valid.count("quick_launch")==3,"Backup includes app folders and Quick Launch");check(!saved.getJSONObject("settings").has("weather_json"),"Transient weather cache is excluded");backupValidation(saved,store,folder);
        clickIn("@quicklaunch","Close window");if(tool(folder)!=null)clickIn(folder,"Close window");ui(()->activity.open("@organize"));snapshot("desktop-organization.png");clickIn("@organize","Close window");
        progress("exporting a real backup through Android's document picker");
        String fileName="Nth-Desktop-Backup-Test-"+SystemClock.uptimeMillis()+".nthdesktop.json",filePath="/sdcard/Documents/"+fileName;
        ui(()->activity.open("@backup"));clickIn("@backup","Save a backup");awaitExternalText("Save");setPickerFileName(fileName);clickPickerText("Save");awaitView(v->v instanceof TextView&&((TextView)v).getText().toString().contains("Backup saved."));String exported=shell("cat "+filePath);DesktopBackup.Plan plan=DesktopBackup.parse(exported);check(plan.string("note","").equals("Research notes — café 🦊"),"Exported UTF-8 file contains the actual note");check(plan.string("gadget_layout_portrait","").equals(portrait)&&plan.string("gadget_layout_landscape","").equals(landscape),"Export preserves distinct orientation layouts");
        ui(()->activity.open(folder));organizerCommand(folder,"Dissolve folder…");clickIn(folder,"Confirm dissolve folder");check(store.desktop().containsAll(java.util.Arrays.asList("@browser","@notes","@computer")),"Dissolving a folder returns every shortcut to the desktop");
        ui(()->{store.quickLaunch(java.util.Collections.singletonList("@files"));store.rename("@notes","Changed name");store.prefs.edit().putString("note","Changes made after backup").putString("gadget_note","New sticky text").apply();});
        progress("reviewing a real imported file without changing the current setup");clickIn("@backup","Open a backup");awaitExternalText(fileName);clickPickerText(fileName);awaitView(v->"Backup preview summary".equals(v.getContentDescription()));check("Changes made after backup".equals(store.prefs.getString("note",""))&&store.folders().length()==0,"Opening a backup only previews; current data is kept");snapshot("backup-preview.png");
        MainActivity previous=activity;resumed.clear();clickIn("@backup","Restore this backup");awaitReplacement(previous,Configuration.ORIENTATION_PORTRAIT);check("Research notes — café 🦊".equals(store.prefs.getString("note","")),"Restore replaces the note with the saved value");check(store.isFolder(folder)&&"Tools".equals(store.name(folder,"")),"Restore recovers folder IDs and names");check("Field Notes".equals(store.name("@notes","")),"Restore recovers shortcut names");check(store.quickLaunch().get(0).equals("@files")&&store.quickLaunch().size()==3,"Restore recovers Quick Launch order");check(portrait.equals(store.prefs.getString("gadget_layout_portrait",""))&&landscape.equals(store.prefs.getString("gadget_layout_landscape","")),"Restore recovers both saved gadget arrangements");check(find(v->"Backup preview summary".equals(v.getContentDescription()))==null,"Restore starts a fresh desktop rather than replaying old tool state");snapshot("backup-restored.png");
        ui(()->activity.open("@notes"));EditText restored=(EditText)find(v->"Notepad text".equals(v.getContentDescription()));check("Research notes — café 🦊".contentEquals(restored.getText()),"A new editor reads restored data, not stale session text");clickIn("@notes","Close window");
        progress("undoing the restore to recover changes made after the export");ui(()->activity.open("@backup"));clickIn("@backup","Undo last restore");View confirm=find(tool("@backup"),v->v instanceof TextView&&"Undo last restore".equals(v.getContentDescription()));check(confirm!=null,"Undo shows a confirmation step");previous=activity;resumed.clear();ui(confirm::performClick);awaitReplacement(previous,Configuration.ORIENTATION_PORTRAIT);check("Changes made after backup".equals(store.prefs.getString("note",""))&&store.folders().length()==0,"Undo recovers the setup kept immediately before restoring");check(store.quickLaunch().equals(java.util.Collections.singletonList("@files")),"Undo recovers the previous Quick Launch");check(!DesktopBackup.file(getTargetContext(),DesktopBackup.UNDO).exists(),"Undo is consumed after successful recovery");snapshot("backup-recovered.png");
        android.net.Uri savedUri=android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents/"+fileName);try(java.io.OutputStream output=getTargetContext().getContentResolver().openOutputStream(savedUri,"wt")){check(output!=null,"Saved fixture remains writable through the document provider");output.write("broken".getBytes(java.nio.charset.StandardCharsets.UTF_8));}check("broken".equals(DesktopBackup.read(getTargetContext().getContentResolver().openInputStream(savedUri))),"Corrupt fixture bytes are confirmed before import");ui(()->activity.open("@backup"));clickIn("@backup","Open a backup");awaitExternalText(fileName);clickPickerText(fileName);awaitView(v->v instanceof TextView&&((TextView)v).getText().toString().contains("Backup was not opened:"));check("Changes made after backup".equals(store.prefs.getString("note","")),"A corrupt imported file cannot change the desktop");shell("rm "+filePath);
        progress("folder organization and native backup round trip passed");
    }
    private void backupValidation(org.json.JSONObject original,DesktopStore store,String folder)throws Exception {
        progress("rejecting corrupt backup data before any settings are changed");String note=store.prefs.getString("note","");java.util.List<String> desktop=store.desktop();
        org.json.JSONObject wrong=new org.json.JSONObject(original.toString());wrong.put("version",2);rejectBackup(wrong.toString());wrong=new org.json.JSONObject(original.toString());wrong.getJSONObject("settings").remove("note");rejectBackup(wrong.toString());wrong=new org.json.JSONObject(original.toString());wrong.getJSONObject("settings").put("clock_seconds",new org.json.JSONObject().put("type","boolean").put("value","true"));rejectBackup(wrong.toString());
        wrong=new org.json.JSONObject(original.toString());wrong.getJSONObject("settings").getJSONObject("quick_launch").put("value","[\"@files\",\"@files\"]");rejectBackup(wrong.toString());wrong=new org.json.JSONObject(original.toString());org.json.JSONObject folders=new org.json.JSONObject(wrong.getJSONObject("settings").getJSONObject("app_folders").getString("value"));folders.getJSONObject(folder).put("items",new org.json.JSONArray().put(folder));wrong.getJSONObject("settings").getJSONObject("app_folders").put("value",folders.toString());rejectBackup(wrong.toString());rejectBackup(original.toString()+" trailing data");rejectBackup("[".repeat(80)+"0"+"]".repeat(80));
        wrong=new org.json.JSONObject(original.toString());wrong.getJSONObject("settings").getJSONObject("shortcuts").put("value",new org.json.JSONArray().put("folder:"+java.util.UUID.randomUUID()).toString());rejectBackup(wrong.toString());
        boolean encoding=false;try{DesktopBackup.read(new java.io.ByteArrayInputStream(new byte[]{(byte)0xc3,(byte)0x28}));}catch(java.io.IOException expected){encoding=true;}check(encoding,"Invalid UTF-8 is rejected");boolean large=false;try{DesktopBackup.read(new java.io.ByteArrayInputStream(new byte[DesktopBackup.MAX_BYTES+1]));}catch(java.io.IOException expected){large=true;}check(large,"Oversized backup files are rejected");check(note.equals(store.prefs.getString("note",""))&&desktop.equals(store.desktop()),"Rejected backups leave existing settings untouched");
    }
    private void rejectBackup(String text){boolean rejected=false;try{DesktopBackup.parse(text);}catch(org.json.JSONException expected){rejected=true;}check(rejected,"Invalid backup is rejected");}
    protected final void awaitExternalText(String text)throws Exception{long deadline=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<deadline){if(systemNode(getUiAutomation().getRootInActiveWindow(),node->pickerNode(node)&&pickerLabel(node,text))!=null)return;Thread.sleep(150);}throw new AssertionError("Picker did not show: "+text);}
    private void setPickerFileName(String name)throws Exception{android.view.accessibility.AccessibilityNodeInfo field=systemNode(getUiAutomation().getRootInActiveWindow(),node->pickerNode(node)&&"android.widget.EditText".contentEquals(node.getClassName()==null?"":node.getClassName()));check(field!=null,"Native Save picker has a filename field");Bundle args=new Bundle();args.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,name);check(field.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,args),"Native filename can be set");Thread.sleep(150);}
    private void refreshActivity(){MainActivity next;while((next=resumed.poll())!=null)if(!next.isDestroyed())activity=next;root=activity.getWindow().getDecorView();}
    private void openDesktopContext() throws Exception {View desktop=find(v->"Desktop workspace".equals(v.getContentDescription()));check(desktop!=null,"Blank desktop is available");int[] position=new int[2];desktop.getLocationOnScreen(position);float x=position[0]+desktop.getWidth()-Aero.dp(activity,18),y=position[1]+desktop.getHeight()*.55f;long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,x,y);Thread.sleep(750);inject(down,MotionEvent.ACTION_UP,x,y);Thread.sleep(250);check(systemNode(getUiAutomation().getRootInActiveWindow(),node->"Desktop context menu".contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))!=null,"Long press opens the pointer-anchored desktop menu");}
    private void landscapeAngle(int requested,int rotation) throws Exception {refreshActivity();ui(()->activity.setRequestedOrientation(requested));long deadline=SystemClock.uptimeMillis()+12000;boolean ready=false;while(SystemClock.uptimeMillis()<deadline){refreshActivity();if(!activity.isDestroyed()&&activity.getWindowManager().getDefaultDisplay().getRotation()==rotation&&root.getWidth()>root.getHeight()&&root.getRootWindowInsets()!=null){android.view.DisplayCutout cutout=root.getRootWindowInsets().getDisplayCutout();if(cutout!=null&&(cutout.getSafeInsetLeft()>0||cutout.getSafeInsetRight()>0)){ready=true;break;}}Thread.sleep(150);}check(ready,"Requested landscape rotation has a live side cutout");Thread.sleep(300);waitForIdleSync();}
    private void edgeBounds() throws Exception {
        awaitView(v->{android.graphics.Rect visible=new android.graphics.Rect();return v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Computer. Hold")&&v.getGlobalVisibleRect(visible)&&v.getWidth()>0&&v.getHeight()>0;});
        for(String label:new String[]{"Computer",BrowserPanel.NAME,"Notepad","Personalize"})check(find(v->{android.graphics.Rect visible=new android.graphics.Rect();return v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(label+". Hold")&&v.getGlobalVisibleRect(visible)&&v.getWidth()>0&&v.getHeight()>0;})!=null,"Desktop icon remains visible after rotation: "+label);
        paintedDesktopIcon();
        android.view.WindowInsets insets=root.getRootWindowInsets();android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()),navigation=insets.getInsets(android.view.WindowInsets.Type.navigationBars());
        check(navigation.left>0||navigation.right>0||navigation.bottom>0,"On-screen Android navigation remains available");View taskbar=find(v->"Desktop taskbar".equals(v.getContentDescription())),sidebar=find(v->"Desktop gadget sidebar".equals(v.getContentDescription()));check(taskbar!=null&&sidebar!=null,"Taskbar and dock are visible");int[] r=new int[2],t=new int[2],s=new int[2];root.getLocationOnScreen(r);taskbar.getLocationOnScreen(t);sidebar.getLocationOnScreen(s);
        check(Math.abs(t[0]-(r[0]+bars.left))<=1,"Taskbar starts at the usable left edge without a cutout gap");check(Math.abs(t[0]+taskbar.getWidth()-(r[0]+root.getWidth()-bars.right))<=1,"Taskbar reaches the usable right edge without a cutout gap");check(Math.abs(s[0]+sidebar.getWidth()-(r[0]+root.getWidth()-bars.right))<=1,"Gadget dock reaches the usable right edge");
    }
    private void paintedDesktopIcon() throws Exception {
        boolean painted=false;int consecutive=0;long deadline=SystemClock.uptimeMillis()+4000;
        while(SystemClock.uptimeMillis()<deadline){waitForIdleSync();android.graphics.Bitmap actual=getUiAutomation().takeScreenshot();boolean matches=actual!=null&&desktopPaintMatches(actual);if(actual!=null)actual.recycle();if(matches){if(++consecutive>=3){painted=true;break;}}else consecutive=0;Thread.sleep(150);}
        check(painted,"Desktop icon is actually painted in consecutive captured landscape screens");
    }
    private boolean desktopPaintMatches(android.graphics.Bitmap actual){
        View tile=find(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Computer. Hold"));View icon=((ViewGroup)tile).getChildAt(0);android.graphics.Bitmap expected=android.graphics.Bitmap.createBitmap(icon.getWidth(),icon.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);ui(()->{AeroIcon reference=new AeroIcon(activity,"computer");reference.layout(0,0,icon.getWidth(),icon.getHeight());reference.draw(new android.graphics.Canvas(expected));});
        int[] location=new int[2];icon.getLocationOnScreen(location);int matching=0,total=0;
        for(int y=0;y<expected.getHeight();y+=Math.max(1,expected.getHeight()/12))for(int x=0;x<expected.getWidth();x+=Math.max(1,expected.getWidth()/12)){int sample=expected.getPixel(x,y);if(android.graphics.Color.alpha(sample)<250)continue;int sx=location[0]+x,sy=location[1]+y;if(sx<0||sy<0||sx>=actual.getWidth()||sy>=actual.getHeight())continue;int screen=actual.getPixel(sx,sy);total++;if(Math.abs(android.graphics.Color.red(sample)-android.graphics.Color.red(screen))<25&&Math.abs(android.graphics.Color.green(sample)-android.graphics.Color.green(screen))<25&&Math.abs(android.graphics.Color.blue(sample)-android.graphics.Color.blue(screen))<25)matching++;}
        expected.recycle();return total>20&&matching>total*.85f;
    }
    private String shell(String command) throws java.io.IOException {try(android.os.ParcelFileDescriptor descriptor=getUiAutomation().executeShellCommand(command);java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);return output.toString("UTF-8");}}
    private void awaitView(Predicate<View> match) throws Exception {long deadline=SystemClock.uptimeMillis()+8000;while(SystemClock.uptimeMillis()<deadline){MainActivity latest;while((latest=resumed.poll())!=null)if(!latest.isDestroyed()){activity=latest;root=activity.getWindow().getDecorView();}waitForIdleSync();if(find(match)!=null)return;Thread.sleep(150);}throw new AssertionError("Expected native view did not appear");}
    private boolean pickerLabel(android.view.accessibility.AccessibilityNodeInfo node,String text){return node.getText()!=null&&text.equalsIgnoreCase(node.getText().toString())||node.getContentDescription()!=null&&(text.equalsIgnoreCase(node.getContentDescription().toString())||node.getContentDescription().toString().startsWith(text+","));}
    private boolean pickerNode(android.view.accessibility.AccessibilityNodeInfo node){return node.getPackageName()!=null&&!getTargetContext().getPackageName().contentEquals(node.getPackageName());}
    private void enterPickerFolder(String name)throws Exception{
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;getUiAutomation().setServiceInfo(info);
        Predicate<android.view.accessibility.AccessibilityNodeInfo> entered=node->pickerNode(node)&&node.getViewIdResourceName()!=null&&node.getViewIdResourceName().endsWith(":id/breadcrumb_text")&&node.getText()!=null&&name.equals(node.getText().toString());
        for(int attempt=0;attempt<4;attempt++){
            if(systemNode(getUiAutomation().getRootInActiveWindow(),entered)!=null)return;
            clickPickerText(name);long deadline=SystemClock.uptimeMillis()+2000;
            while(SystemClock.uptimeMillis()<deadline){if(systemNode(getUiAutomation().getRootInActiveWindow(),entered)!=null)return;Thread.sleep(100);}
        }
        throw new AssertionError("Folder picker did not enter: "+name);
    }
    private void awaitPicker() throws Exception {long deadline=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<deadline){if(systemNode(getUiAutomation().getRootInActiveWindow(),node->pickerNode(node)&&node.getText()!=null&&"Use this folder".equalsIgnoreCase(node.getText().toString()))!=null){getUiAutomation().waitForIdle(300,5000);Thread.sleep(350);return;}Thread.sleep(150);}throw new AssertionError("Android folder picker did not appear");}
    protected final void clickPickerText(String text) throws Exception {long deadline=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<deadline){android.view.accessibility.AccessibilityNodeInfo node=systemNode(getUiAutomation().getRootInActiveWindow(),v->pickerNode(v)&&pickerLabel(v,text));if(node!=null&&node.isEnabled()&&node.isVisibleToUser()){android.graphics.Rect bounds=new android.graphics.Rect();node.getBoundsInScreen(bounds);if(!bounds.isEmpty()){long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY());Thread.sleep(80);inject(down,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY());Thread.sleep(450);return;}}Thread.sleep(150);}throw new AssertionError("Folder picker item not found: "+text);}
    protected final void tapPickerNode(Predicate<android.view.accessibility.AccessibilityNodeInfo> match) throws Exception {
        long deadline=SystemClock.uptimeMillis()+10000;
        while(SystemClock.uptimeMillis()<deadline){
            android.view.accessibility.AccessibilityNodeInfo node=systemNode(getUiAutomation().getRootInActiveWindow(),v->pickerNode(v)&&match.test(v));
            if(node!=null&&node.isEnabled()&&node.isVisibleToUser()){
                android.graphics.Rect bounds=new android.graphics.Rect();node.getBoundsInScreen(bounds);
                if(!bounds.isEmpty()){long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY());Thread.sleep(80);inject(down,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY());Thread.sleep(450);return;}
            }
            Thread.sleep(150);
        }
        throw new AssertionError("Document picker item not found");
    }
    private void clickSystemText(String text) throws Exception {try{clickSystemNode(node->node.getText()!=null&&text.equalsIgnoreCase(node.getText().toString()));}catch(AssertionError ex){throw new AssertionError("System item not found: "+text,ex);}}
    protected final void clickSystemDescription(String text) throws Exception {clickSystemNode(node->node.getContentDescription()!=null&&(text.equalsIgnoreCase(node.getContentDescription().toString())||node.getContentDescription().toString().equalsIgnoreCase("Open navigation drawer")));}
    protected final void clickSystemNode(Predicate<android.view.accessibility.AccessibilityNodeInfo> match) throws Exception {
        long deadline=SystemClock.uptimeMillis()+10000;
        while(SystemClock.uptimeMillis()<deadline){android.view.accessibility.AccessibilityNodeInfo found=systemNode(getUiAutomation().getRootInActiveWindow(),match);if(found!=null&&found.isEnabled()&&found.isVisibleToUser()){android.graphics.Rect bounds=new android.graphics.Rect();found.getBoundsInScreen(bounds);android.view.accessibility.AccessibilityNodeInfo clickable=found;while(!clickable.isClickable()&&clickable.getParent()!=null)clickable=clickable.getParent();if(clickable.isClickable()&&clickable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)){Thread.sleep(350);return;}if(!bounds.isEmpty()){long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY());Thread.sleep(60);inject(down,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY());Thread.sleep(400);return;}}Thread.sleep(150);}throw new AssertionError("System picker or menu item not found");
    }
    protected final android.view.accessibility.AccessibilityNodeInfo systemNode(android.view.accessibility.AccessibilityNodeInfo node,Predicate<android.view.accessibility.AccessibilityNodeInfo> match){if(node==null)return null;if(match.test(node))return node;for(int i=0;i<node.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo found=systemNode(node.getChild(i),match);if(found!=null)return found;}return null;}
    private void dumpSystem(android.view.accessibility.AccessibilityNodeInfo node,StringBuilder out,int depth){if(node==null||depth>30)return;out.append(depth).append(" ").append(node.getViewIdResourceName()).append(" text=").append(node.getText()).append(" description=").append(node.getContentDescription()).append("\n");for(int i=0;i<node.getChildCount();i++)dumpSystem(node.getChild(i),out,depth+1);}
    private void restoreGadgets(String value){android.content.SharedPreferences.Editor e=getTargetContext().getSharedPreferences("desktop",0).edit();if(value==null)e.remove("gadgets");else e.putString("gadgets",value);e.commit();}
    private void snapshot(String name) throws java.io.IOException {
        try{Thread.sleep(250);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new java.io.IOException(interrupted);}
        waitForIdleSync();
        android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();
        if(bitmap==null)throw new java.io.IOException("Screenshot unavailable");
        java.io.File output=new java.io.File(getTargetContext().getFilesDir(),name);
        try(java.io.FileOutputStream stream=new java.io.FileOutputStream(output)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);if(name.startsWith("landscape-cutout-"))check(desktopPaintMatches(bitmap),"Saved landscape screenshot contains the desktop icon: "+name);}finally{bitmap.recycle();}
    }
    private void progress(String message){Bundle status=new Bundle();status.putString("stream","CHECK: "+message+"\n");sendStatus(1,status);}
    private void ui(Runnable action){runOnMainSync(action);waitForIdleSync();}
    private View find(Predicate<View> match){return find(root,match);}
    private View find(View view,Predicate<View> match){
        if(!view.isShown())return null;
        if(match.test(view))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),match);if(found!=null)return found;}}
        return null;
    }
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
