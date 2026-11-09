package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.json.*;

/** Native input and rendered-state checks for the final Seven interaction pass. */
public final class SevenPolishSmoke extends Instrumentation {
    private MainActivity home;private View root;private DesktopStore store;
    private long down;private boolean pointerDown;private float pointerX,pointerY;
    private final BlockingQueue<MainActivity> resumed=new LinkedBlockingQueue<>();
    private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){
        public void onActivityResumed(Activity activity){if(activity instanceof MainActivity)resumed.offer((MainActivity)activity);}
        public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivityPaused(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
    };
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();boolean success=false;Application app=(Application)getTargetContext().getApplicationContext();app.registerActivityLifecycleCallbacks(lifecycle);
        try{
            store=new DesktopStore(getTargetContext());
            store.prefs.edit().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true)
                    .putBoolean("taskbar_show_labels",false).putBoolean("taskbar_apps",false).putBoolean("tray_expanded",true)
                    .putBoolean("taskbar_static",false).putBoolean("sidebar_visible",false).putBoolean("desktop_sounds",false).commit();
            store.quickLaunch(Arrays.asList("@notes","@files","@calendar"));
            getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();
            getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();
        getTargetContext().getSharedPreferences(InitializationState.PREFS,0).edit().putInt("flow_version",2).putInt("step",InitializationState.Step.DONE.ordinal()).commit();
            home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();
            ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            await(()->root.getWidth()>root.getHeight()&&home.hasWindowFocus(),"Landscape desktop is focused");settle();
            List<String> failures=new ArrayList<>();
            phase("taskbar",this::reorder,failures);phase("Peek",this::peek,failures);phase("sounds",this::sounds,failures);phase("Action Center",this::actionCenter,failures);phase("portrait",this::portrait,failures);
            if(!failures.isEmpty())throw new AssertionError(String.join("\n",failures));
            result.putString("stream","PASS: real horizontal taskbar drag persists through recreation; keyboard/accessibility reordering; icon-color hover glow; upward flick and still-hold previews; touch and mouse Peek render hollow outlines and restore on release/cancel without changing notes, geometry or minimized state; original sound scheme previews, theme compatibility and backup validation; real-alert Action Center flyout and full-center routing.\n");success=true;
        }catch(Throwable failure){try{if(pointerDown)pointer(MotionEvent.ACTION_CANCEL,pointerX,pointerY);shot("seven-polish-failure");}catch(Throwable ignored){}StringWriter text=new StringWriter();failure.printStackTrace(new PrintWriter(text));result.putString("stream","FAIL: "+text);}
        finally{DesktopSounds.stop();app.unregisterActivityLifecycleCallbacks(lifecycle);}
        finish(success?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private interface Phase {void run()throws Exception;}
    private void phase(String name,Phase test,List<String> failures){
        try{test.run();}catch(Throwable failure){
            StringWriter detail=new StringWriter();failure.printStackTrace(new PrintWriter(detail));failures.add(name+": "+detail);
            progress("phase failed: "+name+" - "+failure);
            try{if(pointerDown)pointer(MotionEvent.ACTION_CANCEL,pointerX,pointerY);ui(home::onBackPressed);}catch(Throwable ignored){}
        }
    }
    private void reorder()throws Exception{
        progress("taskbar drag and persistence");
        await(()->keys().equals(Arrays.asList("@notes","@files","@calendar")),"Three real pinned slots");
        View first=slot("@notes"),last=slot("@calendar");Rect from=bounds(first),to=bounds(last);
        pointer(MotionEvent.ACTION_DOWN,from.centerX(),from.centerY());SystemClock.sleep(210);
        ui(()->{try{java.lang.reflect.Method refresh=MainActivity.class.getDeclaredMethod("renderTaskbar");refresh.setAccessible(true);refresh.invoke(home);check(slot("@notes")==first,"Catalog refresh preserves the pressed taskbar button before drag starts");}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}});
        pointer(MotionEvent.ACTION_MOVE,from.centerX()+dp(18),from.centerY());SystemClock.sleep(120);
        for(int step=1;step<=6;step++){pointer(MotionEvent.ACTION_MOVE,from.centerX()+(to.right-from.centerX()-2)*step/6f,from.centerY());SystemClock.sleep(40);}
        pointer(MotionEvent.ACTION_UP,to.right-2,from.centerY());
        await(()->keys().equals(Arrays.asList("@files","@calendar","@notes")),"Horizontal drag commits its drop order");
        check(store.quickLaunch().equals(Arrays.asList("@files","@calendar","@notes")),"Dragging updates the Quick Launch editor order");
        shot("seven-taskbar-order");
        MainActivity previous=home;resumed.clear();ui(home::recreate);
        long deadline=SystemClock.uptimeMillis()+8000;
        while(SystemClock.uptimeMillis()<deadline){MainActivity next=resumed.poll(500,TimeUnit.MILLISECONDS);if(next!=null&&next!=previous){home=next;break;}}
        check(home!=previous,"Activity recreates");root=home.getWindow().getDecorView();await(()->home.hasWindowFocus()&&keys().equals(Arrays.asList("@files","@calendar","@notes")),"Order survives recreation");settle();
        View note=slot("@notes");ui(()->check(note.performAccessibilityAction(R.id.taskbar_move_left,null),"Accessibility Move left works"));
        await(()->keys().equals(Arrays.asList("@files","@notes","@calendar")),"Accessible order applies");
        sendKeyDownUpSync(KeyEvent.KEYCODE_TAB); // Enter keyboard mode before requesting non-touch focus.
        ui(()->check(slot("@notes").requestFocus(),"Taskbar accepts keyboard focus"));
        long keyTime=SystemClock.uptimeMillis();
        for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP}){
            KeyEvent event=new KeyEvent(keyTime,SystemClock.uptimeMillis(),action,KeyEvent.KEYCODE_DPAD_RIGHT,0,KeyEvent.META_CTRL_ON|KeyEvent.META_SHIFT_ON,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,InputDevice.SOURCE_KEYBOARD);
            check(getUiAutomation().injectInputEvent(event,true),"Keyboard chord is injected");
        }
        await(()->keys().equals(Arrays.asList("@files","@calendar","@notes")),"Keyboard reorder applies");
        View files=slot("@files");final Bitmap[] images=new Bitmap[3];
        ui(()->{images[0]=draw(files);hover(files,MotionEvent.ACTION_HOVER_ENTER);images[1]=draw(files);hover(files,MotionEvent.ACTION_HOVER_EXIT);images[2]=draw(files);});
        check(!images[0].sameAs(images[1])&&images[0].sameAs(images[2]),"Icon-color glow appears on hover and clears on exit");for(Bitmap image:images)image.recycle();
        // A fast release-only flick must not tap the pinned Explorer button.
        Rect at=bounds(files);pointer(MotionEvent.ACTION_DOWN,at.centerX(),at.centerY());SystemClock.sleep(45);pointer(MotionEvent.ACTION_UP,at.centerX()+dp(2),at.centerY()-dp(15));
        await(()->node("Open new Explorer window"),"Upward flick opens the Jump List");ui(home::onBackPressed);
        ui(()->home.open("@notes"));await(()->window("@notes")!=null,"Notepad opens");settle();
        View task=slot("@notes");Rect hold=bounds(task);pointer(MotionEvent.ACTION_DOWN,hold.centerX(),hold.centerY());SystemClock.sleep(650);pointer(MotionEvent.ACTION_UP,hold.centerX(),hold.centerY());
        await(()->node("Window preview for Notepad"),"Holding still opens a preview");ui(home::onBackPressed);
    }
    private void peek()throws Exception{
        progress("touch Peek and rendered hollow windows");
        ui(()->{home.open("@calendar");home.open("@notes");});settle();
        DesktopWindow notes=window("@notes"),calendar=window("@calendar");
        EditText editor=(EditText)find(notes,v->"Notepad text".equals(v.getContentDescription()));ui(()->editor.setText("Seven Peek continuity"));
        JSONObject geometry=notes.geometry();Rect at=bounds(showDesktop());
        pointer(MotionEvent.ACTION_DOWN,at.centerX(),at.centerY());SystemClock.sleep(430);
        await(()->notes.isPeeking()&&calendar.isPeeking(),"Touch hold starts Peek");
        final Bitmap[] capture=new Bitmap[2];
        ui(()->{capture[0]=draw(notes);capture[1]=Bitmap.createBitmap(notes.getWidth(),notes.getHeight(),Bitmap.Config.ARGB_8888);notes.drawPreview(new Canvas(capture[1]));});
        check(Color.alpha(capture[0].getPixel(capture[0].getWidth()/2,capture[0].getHeight()/2))==0,"Peek body is transparent");
        int visible=0;for(int y=0;y<capture[0].getHeight();y++)for(int x=0;x<capture[0].getWidth();x++)if(Color.alpha(capture[0].getPixel(x,y))>0)visible++;
        check(visible>0&&visible<capture[0].getWidth()*capture[0].getHeight()/4,"Only hollow frame outlines remain");
        check(Color.alpha(capture[1].getPixel(capture[1].getWidth()/2,capture[1].getHeight()/2))>0&&notes.isPeeking(),"Preview capture retains real contents while Peek stays active");for(Bitmap image:capture)image.recycle();
        shot("seven-touch-peek");pointer(MotionEvent.ACTION_UP,at.centerX(),at.centerY());
        await(()->!notes.isPeeking()&&!calendar.isPeeking(),"Release restores the window contents");
        check(notes.isShown()&&calendar.isShown()&&geometry.toString().equals(notes.geometry().toString())&&"Seven Peek continuity".contentEquals(editor.getText()),"Peek preserves note, geometry and window visibility");
        pointer(MotionEvent.ACTION_DOWN,at.centerX(),at.centerY());SystemClock.sleep(430);await(notes::isPeeking,"Second Peek starts");pointer(MotionEvent.ACTION_CANCEL,at.centerX(),at.centerY());await(()->!notes.isPeeking(),"Canceled hold restores content");
        ui(()->hover(showDesktop(),MotionEvent.ACTION_HOVER_ENTER));SystemClock.sleep(360);await(notes::isPeeking,"Mouse hover Peeks");ui(()->hover(showDesktop(),MotionEvent.ACTION_HOVER_EXIT));await(()->!notes.isPeeking(),"Hover exit restores content");
        tap(showDesktop(),50);await(()->notes.getVisibility()==View.GONE&&calendar.getVisibility()==View.GONE,"Short tap shows desktop");
        tap(showDesktop(),50);await(()->notes.isShown()&&calendar.isShown(),"Second tap restores desktop windows");
        ui(()->{notes.closeWindow();calendar.closeWindow();});
    }
    private void sounds()throws Exception{
        progress("sound settings, portable themes and backups");
        ui(()->home.open("setting:page:sounds"));settle();
        View panel=window("@control");check(panel!=null,"Control Panel sound page opens");
        Spinner schemes=(Spinner)find(panel,v->"Desktop sound scheme".equals(v.getContentDescription()));
        ui(()->schemes.setSelection(SoundSchemes.index("twilight")));await(()->DesktopSounds.scheme(store).equals("twilight"),"Sound settings persist the selected scheme");
        View test=find(panel,v->"Test notification sound".equals(v.getContentDescription()));ui(test::performClick);
        check(!store.prefs.getBoolean("desktop_sounds",true),"Testing a cue does not opt into automatic sounds");
        JSONObject profile=ThemeProfiles.capture(store,"Seven sound test");byte[] data=ThemeProfiles.pack(home,profile);
        JSONObject imported=ThemeProfiles.unpack(home,new ByteArrayInputStream(data));check("twilight".equals(imported.getString(DesktopSounds.SCHEME)),"Portable theme includes the scheme");
        store.prefs.edit().putString(DesktopSounds.SCHEME,"meadow").commit();ThemeProfiles.apply(store,imported);check(DesktopSounds.scheme(store).equals("twilight"),"Applying the theme restores the scheme");
        JSONObject legacy=new JSONObject(profile.toString());legacy.remove(DesktopSounds.SCHEME);ThemeProfiles.apply(store,legacy);check(DesktopSounds.scheme(store).equals("classic"),"Older themes keep classic cues");ThemeProfiles.apply(store,profile);
        JSONObject backup=DesktopBackup.snapshot(store);DesktopBackup.Plan plan=DesktopBackup.parse(backup.toString());
        check(plan.string(DesktopSounds.SCHEME,"").equals("twilight")&&plan.count(TaskbarOrder.KEY)==3,"Desktop backup preserves sound and order");
        JSONObject invalid=new JSONObject(backup.toString());invalid.getJSONObject("settings").getJSONObject(DesktopSounds.SCHEME).put("value","unknown");
        boolean rejected=false;try{DesktopBackup.parse(invalid.toString());}catch(JSONException expected){rejected=true;}check(rejected,"Unknown backup scheme is rejected before applying");
        invalid=new JSONObject(backup.toString());invalid.getJSONObject("settings").getJSONObject(TaskbarOrder.KEY).put("value","[\"@notes\",\"@notes\"]");
        rejected=false;try{DesktopBackup.parse(invalid.toString());}catch(JSONException expected){rejected=true;}check(rejected,"Duplicate backup order is rejected");
        settle();shot("seven-sound-schemes");
        DesktopWindow control=window("@control");if(control!=null)ui(control::closeWindow);
    }
    private void actionCenter()throws Exception{
        progress("real-alert Action Center flyout");
        Context context=getTargetContext();context.getSharedPreferences("seven_action_center",0).edit().clear().commit();store.prefs.edit().putString("linked_folders","[]").commit();
        check(ActionCenter.alerts(context,store).isEmpty(),"Optional file/Home choices are not alerts");
        View flag=find(root,v->"Action Center".equals(v.getContentDescription()));check(flag!=null&&flag.isShown(),"Canvas tray flag is accessible");tap(flag,50);
        await(()->find(root,v->"Action Center flyout".equals(v.getContentDescription()))!=null,"Flag opens compact flyout");
        check(find(root,v->v instanceof TextView&&"No recent Nth alerts.".contentEquals(((TextView)v).getText()))!=null,"Quiet flyout has no fake warnings");
        ui(home::onBackPressed);
        ActionCenter.notice(context,"files","A test folder could not be read.");
        await(()->"1 Nth alert".equals(flag.getStateDescription()),"Tray flag updates when a real alert arrives");tap(flag,50);
        await(()->find(root,v->"Review Nth Explorer".equals(v.getContentDescription()))!=null,"A recorded file issue is actionable");shot("seven-action-center");
        View full=find(root,v->"Open Action Center".equals(v.getContentDescription()));ui(full::performClick);
        await(()->window("@actioncenter")!=null,"Flyout routes to the full Action Center");
        check(find(root,v->"Action Center flyout".equals(v.getContentDescription()))==null,"Opening the full window dismisses flyout");
        ui(()->window("@actioncenter").closeWindow());
        context.getSharedPreferences("seven_action_center",0).edit().clear().commit();
        // Only genuinely lost existing links add attention to the flag.
        store.prefs.edit().putString("linked_folders","[{\"uri\":\"content://com.example.missing/tree/a\",\"name\":\"Missing link\"}]").commit();
        check(ActionCenter.alerts(context,store).size()==1,"Lost linked-folder grants are real alerts");store.prefs.edit().putString("linked_folders","[]").commit();
    }
    private void portrait()throws Exception{
        progress("portrait tray and Peek bounds");
        ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        await(()->root.getHeight()>root.getWidth(),"Portrait layout settles");settle();
        View peek=showDesktop(),strip=find(root,v->"taskbar-items".equals(v.getTag()));
        await(()->{int[] rootAt=new int[2],peekAt=new int[2];root.getLocationOnScreen(rootAt);peek.getLocationOnScreen(peekAt);return peekAt[0]>=rootAt[0]&&peekAt[0]+peek.getWidth()<=rootAt[0]+root.getWidth();},"Full Peek touch target stays inside portrait bounds");
        shot("seven-portrait-tray");
        check(peek.getWidth()>=dp(32)&&strip.getWidth()>0,"Expanded tray leaves a usable Peek target and a scrolling task area");
        View tray=find(root,v->v instanceof SystemTray);for(int i=0;i<((ViewGroup)tray).getChildCount();i++){View icon=((ViewGroup)tray).getChildAt(i);if(icon.isShown())check(bounds(icon).width()>=dp(24),"Tray controls keep their touch width");}
        ui(()->{
            SystemTray fitted=(SystemTray)tray;
            fitted.fitWidth(dp(350));int expected=fitted.getChildAt(1).getLayoutParams().width;
            fitted.measure(View.MeasureSpec.makeMeasureSpec(dp(350),View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(dp(48),View.MeasureSpec.EXACTLY));
            check(fitted.getChildAt(1).getLayoutParams().width==expected,"Measurement preserves an explicitly narrower workspace budget");
            fitted.fitWidth(((View)fitted.getParent()).getWidth());
        });
    }
    private View showDesktop(){return find(root,v->v instanceof FrameLayout&&( "Show desktop".equals(v.getContentDescription())||"Restore desktop windows".equals(v.getContentDescription())));}
    private View slot(String id){return find(root,v->id.equals(TaskbarReorder.key(v)));}
    private List<String> keys(){View strip=find(root,v->"taskbar-items".equals(v.getTag()));List<String> result=new ArrayList<>();if(strip instanceof ViewGroup&&((ViewGroup)strip).getChildCount()>0){ViewGroup items=(ViewGroup)((ViewGroup)strip).getChildAt(0);for(int i=0;i<items.getChildCount();i++){String id=TaskbarReorder.key(items.getChildAt(i));if(id!=null)result.add(id);}}return result;}
    private DesktopWindow window(String id){View found=find(root,v->v instanceof DesktopWindow&&id.equals(((DesktopWindow)v).toolId));return (DesktopWindow)found;}
    private View find(View start,Predicate<View> wanted){if(start==null)return null;if(wanted.test(start))return start;if(start instanceof ViewGroup)for(int i=0;i<((ViewGroup)start).getChildCount();i++){View found=find(((ViewGroup)start).getChildAt(i),wanted);if(found!=null)return found;}return null;}
    private boolean node(String value){return node(getUiAutomation().getRootInActiveWindow(),value);}
    private boolean node(AccessibilityNodeInfo current,String value){if(current==null)return false;if(value.contentEquals(current.getText()==null?"":current.getText())||value.contentEquals(current.getContentDescription()==null?"":current.getContentDescription()))return true;for(int i=0;i<current.getChildCount();i++)if(node(current.getChild(i),value))return true;return false;}
    private Rect bounds(View view){check(view!=null,"Input target exists");Rect rect=new Rect();check(view.getGlobalVisibleRect(rect),"Input target is on screen");return rect;}
    private int dp(int value){return Aero.dp(home,value);}
    private void tap(View view,long hold)throws Exception{Rect at=bounds(view);pointer(MotionEvent.ACTION_DOWN,at.centerX(),at.centerY());SystemClock.sleep(hold);pointer(MotionEvent.ACTION_UP,at.centerX(),at.centerY());settle();}
    private void pointer(int action,float x,float y){if(action==MotionEvent.ACTION_DOWN){down=SystemClock.uptimeMillis();pointerDown=true;}pointerX=x;pointerY=y;MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(getUiAutomation().injectInputEvent(event,true),"Native input is injected, including system drag handoff");}finally{event.recycle();}if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)pointerDown=false;}
    private void hover(View view,int action){MotionEvent event=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),action,view.getWidth()/2f,view.getHeight()/2f,0);event.setSource(InputDevice.SOURCE_MOUSE);try{view.dispatchGenericMotionEvent(event);}finally{event.recycle();}}
    private Bitmap draw(View view){Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));return bitmap;}
    private void settle(){waitForIdleSync();SystemClock.sleep(220);}
    private void ui(Runnable task){final Throwable[] error={null};runOnMainSync(()->{try{task.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("UI check",error[0]);waitForIdleSync();}
    private void await(BooleanSupplier value,String message)throws Exception{long end=SystemClock.uptimeMillis()+6500;while(SystemClock.uptimeMillis()<end){if(value.getAsBoolean())return;SystemClock.sleep(70);}throw new AssertionError(message);}
    private void shot(String name)throws Exception{final Bitmap[] rendered={null};ui(()->rendered[0]=draw(root));Bitmap bitmap=rendered[0];check(bitmap!=null,"Native app render exists");try(FileOutputStream out=getTargetContext().openFileOutput(name+".png",0)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}}
    private void progress(String message){Bundle update=new Bundle();update.putString("stream","CHECK: "+message+"\n");sendStatus(1,update);}
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
