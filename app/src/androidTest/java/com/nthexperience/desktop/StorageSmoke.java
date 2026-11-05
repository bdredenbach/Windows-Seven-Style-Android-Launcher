package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.json.*;

/** Real shared storage, setup ordering and restricted URI grants. Disposable emulator only. */
public final class StorageSmoke extends ExplorerSmoke {
    private String phase="setup";private boolean own;private Activity current;private MainActivity home;private View root;private String fixtureName;private Uri fixture;
    private final BlockingQueue<Activity> resumed=new LinkedBlockingQueue<>();
    private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){public void onActivityResumed(Activity a){resumed.offer(a);}public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}};
    @Override public void onCreate(Bundle args){own=args!=null&&"storage".equals(args.getString("suite"));if(args!=null)phase=args.getString("phase","setup");super.onCreate(args);}
    @Override public void onStart(){if(!own){super.onStart();return;}Bundle result=new Bundle();Application app=(Application)getTargetContext().getApplicationContext();app.registerActivityLifecycleCallbacks(lifecycle);try{
        DesktopStore store=new DesktopStore(getTargetContext());
        if(phase.equals("home-upgrade")){
            progress("checking the first Home offer on an existing Seven 0.1.0 profile");
            SharedPreferences session=getTargetContext().getSharedPreferences(StartupExperience.SESSION,0);
            session.edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putInt("runs",1)
                    .remove(StartupExperience.HOME_OFFERED).commit();
            getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();
            store.prefs.edit().putInt("startup_mode",2).putBoolean("welcome_on_start",false).commit();
            startDesktop();
            dismissHomeOffer(session,"home-upgrade.png");
            await(()->home.hasWindowFocus(),"Seven returns after Cancel");
            check(!home.isHome(),"Cancel leaves the selected Android launcher in place");
            check(session.getInt("runs",0)==1&&node("Set up Nth Explorer")==null,"Upgrade keeps completed startup and file setup");
            recreate(MainActivity.class);
            await(()->home.hasWindowFocus(),"The recreated desktop remains focused");
            check(!systemHomePrompt(),"The offer is made only once after upgrading");
            app.unregisterActivityLifecycleCallbacks(lifecycle);
            result.putString("stream","PASS: the first Home offer reaches existing Seven users with startup disabled, preserves their launcher after Cancel, and stays dismissed after recreation.\n");
            finish(Activity.RESULT_OK,result);return;
        }
        if(phase.equals("resize")){
            getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();
            getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit()
                    .putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();
            store.prefs.edit().putInt("startup_mode",2).putBoolean("reduce_motion",true).commit();
            startDesktop();
            ui(()->home.open("@computer"));
            resizeWindow();
            result.putString("stream","PASS: native corner resizing, anchored opposite corners, maximize/restore and recreation.\n");
            finish(Activity.RESULT_OK,result);return;
        }
        if(phase.equals("setup")){
        shell("settings put global animator_duration_scale 1");shell("appops set --uid "+getTargetContext().getPackageName()+" GET_USAGE_STATS deny");store.usagePrefs.edit().clear().commit();getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().clear().commit();store.prefs.edit().putInt("startup_mode",0).putBoolean("startup_sound",false).putBoolean("welcome_on_start",false).commit();getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().clear().commit();
        progress("checking optional setup appears after the introduction and survives recreation");home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));current=home;root=home.getWindow().getDecorView();
        await(this::systemHomePrompt,"Android offers the Home role at first launch");
        check(find("Nth startup sequence")==null,"Home prompt follows the introduction");
        check(getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).getBoolean(StartupExperience.HOME_OFFERED,false),
                "The first offer is recorded before Android takes focus");
        shot("home-first-run.png");
        systemClick("Cancel");
        await(()->node("Set up Nth Explorer")!=null,"File setup follows the Home prompt");
        check(!home.isHome(),"Cancel keeps the current Android launcher");
        check(find("Nth startup sequence")==null&&getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).getInt("runs",0)==1,"Setup appears only after the first startup presentation finishes");
        check(!usageGuideVisible(),"Explorer setup precedes the Usage Access offer");
        shot("storage-setup.png");
        recreate(MainActivity.class);
        await(()->node("Set up Nth Explorer")!=null,"Explorer setup survives activity recreation");
        systemClick("Later");
        await(this::usageGuideVisible,"Usage Access is offered after Explorer setup");
        shot("vista-usage-first-run.png");
        systemClick("Later");
        shell("settings put global animator_duration_scale 0");
        check(!StorageAccess.has(getTargetContext())&&!ExternalTasks.permitted(getTargetContext()),"Later leaves both permissions off");
        recreate(MainActivity.class);
        await(home::hasWindowFocus,"Desktop regains focus after recreation");
        check(node("Set up Nth Explorer")==null&&!systemHomePrompt(),"Dismissed Explorer and Home prompts are not repeated");
        usageSetup(store);
        ui(()->home.open("@computer"));
        await(()->explorer()!=null,"Computer opens");
        click("Internal storage");
        await(()->find("Enable device file browsing")!=null,"Denied access stays in an in-launcher setup page");
        check(!StorageAccess.has(getTargetContext()),"Browsing never silently grants all-files access");
        resizeWindow();

            app.unregisterActivityLifecycleCallbacks(lifecycle);result.putString("stream","PASS: Home, Explorer and Usage Access setup ordering, one-time dismissal, Welcome Center and Settings recovery, and native window resizing with persisted bounds.\n");finish(Activity.RESULT_OK,result);return;
        }
        getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();store.prefs.edit().putInt("startup_mode",2).putBoolean("welcome_on_start",false).commit();getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));current=home;root=home.getWindow().getDecorView();ui(()->home.open("@computer"));await(()->explorer()!=null,"Computer opens");
        if(phase.equals("revoked")){check(!StorageAccess.has(getTargetContext()),"Android revoked all-files access");ui(()->explorer().openDevice("internal"));await(()->find("Enable device file browsing")!=null,"Revoked storage provides a recovery page");check(!exists(StorageAccess.document("")),"Revoked direct provider reads are denied");shot("storage-revoked.png");check(node("Set up Nth Explorer")==null,"Changing access does not repeat first-run setup");app.unregisterActivityLifecycleCallbacks(lifecycle);result.putString("stream","PASS: actual all-files revocation fails closed, native reads are denied, and the launcher remains usable with recovery and individual folder access.\n");finish(Activity.RESULT_OK,result);return;}
        progress("checking Android's actual all-files grant in the disposable emulator");Intent permission=StorageAccess.settingsIntent(getTargetContext());check(permission.getAction().equals(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)&&permission.getDataString().equals("package:"+getTargetContext().getPackageName()),"The special-access request targets only Nth");await(()->StorageAccess.has(getTargetContext()),"Android reports all-files access");ui(()->explorer().openDevice("internal"));await(()->explorer().page().startsWith("folder:"),"Internal storage opens without another picker");check(DocumentsContract.getDocumentId(DocumentOps.current(explorer().page())).equals("primary:"),"Internal storage opens the shared-storage root");shot("storage-root.png");
        fixtureName="Nth-Device-Test-"+SystemClock.uptimeMillis();fixture=DocumentOps.createFolder(getTargetContext(),StorageAccess.document(""),fixtureName,new DocumentOps.Cancel());Uri source=create(fixture,"Source.txt","native device files — café 🌲"),unselected=create(fixture,"Unselected.txt","not chosen");Uri target=DocumentOps.createFolder(getTargetContext(),fixture,"Target",new DocumentOps.Cancel());DocumentOps.Ref ref=reference(source,fixture);DocumentOps.paste(getTargetContext(),Collections.singletonList(ref),target,false,new DocumentOps.Cancel(),s->{});Uri copied=StorageAccess.document(fixtureName+"/Target/Source.txt");check(Arrays.equals(bytes(source),bytes(copied)),"The existing verified copy engine works with real device files");Uri renamed=DocumentOps.rename(getTargetContext(),reference(copied,target),"Renamed.txt",new DocumentOps.Cancel());check(DocumentsContract.getDocumentId(renamed).endsWith("Renamed.txt"),"Native rename returns its updated document ID");DocumentOps.paste(getTargetContext(),Collections.singletonList(reference(renamed,target)),fixture,true,new DocumentOps.Cancel(),s->{});check(!exists(renamed)&&exists(StorageAccess.document(fixtureName+"/Renamed.txt")),"Verified cut removes only the copied native source");DocumentOps.delete(getTargetContext(),Collections.singletonList(reference(StorageAccess.document(fixtureName+"/Renamed.txt"),fixture)),new DocumentOps.Cancel());check(!exists(StorageAccess.document(fixtureName+"/Renamed.txt")),"Native delete reaches the actual file");
        progress("opening every standard folder in the same Explorer window");for(String[] favorite:StorageAccess.FAVORITES){Uri dir=StorageAccess.document(favorite[2]);if(!exists(dir))DocumentOps.createFolder(getTargetContext(),StorageAccess.document(""),favorite[2],new DocumentOps.Cancel());Uri example=create(dir,"Nth-Device-Example-"+fixtureName+".txt",favorite[1]);try{ui(()->explorer().navigate("computer"));click(favorite[1]);await(()->find("Nth-Device-Example-"+fixtureName+".txt")!=null,"Standard folder opens in Nth: "+favorite[1]);check(StorageAccess.ours(DocumentOps.current(explorer().page())),"Standard folders stay in the built-in provider");if(favorite[0].equals("documents"))shot("storage-documents.png");}finally{DocumentOps.delete(getTargetContext(),Collections.singletonList(reference(example,dir)),new DocumentOps.Cancel());}}
        ui(()->explorer().openLinked(StorageAccess.tree().toString(),"Internal storage"));await(()->explorer().page().equals(StorageAccess.page("internal")),"Root navigation stays in Explorer");ui(()->explorer().navigate(child(StorageAccess.page("internal"),statUnchecked(fixture))));await(()->find("Source.txt")!=null,"Native fixture opens");View file=find("Source.txt");hold(file);await(()->node("File context menu")!=null,"Device files use Vista menus");shot("storage-device-menu.png");back();
        for(String path:new String[]{"../data","/data","Android/data","Android/obb","Android//media","Android/media/"})check(!exists(StorageAccess.document(path)),"Device provider rejects protected/out-of-root paths");DocumentFolders.Item rootInfo=stat(StorageAccess.document(""));check((rootInfo.flags&(DocumentsContract.Document.FLAG_SUPPORTS_DELETE|DocumentsContract.Document.FLAG_SUPPORTS_RENAME))==0,"Storage root cannot be deleted or renamed");
        progress("checking a separate app gets only one chosen device file");resumed.clear();shell("am start -n "+getContext().getPackageName()+"/com.nthexperience.desktop.PickerCallerActivity --es token device --ez explicit true --ez multiple false --es authority "+StorageAccess.AUTHORITY+" --es unselected_id primary:"+fixtureName+"/Unselected.txt");current=awaitActivity(ExplorerPickerActivity.class,null);root=current.getWindow().getDecorView();ui(()->explorer().navigate(child(StorageAccess.page("internal"),statUnchecked(fixture))));await(()->find("Source.txt")!=null,"External chooser opens real storage");shot("storage-picker.png");click("Source.txt");JSONObject report=report("device");for(String flag:new String[]{"read","writeDenied","unselectedDenied","mutationDenied","persistableDenied","childrenDenied"})check(report.getBoolean(flag),"Chosen-file boundary: "+flag);current=awaitActivity(MainActivity.class,null);home=(MainActivity)current;root=home.getWindow().getDecorView();
        DocumentOps.delete(getTargetContext(),Collections.singletonList(reference(fixture,StorageAccess.document(""))),new DocumentOps.Cancel());fixture=null;check(!DesktopBackup.snapshot(store).getJSONObject("settings").has("storage_setup_open"),"Setup and special permissions are not portable backup data");
        app.unregisterActivityLifecycleCallbacks(lifecycle);result.putString("stream","PASS: optional setup after startup, Later/recreation/no repeat, real Android all-files grant/revocation, in-launcher storage root and all five standard folders, real native copy/cut/rename/delete, Vista file menus, protected-path/root guards, and selected-file-only cross-UID reads with writes/rename/persistable grants/root listing/unselected reads denied.\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable failure){try{shot("storage-failure.png");StringBuilder out=new StringBuilder();dump(getUiAutomation().getRootInActiveWindow(),out,0);try(FileOutputStream file=getTargetContext().openFileOutput("storage-tree.txt",0)){file.write(out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}}catch(Exception ignored){}app.unregisterActivityLifecycleCallbacks(lifecycle);StringWriter trace=new StringWriter();failure.printStackTrace(new PrintWriter(trace));result.putString("stream","FAIL: "+trace);finish(Activity.RESULT_CANCELED,result);}}
    private String child(String page,DocumentFolders.Item item){try{return DocumentFolders.childPage(page,item);}catch(JSONException ex){throw new AssertionError(ex);}}
    private VistaExplorer explorer(){return (VistaExplorer)find(root,v->v instanceof VistaExplorer);}
    private void usageSetup(DesktopStore store)throws Exception{
        progress("checking the Usage Access guide and the real Android Settings return");
        ui(()->home.open("@welcome"));
        await(home::hasWindowFocus,"Welcome Center opens without a dialog");
        check(find(root,v->v instanceof WelcomeCenter)!=null&&!usageGuideVisible(),
                "Welcome Center preserves the dismissed offer");
        click("Set up Usage Access");
        await(this::usageGuideVisible,"Welcome Center can reopen the guide");
        systemClick("Cancel");

        ui(()->home.open("setting:page:taskbar"));
        String label="Include apps opened outside Nth Desktop";
        CheckBox box=(CheckBox)find(label);
        check(box!=null&&!box.isChecked(),"Unchecked without a grant");
        ui(box::performClick);
        await(this::usageGuideVisible,"Checkbox opens the guide before Settings");
        check(!box.isChecked(),"The guide leaves the checkbox off");
        if(Build.VERSION.SDK_INT>=33){
            check(node("Open App info")!=null,"Restricted-settings recovery is available from the guide");
            check(node("Android App info menu showing Allow restricted settings")!=null,
                    "The menu illustration has an accessible description");
        }else{
            check(node("Open App info")==null,"Older Android omits restricted-settings recovery");
        }
        boolean settingsAvailable=!shell("cmd package resolve-activity --brief -a android.settings.USAGE_ACCESS_SETTINGS").contains("No activity found");
        systemClick("OK");
        if(!settingsAvailable){
            await(()->home.hasWindowFocus()&&!usageGuideVisible(),"Unavailable Settings returns to the desktop");
            check(!box.isChecked()&&!ExternalTasks.permitted(home),"Unavailable Settings keeps inclusion off");
            progress("Usage Access settings are unavailable on this image; the guide fails closed");
            return;
        }
        await(this::androidSettingsVisible,"OK opens Android Usage Access settings");
        back();
        await(this::usageGuideVisible,"Returning without a grant reopens the guide");
        check(!box.isChecked(),"Returning without a grant keeps the checkbox off");
        shot("vista-usage-return.png");
        if(Build.VERSION.SDK_INT>=33){
            systemClick("Open App info");
            await(()->androidSettingsVisible()&&node("App info")!=null,"App info opens for the launcher");
            back();
            await(()->androidSettingsVisible()&&node("App info")==null,"Back from App info resumes Usage Access settings");
        }else{
            check(node("Open App info")==null,"Older Android omits restricted-settings recovery");
            systemClick("OK");
            await(this::androidSettingsVisible,"The guide can retry Android Settings");
        }
        shell("appops set --uid "+getTargetContext().getPackageName()+" GET_USAGE_STATS allow");
        back();
        await(()->home.hasWindowFocus()&&box.isChecked(),"A real grant checks the box when Vista resumes");
        check(!store.usagePrefs.contains(DesktopStore.TASKBAR_USAGE),"A grant enables inclusion without a saved override");
        ui(box::performClick);
        check(!box.isChecked()&&!store.usagePrefs.getBoolean(DesktopStore.TASKBAR_USAGE,true),"Opt-out persists with the Android grant");
        ui(()->home.open("@welcome"));
        click("Set up Usage Access");
        await(()->home.hasWindowFocus()&&find(label)!=null,"With a grant, Welcome opens the Taskbar page");
        check(!((CheckBox)find(label)).isChecked(),"Reviewing the setting preserves opt-out");
        shell("appops set --uid "+getTargetContext().getPackageName()+" GET_USAGE_STATS deny");
    }
    private boolean usageGuideVisible(){return node("Set up Usage Access")!=null&&node("Close dialog")!=null;}
    private boolean androidSettingsVisible(){
        AccessibilityNodeInfo active=getUiAutomation().getRootInActiveWindow();
        return active!=null&&"com.android.settings".contentEquals(active.getPackageName());
    }

    private void resizeWindow()throws Exception{
        progress("dragging all four corners and restoring the window's saved size");
        DesktopWindow computer=(DesktopWindow)find(root,v->v instanceof DesktopWindow&&"@computer".equals(((DesktopWindow)v).toolId));
        check(computer!=null,"Explorer window is visible");
        await(()->home.hasWindowFocus()&&computer.getWidth()>0&&computer.getHeight()>0,"Explorer window is laid out before the corner drag");
        int step=Aero.dp(home,36);
        Rect original=screenBounds(computer);
        dragCorner(original,false,false,-step,-step);
        await(()->computer.getWidth()<original.width()&&computer.getHeight()<original.height(),"Bottom-right drag resizes Explorer");
        Rect smaller=screenBounds(computer);
        check(Math.abs(smaller.left-original.left)<=1&&Math.abs(smaller.top-original.top)<=1,"The opposite corner stays fixed");
        dragCorner(smaller,true,true,step,step);
        await(()->computer.getWidth()<smaller.width()&&computer.getHeight()<smaller.height(),"Top-left drag also resizes Explorer");
        Rect topLeft=screenBounds(computer);
        check(Math.abs(topLeft.right-smaller.right)<=1&&Math.abs(topLeft.bottom-smaller.bottom)<=1,"Resizing from the top left anchors the bottom right");
        int grow=Aero.dp(home,12);
        dragCorner(topLeft,true,false,-grow,grow);
        await(()->computer.getWidth()>topLeft.width()&&computer.getHeight()>topLeft.height(),"Bottom-left drag resizes Explorer");
        Rect bottomLeft=screenBounds(computer);
        check(Math.abs(bottomLeft.right-topLeft.right)<=1&&Math.abs(bottomLeft.top-topLeft.top)<=1,"Bottom-left resizing anchors the top right");
        dragCorner(bottomLeft,false,true,grow,-grow);
        await(()->computer.getWidth()>bottomLeft.width()&&computer.getHeight()>bottomLeft.height(),"Top-right drag resizes Explorer");
        Rect saved=screenBounds(computer);
        check(Math.abs(saved.left-bottomLeft.left)<=1&&Math.abs(saved.bottom-bottomLeft.bottom)<=1,"Top-right resizing anchors the bottom left");
        ui(computer::maximize);
        await(computer::maximized,"Explorer maximizes");
        ui(computer::restoreDown);
        await(()->!computer.maximized()&&screenBounds(computer).equals(saved),"Restoring retains the resized bounds");
        recreate(MainActivity.class);
        await(()->{
            View restored=find(root,v->v instanceof DesktopWindow&&"@computer".equals(((DesktopWindow)v).toolId));
            return restored!=null&&screenBounds(restored).equals(saved);
        },"Resized bounds survive activity recreation");
    }
    private Rect screenBounds(View view){
        int[] xy=new int[2];
        view.getLocationOnScreen(xy);
        return new Rect(xy[0],xy[1],xy[0]+view.getWidth(),xy[1]+view.getHeight());
    }
    private void dragCorner(Rect bounds,boolean left,boolean top,int dx,int dy)throws Exception{
        int inset=Aero.dp(home,4);
        float x=left?bounds.left+inset:bounds.right-inset;
        float y=top?bounds.top+inset:bounds.bottom-inset;
        long down=SystemClock.uptimeMillis();
        inject(down,MotionEvent.ACTION_DOWN,x,y);
        for(int i=1;i<=4;i++){
            SystemClock.sleep(45);
            inject(down,MotionEvent.ACTION_MOVE,x+dx*i/4f,y+dy*i/4f);
        }
        inject(down,MotionEvent.ACTION_UP,x+dx,y+dy);
        // Injecting UP waits for input dispatch, not the following layout frame.
        // Read the final anchored bounds after layout and accessibility settle.
        waitForIdleSync();
        getUiAutomation().waitForIdle(100,3000);
        waitForIdleSync();
    }

    private Uri create(Uri parent,String name,String text)throws Exception{Uri uri=DocumentsContract.createDocument(getTargetContext().getContentResolver(),parent,"text/plain",name);check(uri!=null,"A native fixture is created");try(OutputStream out=getTargetContext().getContentResolver().openOutputStream(uri)){out.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}return uri;}
    private DocumentFolders.Item stat(Uri uri)throws Exception{return DocumentOps.stat(getTargetContext(),uri,new DocumentOps.Cancel());}
    private DocumentFolders.Item statUnchecked(Uri uri){try{return stat(uri);}catch(Exception ex){throw new AssertionError(ex);}}
    private DocumentOps.Ref reference(Uri uri,Uri parent)throws Exception{return new DocumentOps.Ref(uri,parent,stat(uri));}
    private boolean exists(Uri uri){try{stat(uri);return true;}catch(Exception ex){return false;}}
    private byte[] bytes(Uri uri)throws Exception{try(InputStream in=getTargetContext().getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int read;while((read=in.read(buffer))!=-1)out.write(buffer,0,read);return out.toByteArray();}}
    private void recreate(Class<?> type)throws Exception{Activity previous=current;resumed.clear();ui(current::recreate);current=awaitActivity(type,previous);root=current.getWindow().getDecorView();if(current instanceof MainActivity)home=(MainActivity)current;}
    private Activity awaitActivity(Class<?> type,Activity old)throws Exception{long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){Activity activity=resumed.poll(100,TimeUnit.MILLISECONDS);if(activity!=null&&type.isInstance(activity)&&activity!=old&&!activity.isDestroyed()){Thread.sleep(120);return activity;}}throw new AssertionError("Activity unavailable: "+type.getSimpleName());}
    private JSONObject report(String token)throws Exception{long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){try{JSONObject result=new JSONObject(shell("run-as "+getContext().getPackageName()+" cat files/picker-result.json"));if(token.equals(result.optString("token")))return result;}catch(JSONException ignored){}Thread.sleep(80);}throw new AssertionError("Caller result missing");}
    private void click(String description){View view=find(description);check(view!=null,"Control available: "+description);ui(view::performClick);}
    private View find(String description){return find(root,v->description.equals(v.getContentDescription()));}
    private View find(View view,Predicate<View> match){if(view==null||!view.isShown())return null;if(match.test(view))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=find(((ViewGroup)view).getChildAt(i),match);if(found!=null)return found;}return null;}
    private void hold(View view)throws Exception{int[] xy=new int[2];view.getLocationOnScreen(xy);long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,xy[0]+view.getWidth()/2f,xy[1]+view.getHeight()/2f);Thread.sleep(700);inject(down,MotionEvent.ACTION_UP,xy[0]+view.getWidth()/2f,xy[1]+view.getHeight()/2f);}
    private void inject(long down,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(e,true);e.recycle();}
    private void back(){getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);}
    private void startDesktop(){
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(getTargetContext(),MainActivity.class))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        current=home;root=home.getWindow().getDecorView();
    }
    private void dismissHomeOffer(SharedPreferences session,String screenshot)throws Exception{
        await(this::systemHomePrompt,"Android offers to make Seven the Home app");
        check(find("Nth startup sequence")==null,"The Home prompt follows the intro when it is enabled");
        check(session.getBoolean(StartupExperience.HOME_OFFERED,false),"The offer is recorded before Android takes focus");
        shot(screenshot);
        systemClick("Cancel");
    }
    private boolean systemHomePrompt(){
        AccessibilityNodeInfo active=getUiAutomation().getRootInActiveWindow();
        CharSequence owner=active==null?null:active.getPackageName();
        return owner!=null&&owner.toString().endsWith(".permissioncontroller");
    }
    private AccessibilityNodeInfo node(String text){return node(getUiAutomation().getRootInActiveWindow(),text);}
    private AccessibilityNodeInfo node(AccessibilityNodeInfo node,String text){if(node==null)return null;if(text.equalsIgnoreCase(node.getText()==null?"":node.getText().toString())||text.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=node(node.getChild(i),text);if(found!=null)return found;}return null;}
    private void systemClick(String text)throws Exception{await(()->node(text)!=null,"Dialog button: "+text);AccessibilityNodeInfo node=node(text);while(node!=null&&!node.isClickable())node=node.getParent();check(node!=null&&node.isVisibleToUser(),"Dialog action is visible");android.graphics.Rect bounds=new android.graphics.Rect();node.getBoundsInScreen(bounds);check(!bounds.isEmpty()&&bounds.width()>10&&bounds.height()>10,"Dialog action has usable screen bounds");long down=SystemClock.uptimeMillis();inject(down,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY());Thread.sleep(70);inject(down,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY());Thread.sleep(120);}
    private void ui(Runnable action){runOnMainSync(action);try{Thread.sleep(100);}catch(InterruptedException ex){throw new AssertionError(ex);}}
    private void await(BooleanSupplier condition,String message)throws Exception{long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){if(condition.getAsBoolean())return;Thread.sleep(80);}throw new AssertionError(message);}
    private String shell(String command)throws Exception{try(ParcelFileDescriptor descriptor=getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(descriptor);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int read;while((read=in.read(buffer))!=-1)out.write(buffer,0,read);return out.toString("UTF-8");}}
    private void shot(String name)throws Exception{Thread.sleep(180);Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"Screenshot available");try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}}
    private void dump(AccessibilityNodeInfo node,StringBuilder out,int depth){if(node==null||depth>30)return;out.append(depth).append(" ").append(node.getClassName()).append(" text=").append(node.getText()).append(" description=").append(node.getContentDescription()).append("\n");for(int i=0;i<node.getChildCount();i++)dump(node.getChild(i),out,depth+1);}
    private void progress(String text){Bundle out=new Bundle();out.putString("stream","CHECK: "+text+"\n");sendStatus(1,out);}
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
