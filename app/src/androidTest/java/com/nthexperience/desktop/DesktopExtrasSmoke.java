package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import org.json.*;

/** New desktop features exercised against real views, files and Android settings. */
public final class DesktopExtrasSmoke extends Instrumentation {
    private MainActivity home;private View root;private DesktopStore store;private boolean systemPhase,usabilityPhase;private File folder;
    private final Map<String,Object> original=new HashMap<>();private final List<JSONObject> binItems=new ArrayList<>();
    @Override public void onCreate(Bundle args){super.onCreate(args);systemPhase=args!=null&&"system".equals(args.getString("phase"));usabilityPhase=args!=null&&"usability".equals(args.getString("phase"));start();}
    @Override public void onStart(){Bundle result=new Bundle();boolean success=false;try{
        android.accessibilityservice.AccessibilityServiceInfo service=getUiAutomation().getServiceInfo();service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(service);store=new DesktopStore(getTargetContext());original.putAll(store.prefs.getAll());store.prefs.edit().putInt("startup_mode",2).putBoolean("tray_expanded",false).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("brightness_system",false).putInt("brightness_window",-1).putBoolean("desktop_sounds",false).commit();getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();await(()->home.hasWindowFocus(),"Desktop focused");
        if(systemPhase)systemBrightness();else if(usabilityPhase)usability();else{previews();brightness();files();workspaces();settings();}
        result.putString("stream","PASS: "+(usabilityPhase?"enlarged system fonts, visible caption/taskbar targets, keyboard dismissal and preview switching":systemPhase?"real granted Android screen brightness changes and restoration":"physical Switch Windows, minimized-window restoration and mouse hover; privacy exclusion of InPrivate; app brightness and Follow phone; real image thumbnails with bounded decoding; verified Recycle Bin delete, conflict-safe restore, original-byte preservation and cancellation; recent-folder/file and Favorites search; actual saved workspace save/apply, note continuity and backup validation; settings discovery and touch target checks")+".\n");success=true;
    }catch(Throwable failure){try{shot("extras-failure");StringBuilder tree=new StringBuilder();dump(getUiAutomation().getRootInActiveWindow(),tree,0);try(FileOutputStream out=getTargetContext().openFileOutput("extras-tree.txt",0)){out.write(tree.toString().getBytes(StandardCharsets.UTF_8));}}catch(Exception ignored){}StringWriter text=new StringWriter();failure.printStackTrace(new PrintWriter(text));result.putString("stream","FAIL: "+text);
    }finally{try{if(home!=null)ui(()->home.onBackPressed());for(JSONObject item:binItems)try{RecycleBin.remove(getTargetContext(),item);}catch(Exception ignored){}if(folder!=null)delete(folder);if(store!=null){SharedPreferences.Editor e=store.prefs.edit().clear();for(Map.Entry<String,Object> entry:original.entrySet()){Object value=entry.getValue();if(value instanceof String)e.putString(entry.getKey(),(String)value);else if(value instanceof Boolean)e.putBoolean(entry.getKey(),(Boolean)value);else if(value instanceof Integer)e.putInt(entry.getKey(),(Integer)value);else if(value instanceof Long)e.putLong(entry.getKey(),(Long)value);else if(value instanceof Float)e.putFloat(entry.getKey(),(Float)value);}e.commit();}}catch(Exception ignored){}}
        finish(success?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private void flipFirstOpen() throws Exception {
        String[] tools = {"@notes", "@calendar", "@sticky", "@computer", "@files"};
        List<String> captions = new ArrayList<>();
        String originalScale = Settings.Global.getString(home.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE);
        try {
            setAnimatorScale("1");
            store.prefs.edit().putBoolean("reduce_motion", false).commit();
            await(() -> Aero.motion(home), "Flip 3D runs with animations enabled");
            for (int count = 0; count <= tools.length; count++) {
                if (count > 0) {
                    String tool = tools[count - 1];
                    ui(() -> home.open(tool));
                    await(() -> window(tool) != null, "Window opened: " + tool);
                    captions.add(window(tool).caption());
                }
                progress("Flip 3D renders before input with " + count + " open windows");
                ui(() -> home.open("@flip3d"));
                for (String caption : captions) awaitFlipCard(caption);
                // The desktop starts just beyond the five visible cards in a six-card deck.
                if (count == tools.length) nodeTap("Next window");
                awaitFlipCard("Show Desktop");
                nodeTap("Close Flip 3D");
            }
        } finally {
            setAnimatorScale(originalScale);
            store.prefs.edit().putBoolean("reduce_motion", true).commit();
            ui(() -> {
                for (String tool : tools) {
                    DesktopWindow opened = window(tool);
                    if (opened != null) opened.closeWindow();
                }
            });
        }
    }
    private void setAnimatorScale(String scale) throws IOException {
        String setting = "global " + Settings.Global.ANIMATOR_DURATION_SCALE;
        String command = scale == null ? "settings delete " + setting : "settings put " + setting + " " + Float.parseFloat(scale);
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command))) {
            read(in);
        }
    }
    private void awaitFlipCard(String caption) throws Exception {
        long end = SystemClock.uptimeMillis() + 4000;
        Rect bounds = new Rect();
        while (SystemClock.uptimeMillis() < end) {
            AccessibilityNodeInfo card = node("Flip to " + caption);
            if (card != null && card.isVisibleToUser()) {
                card.getBoundsInScreen(bounds);
                if (bounds.width() >= Aero.dp(home, 24) && bounds.height() >= Aero.dp(home, 24)) return;
            }
            Thread.sleep(80);
        }
        throw new AssertionError("Flip 3D card has no visible bounds: " + caption);
    }
    private void previews()throws Exception{
        flipFirstOpen();
        ui(()->home.open("@notes"));DesktopWindow notes=window("@notes");
        ui(()->home.open("@flip3d"));awaitFlipCard("Notepad");awaitFlipCard("Show Desktop");
        nodeTap("Next window");awaitNode("Selected window: Show Desktop");
        nodeTap("Previous window");awaitNode("Selected window: Notepad");nodeTap("Switch to window");
        progress("native window overview and pointer preview");EditText edit=(EditText)find(notes,v->"Notepad text".equals(v.getContentDescription()));ui(()->edit.setText("Extras keeps this note"));ui(()->home.open("@calendar"));DesktopWindow calendar=window("@calendar");ui(calendar::minimize);
        View arrow = find(root, v -> "Device controls".equals(v.getContentDescription()));
        tap(arrow, 60);
        View taskManager = find(root, v -> "Task Manager".equals(v.getContentDescription()));
        check(taskManager != null && taskManager.getWidth() == arrow.getWidth(), "Accessible Task Manager shortcut");
        tap(taskManager, 60);
        await(() -> window("@taskmanager") != null, "Taskbar shortcut opens Task Manager");
        ui(() -> window("@taskmanager").closeWindow());
        tap(arrow, 60);
        tap(find(root, v -> "Taskbar commands".equals(v.getContentDescription())), 700);
        nodeTap("Switch Windows");
        awaitNode("Switch to Calendar");shot("window-switcher");nodeTap("Switch to Calendar");await(()->calendar.isShown()&&calendar.active(),"Overview restores actual minimized Calendar");check(edit.getText().toString().equals("Extras keeps this note"),"Same note contents after switching");
        View task=find(root,v->v.getContentDescription()!=null&&v.getContentDescription().toString().equals("Minimize Calendar"));check(task!=null,"Actual task button");ui(()->task.requestRectangleOnScreen(new Rect(0,0,task.getWidth(),task.getHeight()),true));Thread.sleep(200);Rect bounds=new Rect();check(task.getGlobalVisibleRect(bounds),"Task button visible");mouse(bounds.exactCenterX(),bounds.exactCenterY());awaitNode("Window preview for Calendar");shot("window-hover");mouse(root.getWidth()-10,10);long end=SystemClock.uptimeMillis()+3000;while(node("Window preview for Calendar")!=null&&SystemClock.uptimeMillis()<end)Thread.sleep(80);check(node("Window preview for Calendar")==null,"Moving the pointer away closes the actual preview");
        ui(calendar::minimize);
        tap(find(root,v->"Taskbar commands".equals(v.getContentDescription())),700);
        nodeTap("Flip 3D");
        awaitNode("Previous window");
        awaitNode("Flip to Calendar");
        awaitNode("Flip to Show Desktop");
        nodeTap("Next window");
        nodeTap("Previous window");
        shot("vista-flip-3d");
        for(int i=0;i<4&&node("Selected window: Calendar")==null;i++)nodeTap("Next window");
        awaitNode("Selected window: Calendar");
        flipSwipe(false);
        getUiAutomation().waitForIdle(300,2500);
        check(node("Selected window: Calendar")==null,"A left swipe advances the stack");
        flipSwipe(true);
        awaitNode("Selected window: Calendar");
        nodeTap("Switch to window");
        await(()->calendar.isShown()&&calendar.active(),"Flip 3D restores the actual window");
        ui(()->home.open("@browser"));BrowserPanel browser=(BrowserPanel)window("@browser").body;if(browser.privateAvailable()){ui(()->browser.addTab("",true));ui(()->home.open("@switcher"));awaitNode("InPrivate — preview hidden");shot("window-private-preview");nodeTap("Close");
            ui(()->home.open("@flip3d"));awaitNode("Flip to "+BrowserPanel.NAME+". InPrivate — preview hidden");nodeTap("Close Flip 3D");}
        ui(()->window("@browser").closeWindow());
    }
    private void flipSwipe(boolean right)throws Exception{
        Rect bounds=new Rect();
        getUiAutomation().getRootInActiveWindow().getBoundsInScreen(bounds);
        float left=bounds.left+bounds.width()*.30f,rightEdge=bounds.left+bounds.width()*.70f;
        float start=right?left:rightEdge,end=right?rightEdge:left,y=bounds.top+bounds.height()*.46f;
        long down=SystemClock.uptimeMillis();
        event(down,MotionEvent.ACTION_DOWN,start,y);
        for(int i=1;i<=8;i++){Thread.sleep(22);event(down,MotionEvent.ACTION_MOVE,start+(end-start)*i/8,y);}
        event(down,MotionEvent.ACTION_UP,end,y);
        Thread.sleep(300);
    }
    private void openBrightness() throws Exception {
        tap(find(root, v -> "Device controls".equals(v.getContentDescription())), 700);
        nodeTap("Brightness control");
    }

    private void brightness()throws Exception{
        progress("native brightness control without system access");check(!Settings.System.canWrite(home),"App-only phase has no system-write grant");int prior=Settings.System.getInt(home.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128);openBrightness();awaitNode("Brightness");View slider=find(root,v->v instanceof SeekBar&&"Brightness".equals(v.getContentDescription()));check(slider!=null,"Brightness slider attached to actual flyout");drag((SeekBar)slider,.6f);await(()->home.getWindow().getAttributes().screenBrightness>.4f,"Activity brightness actually changes");check(Settings.System.getInt(home.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128)==prior,"App-only mode leaves shared setting unchanged");shot("tray-brightness");nodeTap("Follow phone brightness");await(()->home.getWindow().getAttributes().screenBrightness<0,"Follow phone removes window override");nodeTap("Close tray flyout");
    }
    private void systemBrightness()throws Exception{
        progress("native granted whole-phone brightness");check(Settings.System.canWrite(home),"Separate system-write phase has the real app-op grant");int level=Settings.System.getInt(home.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128),mode=Settings.System.getInt(home.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,0);
        try{openBrightness();nodeTap("Use whole-phone control");awaitNode("Whole-phone brightness: "+Math.round(level*100f/255)+"%");
            SeekBar[] slider=new SeekBar[1];await(()->{slider[0]=(SeekBar)find(root,v->v instanceof SeekBar&&"Brightness".equals(v.getContentDescription()));return slider[0]!=null&&slider[0].getGlobalVisibleRect(new Rect());},"System brightness slider is laid out");drag(slider[0],.35f);check(Settings.System.getInt(home.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,-1)==0,"System slider explicitly selects manual mode");int actual=Settings.System.getInt(home.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,-1);check(actual>20&&actual<160,"Real shared brightness is changed by physical slider");check(home.getWindow().getAttributes().screenBrightness<0,"System mode has no competing window override");shot("tray-brightness-system");}finally{Settings.System.putInt(home.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,level);Settings.System.putInt(home.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,mode);}
    }
    private void files()throws Exception{
        progress("actual file thumbnails, verified deletion and restoration");check(StorageAccess.has(home),"Native file access enabled for isolated fixtures");folder=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"Nth-Extras-"+UUID.randomUUID());check(folder.mkdirs(),"Create isolated native fixture folder");File image=new File(folder,"Preview.png");Bitmap bitmap=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xffab371d);try(FileOutputStream out=new FileOutputStream(image)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();File note=new File(folder,"Keep.txt");byte[] bytes="Recovery keeps every byte \u2603".getBytes(StandardCharsets.UTF_8);try(FileOutputStream out=new FileOutputStream(note)){out.write(bytes);}
        String page=StorageAccess.page("documents");DocumentFolders.Listing list=DocumentFolders.read(home,page,new CancellationSignal());DocumentFolders.Item child=list.items.stream().filter(i->i.name.equals(folder.getName())).findFirst().orElseThrow();page=DocumentFolders.childPage(page,child);String fixturePage=page;
        ui(()->home.open("@files"));VistaExplorer explorer=(VistaExplorer)window("@files").body;ui(()->explorer.navigate(fixturePage));await(()->find(explorer,v->"Preview.png".equals(v.getContentDescription()))!=null,"Image row loaded");
        View imageRow=find(explorer,v->"Preview.png".equals(v.getContentDescription()));await(()->find(imageRow,v->v instanceof ImageView&&((ImageView)v).getDrawable() instanceof BitmapDrawable)!=null,"Actual thumbnail bitmap decoded");ImageView thumb=(ImageView)find(imageRow,v->v instanceof ImageView&&((ImageView)v).getDrawable() instanceof BitmapDrawable);Bitmap small=((BitmapDrawable)thumb.getDrawable()).getBitmap();check(small.getWidth()<=192&&small.getHeight()<=192,"Thumbnail decode is bounded");check(Color.red(small.getPixel(small.getWidth()/2,small.getHeight()/2))>120,"Thumbnail contains actual image pixels");shot("explorer-thumbnails");
        View row=find(explorer,v->"Keep.txt".equals(v.getContentDescription()));tap(row,700);nodeTap("Delete…");nodeTap("Cancel");check(note.isFile(),"Cancel leaves actual file");tap(row,700);nodeTap("Delete…");nodeTap("Move to Recycle Bin");await(()->!note.exists(),"Original removed after verified stash");List<JSONObject> bin=RecycleBin.list(home);JSONObject item=bin.stream().filter(i->i.optString("name").equals("Keep.txt")).findFirst().orElseThrow();binItems.add(item);check(item.optBoolean("deleted"),"Durable deletion receipt");
        try(FileOutputStream out=new FileOutputStream(note)){out.write("New original".getBytes(StandardCharsets.UTF_8));}ui(()->home.open("@recycle"));await(()->find(root,v->"Recycle item: Keep.txt".equals(v.getContentDescription()))!=null,"Recycle Bin Restore available");tap(find(root,v->"Recycle item: Keep.txt".equals(v.getContentDescription())),60);tap(find(root,v->v instanceof TextView&&"Restore this item".contentEquals(((TextView)v).getText())),60);await(()->new File(folder,"Keep - Copy.txt").exists(),"Restore avoids existing filename");check(Arrays.equals(bytes,java.nio.file.Files.readAllBytes(new File(folder,"Keep - Copy.txt").toPath())),"Restored bytes exact");check(new String(java.nio.file.Files.readAllBytes(note.toPath()),StandardCharsets.UTF_8).equals("New original"),"Existing file untouched");check(RecycleBin.list(home).stream().noneMatch(i->item.optString("id").equals(i.optString("id"))),"Verified restore removes recovery entry");shot("recycle-bin");
        check(RecentFiles.list(store).toString().contains(folder.getName()),"Actual navigation adds recent folder");check(StartSearch.find(store,new ArrayList<>(),folder.getName()).stream().anyMatch(r->r.group.equals("Recent Items")),"Recent folder searchable");BrowserData browser=new BrowserData(home);String favorites=browser.list("favorites").toString();browser.remember("favorites","Nth extras bookmark","https://example.org/extra",40);check(StartSearch.find(store,new ArrayList<>(),"Nth extras bookmark").stream().anyMatch(r->r.group.equals("Favorites")),"Real Favorites are searchable");browser.list("favorites",new JSONArray(favorites));
        DocumentOps.Ref source=new DocumentOps.Ref(DocumentsContract.buildDocumentUriUsingTree(DocumentOps.current(fixturePage),"primary:Documents/"+folder.getName()+"/Keep.txt"),DocumentOps.current(fixturePage),DocumentOps.stat(home,DocumentsContract.buildDocumentUriUsingTree(DocumentOps.current(fixturePage),"primary:Documents/"+folder.getName()+"/Keep.txt"),new DocumentOps.Cancel()));DocumentOps.Cancel cancelled=new DocumentOps.Cancel();cancelled.cancel();try{RecycleBin.trash(home,source,cancelled);throw new AssertionError("Cancelled deletion accepted");}catch(OperationCanceledException expected){}check(note.exists(),"Cancelled bin operation preserves source");
        File large=new File(folder,"Too-large.bin");try(RandomAccessFile data=new RandomAccessFile(large,"rw")){data.setLength(RecycleBin.FILE_LIMIT+1);}DocumentOps.Ref largeRef=new DocumentOps.Ref(StorageAccess.document("Documents/"+folder.getName()+"/Too-large.bin"),DocumentOps.current(fixturePage),DocumentOps.stat(home,StorageAccess.document("Documents/"+folder.getName()+"/Too-large.bin"),new DocumentOps.Cancel()));try{RecycleBin.trash(home,largeRef,new DocumentOps.Cancel());throw new AssertionError("Oversized deletion accepted");}catch(IOException expected){}check(large.isFile(),"Quota refusal keeps original");large.delete();
        File damaged=new File(folder,"Damaged.txt");try(FileOutputStream out=new FileOutputStream(damaged)){out.write(bytes);}Uri damagedUri=StorageAccess.document("Documents/"+folder.getName()+"/Damaged.txt");RecycleBin.trash(home,new DocumentOps.Ref(damagedUri,DocumentOps.current(fixturePage),DocumentOps.stat(home,damagedUri,new DocumentOps.Cancel())),new DocumentOps.Cancel());JSONObject recovery=RecycleBin.list(home).stream().filter(i->i.optString("name").equals("Damaged.txt")).findFirst().orElseThrow();binItems.add(recovery);File payload=new File(home.getFilesDir(),"recycle-bin/"+recovery.getString("id")+".data");try(FileOutputStream out=new FileOutputStream(payload)){out.write("damaged".getBytes(StandardCharsets.UTF_8));}try{RecycleBin.restore(home,recovery,new DocumentOps.Cancel());throw new AssertionError("Corrupt recovery accepted");}catch(IOException expected){}check(payload.isFile()&&!damaged.exists(),"Damaged recovery copy stays retained and is not restored");
        ui(()->window("@recycle").closeWindow());ui(()->window("@files").closeWindow());safRecovery();
    }
    private void safRecovery()throws Exception{
        progress("cross-UID linked-provider recovery");String token=UUID.randomUUID().toString();ui(()->home.startActivity(new Intent().setComponent(new ComponentName("com.nthexperience.desktop.fixture","com.nthexperience.desktop.fixture.GrantActivity")).putExtra("receipt",token)));long end=SystemClock.uptimeMillis()+12000;boolean granted=false;while(SystemClock.uptimeMillis()<end){try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("run-as com.nthexperience.desktop.fixture cat files/grant-receipt.txt");InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){if(token.equals(new String(read(in),StandardCharsets.UTF_8))){granted=true;break;}}Thread.sleep(100);}check(granted,"Real separate-UID provider grants access");await(()->home.hasWindowFocus(),"Grant Activity returns to launcher");
        Uri tree=DocumentsContract.buildTreeDocumentUri("com.nthexperience.desktop.fixture.documents","root:"),parent=DocumentsContract.buildDocumentUriUsingTree(tree,"root:"),uri=DocumentsContract.buildDocumentUriUsingTree(tree,"root:Source.txt");byte[] original;try(InputStream in=home.getContentResolver().openInputStream(uri)){original=read(in);}RecycleBin.trash(home,new DocumentOps.Ref(uri,parent,DocumentOps.stat(home,uri,new DocumentOps.Cancel())),new DocumentOps.Cancel());JSONObject item=RecycleBin.list(home).stream().filter(i->uri.toString().equals(i.optString("uri"))).findFirst().orElseThrow();binItems.add(item);Uri restored=RecycleBin.restore(home,item,new DocumentOps.Cancel());try(InputStream in=home.getContentResolver().openInputStream(restored)){check(Arrays.equals(original,read(in)),"Separate-UID SAF restore preserves exact source bytes");}
    }
    private void workspaces()throws Exception{
        progress("save and reopen real window layout");ui(()->home.open("@notes"));DesktopWindow notes=window("@notes");ui(()->notes.arrange(12,30,300,300));JSONObject geometry=notes.geometry();ui(()->home.open("@workspaces"));nodeTap("Save current workspace…");awaitNode("Workspace name");AccessibilityNodeInfo input=node("Workspace name");Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Extras research");check(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"Enter actual workspace name");nodeTap("Save");nodeTap("Close");check(WorkspacePresets.list(store).toString().contains("Extras research"),"Named layout saved");ui(notes::maximize);ui(()->home.open("@workspaces"));nodeTap("Open Extras research");await(()->!notes.maximized(),"Workspace applies saved normal geometry");check(Math.abs(notes.geometry().optDouble("x")-geometry.optDouble("x"))<.1,"Saved position restored");check(store.prefs.getString("note","").equals("Extras keeps this note"),"Layout switching preserves saved note");DesktopBackup.parse(DesktopBackup.snapshot(store).toString());shot("saved-workspace");
    }
    private void usability()throws Exception{
        progress("enlarged fonts and actual keyboard/window switching");check(home.getResources().getConfiguration().fontScale>=1.25f,"Real system font scale is enlarged");ui(()->home.open("@notes"));DesktopWindow notes=window("@notes");EditText edit=(EditText)find(notes,v->"Notepad text".equals(v.getContentDescription()));ui(()->{edit.setText("Larger fonts keep this text");edit.requestFocus();((android.view.inputmethod.InputMethodManager)home.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(edit,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
        android.view.inputmethod.InputMethodManager input=(android.view.inputmethod.InputMethodManager)home.getSystemService(Context.INPUT_METHOD_SERVICE);boolean hasKeyboard=!input.getInputMethodList().isEmpty();if(hasKeyboard)await(()->root.getRootWindowInsets()!=null&&root.getRootWindowInsets().isVisible(WindowInsets.Type.ime()),"Installed keyboard opens");
        ui(()->home.open("@switcher"));awaitNode("Switch to Notepad");if(hasKeyboard)await(()->root.getRootWindowInsets()!=null&&!root.getRootWindowInsets().isVisible(WindowInsets.Type.ime()),"Switcher dismisses actual keyboard before showing previews");nodeTap("Switch to Notepad");
        check(edit.getText().toString().equals("Larger fonts keep this text"),"Typed content retained");for(String name:new String[]{"Minimize window","Maximize or restore window","Close window"}){View button=find(notes,v->name.equals(v.getContentDescription()));Rect bounds=new Rect();check(button!=null&&button.getGlobalVisibleRect(bounds)&&bounds.width()>=Aero.dp(home,40)&&bounds.height()>=Aero.dp(home,40),"Unclipped caption target at enlarged font: "+name);}
        for(String name:new String[]{"Device controls"}){View button=find(root,v->name.equals(v.getContentDescription()));Rect bounds=new Rect();check(button!=null&&button.isFocusable()&&button.getGlobalVisibleRect(bounds)&&bounds.width()>=Aero.dp(home,40),"Accessible phone taskbar target: "+name);}shot("desktop-large-fonts");
    }
    private void settings()throws Exception{progress("shared settings and touch controls");ui(()->home.open("setting:page:sounds"));await(()->find(root,v->"Enable desktop sound effects".equals(v.getContentDescription()))!=null,"Sound settings routed through Control Panel");check(!store.prefs.getBoolean("desktop_sounds",false),"Sounds opt-in by default");ui(()->home.open("@recent"));await(()->find(root,v->"Remember recent documents and folders".equals(v.getContentDescription()))!=null,"History controls discoverable");shot("recent-items");}
    private DesktopWindow window(String id){return (DesktopWindow)findAll(root,v->v instanceof DesktopWindow&&id.equals(((DesktopWindow)v).toolId));}
    private View find(View v,Predicate<View> match){if(v==null||!v.isShown())return null;return findAll(v,match);}
    private View findAll(View v,Predicate<View> match){if(v==null)return null;if(match.test(v)&&v.isShown())return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View result=findAll(((ViewGroup)v).getChildAt(i),match);if(result!=null)return result;}return null;}
    private void tap(View view,long hold)throws Exception{check(view!=null,"Native target exists");ui(()->view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),true));getUiAutomation().waitForIdle(150,2500);Rect r=new Rect();check(view.isShown()&&view.getGlobalVisibleRect(r)&&!r.isEmpty(),"Native target visible: "+view.getContentDescription());touch(r.exactCenterX(),r.exactCenterY(),hold);}
    private void touch(float x,float y,long hold)throws Exception{long t=SystemClock.uptimeMillis();event(t,MotionEvent.ACTION_DOWN,x,y);Thread.sleep(hold);event(t,MotionEvent.ACTION_UP,x,y);Thread.sleep(150);}
    private void mouse(float x,float y){MotionEvent.PointerProperties properties=new MotionEvent.PointerProperties();properties.id=0;properties.toolType=MotionEvent.TOOL_TYPE_MOUSE;MotionEvent.PointerCoords point=new MotionEvent.PointerCoords();point.x=x;point.y=y;long now=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,MotionEvent.ACTION_HOVER_MOVE,1,new MotionEvent.PointerProperties[]{properties},new MotionEvent.PointerCoords[]{point},0,0,1,1,0,0,InputDevice.SOURCE_MOUSE,0);check(getUiAutomation().injectInputEvent(event,true),"Mouse pointer movement injected");event.recycle();}
    private void drag(SeekBar view,float fraction)throws Exception{check(view!=null,"Actual slider");Rect r=new Rect();check(view.getGlobalVisibleRect(r),"Slider visible");long t=SystemClock.uptimeMillis();float start=r.left+r.width()*.5f,end=r.left+r.width()*fraction;event(t,0,start,r.exactCenterY());for(int i=1;i<=6;i++){Thread.sleep(30);event(t,2,start+(end-start)*i/6,r.exactCenterY());}event(t,1,end,r.exactCenterY());Thread.sleep(200);}
    private void event(long down,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(e,true),"Physical input injected");e.recycle();}
    private void nodeTap(String text)throws Exception{awaitNode(text);getUiAutomation().waitForIdle(150,2500);AccessibilityNodeInfo n=node(text);n.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());getUiAutomation().waitForIdle(150,2500);n=node(text);Rect r=new Rect();n.getBoundsInScreen(r);check(n.isEnabled()&&!r.isEmpty(),"Action visible: "+text);touch(r.exactCenterX(),r.exactCenterY(),65);}
    private void awaitNode(String text)throws Exception{long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){if(node(text)!=null)return;Thread.sleep(100);}throw new AssertionError("Native node: "+text);}
    private AccessibilityNodeInfo node(String text){AccessibilityNodeInfo found=node(getUiAutomation().getRootInActiveWindow(),text);if(found!=null)return found;for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()){found=node(window.getRoot(),text);if(found!=null)return found;}return null;}
    private AccessibilityNodeInfo node(AccessibilityNodeInfo n,String text){if(n==null)return null;if(text.contentEquals(n.getText()==null?"":n.getText())||text.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=node(n.getChild(i),text);if(found!=null)return found;}return null;}
    private void await(BooleanSupplier condition,String text)throws Exception{long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){boolean[] yes={false};runOnMainSync(()->yes[0]=condition.getAsBoolean());if(yes[0])return;Thread.sleep(100);}throw new AssertionError(text);}
    private void ui(Runnable r){runOnMainSync(r);try{Thread.sleep(80);}catch(InterruptedException ex){throw new AssertionError(ex);}}
    private void shot(String name)throws Exception{getUiAutomation().waitForIdle(300,3000);Thread.sleep(150);Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"Screenshot available");try(FileOutputStream out=getTargetContext().openFileOutput(name+".png",0)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private void dump(AccessibilityNodeInfo n,StringBuilder out,int depth){if(n==null||depth>24)return;out.append(depth).append(' ').append(n.getText()).append(" / ").append(n.getContentDescription()).append('\n');for(int i=0;i<n.getChildCount();i++)dump(n.getChild(i),out,depth+1);}
    private byte[] read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();}
    private void delete(File file){if(file.isDirectory()){File[] list=file.listFiles();if(list!=null)for(File child:list)delete(child);}file.delete();}
    private void progress(String text){Bundle r=new Bundle();r.putString("stream","CHECK: "+text+"\n");sendStatus(1,r);}private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
}
