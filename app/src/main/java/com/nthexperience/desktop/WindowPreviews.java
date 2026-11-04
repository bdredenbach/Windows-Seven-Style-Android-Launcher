package com.nthexperience.desktop;

import android.app.*;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** In-process previews of launcher windows. */
final class WindowPreviews {
    interface Host {java.util.List<DesktopWindow> windows();java.util.List<String> external();String label(String id);void open(String id);View wallpaper();void showDesktop();void preparePreview();}
    private final Activity activity;private final Host host;private final Handler main=new Handler(Looper.getMainLooper());
    private Dialog dialog;private PopupWindow folderPicker;private Runnable pending;
    private final Runnable dismissFolderHover=()->{if(folderPicker!=null&&!folderPicker.isFocusable())folderPicker.dismiss();};
    WindowPreviews(Activity activity,Host host){this.activity=activity;this.host=host;}
    static boolean allowed(DesktopWindow window){return !(window.body instanceof BrowserPanel)||!((BrowserPanel)window.body).hasPrivateTabs();}
    private Bitmap snapshot(DesktopWindow window){
        return allowed(window)?capture(window,280,170):null;
    }
    private Bitmap capture(View source,int maxWidth,int maxHeight){
        if(source.getWidth()<1||source.getHeight()<1)return null;
        float scale=Math.min(1,Math.min((float)maxWidth/source.getWidth(),(float)maxHeight/source.getHeight()));
        Bitmap bitmap=null;
        try{
            bitmap=Bitmap.createBitmap(Math.max(1,Math.round(source.getWidth()*scale)),
                    Math.max(1,Math.round(source.getHeight()*scale)),Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            canvas.scale(scale,scale);
            source.draw(canvas);
            if(source instanceof DesktopWindow&&((DesktopWindow)source).body instanceof MediaPanel){
                TextureView video=((MediaPanel)((DesktopWindow)source).body).videoSurface();
                if(video.isShown()&&video.isAvailable()){
                    int[] outer=new int[2],inner=new int[2];source.getLocationInWindow(outer);video.getLocationInWindow(inner);
                    Bitmap frame=video.getBitmap(Math.max(1,Math.round(video.getWidth()*scale)),Math.max(1,Math.round(video.getHeight()*scale)));
                    if(frame!=null){
                        int left=inner[0]-outer[0],top=inner[1]-outer[1];
                        canvas.drawBitmap(frame,null,new Rect(left,top,left+video.getWidth(),top+video.getHeight()),null);
                        frame.recycle();
                    }
                }
            }
            return bitmap;
        }catch(RuntimeException|OutOfMemoryError ex){
            if(bitmap!=null)bitmap.recycle();
            return null;
        }
    }
    static final class Card {
        final String title, detail;
        final View source;
        final Runnable open, close;
        Card(String title,String detail,View source,Runnable open,Runnable close){
            this.title=title;this.detail=detail;this.source=source;this.open=open;this.close=close;
        }
    }
    void bindTask(View button,List<DesktopWindow> windows,boolean mouse,Runnable commands){
        java.util.function.Supplier<List<Card>> cards=()->{
            List<Card> result=new ArrayList<>();
            for(DesktopWindow window:windows){
                if(window.getParent()==null)continue;
                if(window.body instanceof BrowserPanel&&allowed(window)){
                    BrowserPanel browser=(BrowserPanel)window.body;
                    if(browser.tabCount()>1){result.addAll(browser.previewCards(window));continue;}
                }
                boolean visible=allowed(window);
                result.add(new Card(window.caption(),visible?"Preview unavailable":"InPrivate — preview hidden",
                        visible?window:null,window::restore,window::closeWindow));
            }
            return result;
        };
        bind(button,cards,windows.size()>1?"Folder windows":null,mouse,commands);
        if(windows.size()>1)button.setOnClickListener(v->showCards(button,cards.get(),"Folder windows",true,commands));
    }
    void bindApp(View button,String title,boolean running,boolean mouse,Runnable open,Runnable commands){
        bind(button,()->Collections.singletonList(new Card(title,running?"Android app · switch to app":"Pinned shortcut",
                button instanceof ViewGroup?((ViewGroup)button).getChildAt(0):null,open,null)),null,mouse&&running,commands);
    }
    private void bind(View button,java.util.function.Supplier<List<Card>> cards,String group,boolean mouse,Runnable commands){
        button.setOnLongClickListener(v->{showCards(button,cards.get(),group,true,commands);return true;});
        if(!mouse)return;
        button.setOnHoverListener((v,event)->{
            if(!event.isFromSource(InputDevice.SOURCE_MOUSE)&&!event.isFromSource(InputDevice.SOURCE_STYLUS))return false;
            if(event.getActionMasked()==MotionEvent.ACTION_HOVER_ENTER){
                main.removeCallbacks(dismissFolderHover);
                if(pending!=null)main.removeCallbacks(pending);
                pending=()->{pending=null;showCards(button,cards.get(),group,false,commands);};
                main.postDelayed(pending,450);
            }else if(event.getActionMasked()==MotionEvent.ACTION_HOVER_EXIT){
                if(pending!=null){main.removeCallbacks(pending);pending=null;}
                main.postDelayed(dismissFolderHover,300);
            }
            return false;
        });
    }
    private void showCards(View anchor,List<Card> cards,String group,boolean sticky,Runnable commands){
        if(!anchor.isAttachedToWindow()||cards.isEmpty()||!sticky&&(!activity.hasWindowFocus()
                ||folderPicker!=null&&folderPicker.isFocusable()))return;
        dismiss();
        if(sticky){host.preparePreview();hideKeyboard();}
        Context context=activity;
        int gap=Aero.dp(context,7);
        Rect safe=new Rect();anchor.getWindowVisibleDisplayFrame(safe);
        if(safe.width()<Aero.dp(context,110))return;
        int cardWidth=Math.min(Aero.dp(context,190),(safe.width()-4*gap)/Math.min(2,cards.size()));
        int width=Math.min(safe.width()-2*gap,cards.size()*(cardWidth+gap)+gap);
        int height=Aero.dp(context,sticky?179:149);
        LinearLayout shell=Aero.column(context);
        shell.setContentDescription(group==null?(cards.size()>1?"Browser tab previews":"Window preview for "+cards.get(0).title):group);
        shell.setBackground(Aero.gloss(context,new int[]{0xff6088ab,0xff365a7c,0xff183c5d},6,0xff9dbbd0));
        HorizontalScrollView scroll=new HorizontalScrollView(context);scroll.setHorizontalScrollBarEnabled(cards.size()>2);
        if(!sticky)scroll.setOnHoverListener((v,event)->{
            main.removeCallbacks(dismissFolderHover);
            if(event.getActionMasked()==MotionEvent.ACTION_HOVER_EXIT)main.postDelayed(dismissFolderHover,300);
            return false;
        });
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout row=new LinearLayout(context);row.setPadding(gap,gap,0,gap);
        scroll.addView(row,new HorizontalScrollView.LayoutParams(-2,-1));
        Map<Card,ImageView> images=new LinkedHashMap<>();
        for(Card target:cards){
            LinearLayout card=Aero.column(context);Aero.pad(card,4);
            card.setFocusable(true);card.setContentDescription("Switch to "+target.title);
            card.setBackground(Aero.panel(context,0xffedf7ff,0xffc7dfee,3,0xff7199b6));
            LinearLayout header=new LinearLayout(context);
            TextView name=Aero.text(context,target.title,12,Aero.INK);
            name.setSingleLine();name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            header.addView(name,new LinearLayout.LayoutParams(0,-1,1));
            if(target.close!=null){
                TextView close=Aero.button(context,"×",()->{dismiss();target.close.run();});
                close.setMinHeight(0);close.setPadding(0,0,0,0);
                close.setContentDescription("Close "+target.title);
                header.addView(close,new LinearLayout.LayoutParams(Aero.dp(context,29),-1));
            }
            card.addView(header,new LinearLayout.LayoutParams(-1,Aero.dp(context,26)));
            FrameLayout body=new FrameLayout(context);
            TextView detail=Aero.text(context,target.detail,11,0xff34566d);
            detail.setGravity(Gravity.CENTER);body.addView(detail,new FrameLayout.LayoutParams(-1,-1));
            if(target.source!=null){
                ImageView image=new ImageView(context);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                boolean icon=target.close==null;
                body.addView(image,new FrameLayout.LayoutParams(icon?Aero.dp(context,44):-1,icon?Aero.dp(context,44):-1,Gravity.CENTER));
                if(icon){detail.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);detail.setPadding(0,0,0,Aero.dp(context,5));}
                images.put(target,image);
            }
            card.addView(body,new LinearLayout.LayoutParams(-1,0,1));
            card.setOnClickListener(v->{dismiss();target.open.run();});
            card.setOnHoverListener((v,event)->{
                boolean over=event.getActionMasked()!=MotionEvent.ACTION_HOVER_EXIT;
                if(over)main.removeCallbacks(dismissFolderHover);
                else if(!sticky)main.postDelayed(dismissFolderHover,300);
                card.setBackground(over?Aero.panel(context,0xfff3fbff,0xffa7d3f3,3,0xfff7fbff)
                        :Aero.panel(context,0xffedf7ff,0xffc7dfee,3,0xff7199b6));
                return false;
            });
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(cardWidth,-1);size.rightMargin=gap;row.addView(card,size);
        }
        if(sticky){
            TextView options=Aero.text(context,"Window commands  ▸",11,Color.WHITE);
            options.setGravity(Gravity.CENTER);options.setContentDescription("Taskbar item commands");
            options.setOnClickListener(v->{dismiss();if(anchor.isAttachedToWindow())commands.run();});
            shell.addView(options,new LinearLayout.LayoutParams(-1,Aero.dp(context,29)));
        }
        PopupWindow picker=new PopupWindow(shell,width,height,sticky);folderPicker=picker;
        picker.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        picker.setOutsideTouchable(sticky);picker.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        picker.setElevation(Aero.dp(context,8));
        Runnable refresh=new Runnable(){@Override public void run(){
            if(folderPicker!=picker||!picker.isShowing())return;
            for(Map.Entry<Card,ImageView> entry:images.entrySet()){
                Card target=entry.getKey();View source=target.source;ImageView image=entry.getValue();
                if(source instanceof DesktopWindow&&!allowed((DesktopWindow)source)){picker.dismiss();return;}
                if(image.getDrawable()!=null&&(target.close==null||!source.isShown()))continue;
                Bitmap next=capture(source,300,185);
                if(next!=null){
                    image.setImageBitmap(next);
                    if(target.close!=null)((ViewGroup)image.getParent()).getChildAt(0).setVisibility(View.GONE);
                }
            }
            if(!images.isEmpty())main.postDelayed(this,500);
        }};
        picker.setOnDismissListener(()->{main.removeCallbacks(refresh);main.removeCallbacks(dismissFolderHover);if(folderPicker==picker)folderPicker=null;});
        int[] position=new int[2];anchor.getLocationOnScreen(position);
        int x=Math.max(safe.left+gap,Math.min(position[0]+(anchor.getWidth()-width)/2,safe.right-width-gap));
        picker.showAtLocation(anchor.getRootView(),Gravity.TOP|Gravity.LEFT,x,Math.max(safe.top,position[1]-height-gap/2));
        refresh.run();
    }
    void show(){show(host.windows(),"Switch Windows",true);}
    void flip(){
        hideKeyboard();
        dismiss();
        List<DesktopWindow> windows=new ArrayList<>(host.windows());
        Collections.reverse(windows);
        List<String> apps=host.external();
        Bitmap desktop=capture(host.wallpaper(),900,900);
        FlipStack stack=new FlipStack(activity,windows,apps,host,
                window->allowed(window)?capture(window,540,460):null,desktop,this::dismiss);
        Dialog view=new Dialog(activity);
        view.requestWindowFeature(Window.FEATURE_NO_TITLE);
        view.setContentView(stack);
        view.setTitle("Flip 3D");
        dialog=view;
        view.setOnDismissListener(d->{if(dialog==view)dialog=null;});
        view.setOnKeyListener((d,key,event)->{
            if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if(key==KeyEvent.KEYCODE_TAB&&event.isMetaPressed()){
                stack.step(event.isShiftPressed()?-1:1);
                return true;
            }
            if(key==KeyEvent.KEYCODE_ESCAPE){dismiss();return true;}
            return false;
        });
        Window chrome=view.getWindow();
        chrome.setBackgroundDrawableResource(android.R.color.transparent);
        chrome.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        view.show();
        chrome.setLayout(-1,-1);
    }
    private void hideKeyboard(){
        ((android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(),0);
    }
    private void show(java.util.List<DesktopWindow> windows,String title,boolean external){hideKeyboard();dismiss();LinearLayout body=Aero.column(activity);Aero.pad(body,10);java.util.List<Bitmap> images=new ArrayList<>();
        body.addView(Aero.text(activity,"Tap a window to switch. Previews are snapshots taken when this view opens. InPrivate windows have no preview.",12,0xff496171));
        for(DesktopWindow window:windows){
            LinearLayout card=Aero.column(activity);card.setPadding(0,Aero.dp(activity,10),0,Aero.dp(activity,10));card.setFocusable(true);card.setContentDescription("Switch to "+window.caption());card.setBackground(Aero.selection(activity));
            card.addView(Aero.text(activity,window.caption()+(window.getVisibility()!=View.VISIBLE||window.minimizing()?" — Minimized":""),15,Aero.INK));Bitmap bitmap=snapshot(window);
            if(bitmap!=null){images.add(bitmap);ImageView preview=new ImageView(activity);preview.setImageBitmap(bitmap);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(preview,new LinearLayout.LayoutParams(-1,Aero.dp(activity,145)));}
            else card.addView(Aero.text(activity,allowed(window)?"Preview unavailable":"InPrivate — preview hidden",12,0xff496171));
            card.setOnClickListener(v->{dismiss();window.restore();});body.addView(card);
        }
        if(external)for(String id:host.external())body.addView(Aero.button(activity,"Open Android app: "+host.label(id),()->{dismiss();host.open(id);}));
        if(windows.isEmpty()&&(!external||host.external().isEmpty()))body.addView(Aero.text(activity,"No open windows",16,Aero.INK));
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);AlertDialog view=Aero.dialog(activity).setTitle(title).setView(scroll).setNegativeButton("Close",null).create();dialog=view;view.setOnDismissListener(d->{images.clear();if(dialog==view)dialog=null;});view.show();
        int height=Math.round(activity.getResources().getDisplayMetrics().heightPixels*.75f);dialog.getWindow().setLayout(Math.min(Aero.dp(activity,400),activity.getResources().getDisplayMetrics().widthPixels-Aero.dp(activity,24)),height);
    }
    private void hideHover(){if(pending!=null)main.removeCallbacks(pending);pending=null;main.removeCallbacks(dismissFolderHover);}
    boolean dismiss(){boolean was=dialog!=null||folderPicker!=null;hideHover();if(folderPicker!=null)folderPicker.dismiss();if(dialog!=null){Dialog old=dialog;dialog=null;old.dismiss();}return was;}
}
