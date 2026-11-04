package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.*;

import static com.nthexperience.desktop.AeroSnap.Placement;

/** Floating windows are for our built-in tools; Android apps launch normally. */
final class DesktopWindow extends LinearLayout {
    private final FrameLayout host;
    final View body;
    private final Runnable changed;
    private final TextView maximizeButton;
    private final TextView titleLabel;
    private Placement placement=Placement.FLOATING;
    private int normalW, normalH,safeTop,safeRight;
    private float positionX=.5f,positionY=.33f;
    private final DesktopStore store;
    private String geometryKey,captionKind="";
    final String toolId;
    private final GradientDrawable snapOutline = new GradientDrawable();
    private final int[] hostLocation = new int[2];
    private Placement preview = Placement.FLOATING, dragStartPlacement;
    private boolean captionTracking, captionDragging;
    private float downX, downY, gripX, gripY;
    private int dragWidth, dragTop, dragHeight;
    private final int touchSlop, snapEdge, snapMinWidth, snapMinHeight;
    private final AeroGlass glass;
    private final TextView minimizeButton,closeButton;
    private boolean active=true,minimizing;
    private int transition;
    private DesktopContextMenu systemMenu;
    DesktopWindow(Context context, FrameLayout host, String title, View body, AeroWallpaper wallpaper,Runnable changed,DesktopStore store,String toolId,String layoutKey) {
        super(context);this.body=body; this.host=host; this.changed=changed;this.store=store;this.toolId=toolId;geometryKey="window_"+layoutKey+"_"+toolId;
        touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();
        snapEdge=Aero.dp(context,24);
        snapMinWidth=Aero.dp(context,WindowArrangement.MIN_SIDE_WIDTH_DP);
        snapMinHeight=Aero.dp(context,WindowArrangement.MIN_STACK_HEIGHT_DP);
        snapOutline.setColor(0x3396d9ff);
        snapOutline.setStroke(Aero.dp(context,2),0xffb6e8ff);
        setOrientation(VERTICAL); setElevation(Aero.dp(context,16));
        glass=new AeroGlass(wallpaper,AeroGlass.WINDOW);setBackground(glass);
        setPadding(Aero.dp(context,6),Aero.dp(context,2),Aero.dp(context,6),Aero.dp(context,6));
        LinearLayout bar=new LinearLayout(context); bar.setGravity(Gravity.CENTER_VERTICAL);
        String icon=FolderLinks.id(toolId)||toolId.startsWith("@explorer:")?"files":toolId.equals("@recycle")?"recycle":toolId.equals("@taskmanager")?"taskmanager":toolId.equals("@welcome")?"orb":(toolId.startsWith("folder:")||toolId.startsWith("@organizer:"))?"folderapps":toolId.equals("@backup")?"drive":toolId.equals("@organize")||toolId.equals("@quicklaunch")?"folderapps":toolId.equals("@control")?"control":toolId.equals("@appearance")?"appearance":toolId.equals("@files")?"files":toolId.equals("@filemanagers")?"folderapps":toolId.equals("@computer")||toolId.equals("@system")?"computer":toolId.equals("@notes")?"notes":toolId.equals("@photos")?"pictures":toolId.equals(MediaLibrary.PLAYER)?"musicfile":toolId.equals("@browser")?"browser":GadgetCatalog.toolIcon(toolId);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(Aero.dp(context,32),Aero.dp(context,32));iconParams.leftMargin=Aero.dp(context,1);bar.addView(new AeroIcon(context,icon),iconParams);
        TextView label=Aero.text(context,title,13,0xff111b22);titleLabel=label;label.setSingleLine();label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setLayerType(View.LAYER_TYPE_SOFTWARE,null);label.setShadowLayer(Aero.dp(context,6),0,0,0xffffffff); label.setPadding(Aero.dp(context,8),0,0,0);
        bar.addView(label,new LayoutParams(0,Aero.dp(context,48),1));
        TextView minimize=chrome("—",this::minimize);minimizeButton=minimize;
        maximizeButton=chrome("□",this::toggleMaximize);
        TextView close=chrome("×",this::closeWindow);closeButton=close;
        close.setTextColor(Color.WHITE);
        close.setBackground(Aero.gloss(context,new int[]{0xffefb2a3,0xffcf6550,0xffa92d18,0xffc14f33},3,0xff64362f));
        bar.addView(minimize); bar.addView(maximizeButton); bar.addView(close);
        addView(bar,new LayoutParams(-1,Aero.dp(context,48)));
        body.setBackgroundColor(0xfff7f8f9); addView(body,new LayoutParams(-1,0,1));
        bindCaptionIcon(bar.getChildAt(0));
        GestureDetector captionGestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onDoubleTap(MotionEvent e){toggleMaximize();return true;}
            @Override public void onLongPress(MotionEvent e){
                if(captionTracking&&!captionDragging){
                    cancelCaptionDrag();
                    showSystemMenu(label,e.getRawX(),e.getRawY());
                }
            }
        });
        label.setOnContextClickListener(v->{ContextMenus.centered(label,(x,y)->showSystemMenu(label,x,y));return true;});
        label.setOnTouchListener((v,e)->{
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    if(e.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){
                        showSystemMenu(label,e.getRawX(),e.getRawY());
                        return true;
                    }
                    settle();
                    captionGestures.onTouchEvent(e);
                    beginCaptionDrag(e);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if(captionTracking)moveCaption(e);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_DOWN:
                    cancelCaptionDrag();
                    break;
                default:
                    break;
            }
            captionGestures.onTouchEvent(e);
            if(e.getActionMasked()==MotionEvent.ACTION_UP&&captionTracking){
                if(captionDragging)finishCaptionDrag(e);
                else {captionTracking=false;v.performClick();}
            }
            return true;
        });
        label.setContentDescription(title+". Drag to move window.");
    }
    private void bindCaptionIcon(View icon){icon.setPadding(Aero.dp(getContext(),6),Aero.dp(getContext(),6),Aero.dp(getContext(),6),Aero.dp(getContext(),6));icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);icon.setContentDescription("Window menu");icon.setFocusable(true);icon.setOnClickListener(v->ContextMenus.centered(icon,(x,y)->showSystemMenu(icon,x,y)));ContextMenus.bind(icon,(x,y)->showSystemMenu(icon,x,y));}
    boolean captionIcon(String kind){if(captionKind.equals(kind))return false;captionKind=kind;ViewGroup chrome=(ViewGroup)getChildAt(0);View old=chrome.getChildAt(0);ViewGroup.LayoutParams params=old.getLayoutParams();chrome.removeViewAt(0);View icon=new AeroIcon(getContext(),kind);bindCaptionIcon(icon);chrome.addView(icon,0,params);return true;}
    boolean maximized(){return placement==Placement.MAXIMIZED;}
    private boolean fitted(){return effectivePlacement()!=Placement.FLOATING;}
    void maximize(){if(!maximized())place(Placement.MAXIMIZED);restore();}
    void restoreDown(){settle();setVisibility(VISIBLE);bringToFront();place(Placement.FLOATING);}
    void closeWindow(){if(getParent()==null)return;dismissSystemMenu();settle();hideKeyboard();if(body instanceof VistaShell)((VistaShell)body).close();if(body instanceof WeatherPanel)((WeatherPanel)body).close();if(body instanceof BrowserPanel)((BrowserPanel)body).close();if(body instanceof PhotoGallery)((PhotoGallery)body).close();if(body instanceof TaskManagerPanel)((TaskManagerPanel)body).close();if(body instanceof RecyclePanel)((RecyclePanel)body).close();host.removeView(this);changed.run();}
    void showSystemMenu(View anchor,float x,float y){dismissSystemMenu();systemMenu=new DesktopContextMenu(getContext(),"Window commands");systemMenu.item("Restore","empty",fitted()||getVisibility()!=VISIBLE||minimizing,()->{if(getVisibility()!=VISIBLE||minimizing)restore();else if(fitted())restoreDown();});systemMenu.item("Minimize","empty",getVisibility()==VISIBLE&&!minimizing,this::minimize);systemMenu.item("Maximize","empty",!maximized(),this::maximize);systemMenu.separator();systemMenu.item("Close","close",this::closeWindow);ContextMenus.show(systemMenu,anchor,x,y);}
    void dismissSystemMenu(){if(systemMenu!=null){systemMenu.dismiss();systemMenu=null;}}
    void arrange(int x,int y,int width,int height){settle();placement=Placement.FLOATING;normalW=Math.max(1,width);normalH=Math.max(1,height);positionX=DesktopGeometry.fraction(x,hostWidth()-normalW);positionY=DesktopGeometry.fraction(y,host.getHeight()-safeTop-normalH);fitToHost();saveGeometry();}
    String caption(){return titleLabel.getText().toString();}
    void caption(String title){titleLabel.setText(title);titleLabel.setContentDescription(title+". Drag to move window.");}
    boolean minimizing(){return minimizing;}
    boolean active(){return active;}
    void active(boolean value){active=value;glass.active(value);titleLabel.setTextColor(value?0xff111b22:0xff465563);titleLabel.setShadowLayer(Aero.dp(getContext(),value?6:2),0,0,value?0xffffffff:0x99ffffff);minimizeButton.setAlpha(value?1:.65f);maximizeButton.setAlpha(value?1:.65f);closeButton.setAlpha(value?1:.6f);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN&&!minimizing){bringToFront();changed.run();}return super.dispatchTouchEvent(e);}
    private TextView chrome(String symbol,Runnable action){
        TextView view=Aero.button(getContext(),symbol,action);
        view.setTextSize(16);view.setMinHeight(0);view.setPadding(0,0,0,0);LayoutParams lp=new LayoutParams(Aero.dp(getContext(),44),Aero.dp(getContext(),44));lp.leftMargin=Aero.dp(getContext(),1);view.setLayoutParams(lp);
        view.setBackground(Aero.gloss(getContext(),new int[]{0xbbc4deeb,0xaa8daabd,0x99738c9e,0xaab0c9d6},3,0xff627b8c));
        view.setContentDescription(symbol.equals("×")?"Close window":symbol.equals("—")?"Minimize window":"Maximize or restore window");
        return view;
    }
    void open(){
        int margin=Aero.dp(getContext(),12);
        int width=Math.min(Aero.dp(getContext(),520),hostWidth()-margin*2);
        int height=Math.min(Aero.dp(getContext(),490),host.getHeight()-safeTop-margin*2);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.max(1,width),Math.max(1,height));
        normalW=width;normalH=height;host.addView(this,lp); setX((hostWidth()-width)/2f); setY(safeTop+Math.max(margin,(host.getHeight()-safeTop-height)/3f));
        if((toolId.equals("@browser")||toolId.equals("@photos")||toolId.equals(MediaLibrary.PLAYER))&&!store.prefs.contains(geometryKey))placement=Placement.MAXIMIZED;
        readGeometry(null);fitToHost();changed.run();
    }
    private void hideKeyboard(){
        ((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);
        clearFocus();
    }
    void settle(){cancelCaptionDrag();transition++;animate().withEndAction(null).cancel();minimizing=false;setAlpha(1);setScaleX(1);setScaleY(1);setPivotX(getWidth()/2f);setPivotY(getHeight()/2f);}
    void minimize(){dismissSystemMenu();hideKeyboard();settle();if(!Aero.motion(getContext())){setVisibility(GONE);changed.run();return;}minimizing=true;int token=++transition;setPivotX(getWidth()*.2f);setPivotY(host.getHeight()-getY()+Aero.dp(getContext(),20));changed.run();animate().scaleX(.15f).scaleY(.15f).alpha(0).setDuration(180).withEndAction(()->{if(token!=transition)return;setVisibility(GONE);settle();changed.run();}).start();}
    void restore(){boolean hidden=getVisibility()!=VISIBLE||minimizing;settle();setVisibility(VISIBLE);bringToFront();changed.run();if(hidden&&Aero.motion(getContext())){int token=++transition;setPivotX(getWidth()*.2f);setPivotY(host.getHeight()-getY()+Aero.dp(getContext(),20));setScaleX(.18f);setScaleY(.18f);setAlpha(0);animate().scaleX(1).scaleY(1).alpha(1).setDuration(190).withEndAction(()->{if(token==transition)settle();}).start();}}
    @Override protected void onDetachedFromWindow(){dismissSystemMenu();settle();super.onDetachedFromWindow();}
    private void beginCaptionDrag(MotionEvent e){
        captionTracking=true;
        dragStartPlacement=placement;
        dragWidth=hostWidth();
        dragTop=safeTop;
        dragHeight=host.getHeight();
        host.getLocationOnScreen(hostLocation);
        downX=e.getRawX()-hostLocation[0];
        downY=e.getRawY()-hostLocation[1];
        gripX=downX-getX();
        gripY=downY-getY();
    }

    private void moveCaption(MotionEvent e){
        host.getLocationOnScreen(hostLocation);
        float x=e.getRawX()-hostLocation[0],y=e.getRawY()-hostLocation[1];
        if(!captionDragging){
            if(Math.hypot(x-downX,y-downY)<=touchSlop)return;
            captionDragging=true;
            if(fitted()){
                float fraction=clamp(gripX/Math.max(1,getWidth()),0,1);
                resize(floatingWidth(),floatingHeight());
                gripX=fraction*floatingWidth();
                gripY=clamp(gripY,0,Aero.dp(getContext(),48));
            }
            placement=Placement.FLOATING;
            updatePlacementButton(Placement.FLOATING);
        }
        setX(clamp(x-gripX,0,Math.max(0,hostWidth()-floatingWidth())));
        setY(clamp(y-gripY,safeTop,Math.max(safeTop,host.getHeight()-floatingHeight())));
        showSnapPreview(AeroSnap.target(x,y,hostWidth(),safeTop,host.getHeight(),snapEdge,snapMinWidth,snapMinHeight));
    }

    private void finishCaptionDrag(MotionEvent e){
        moveCaption(e);
        Placement target=preview;
        if(target==Placement.FLOATING){
            positionX=DesktopGeometry.fraction(getX(),hostWidth()-floatingWidth());
            positionY=DesktopGeometry.fraction(getY()-safeTop,host.getHeight()-safeTop-floatingHeight());
        }
        captionTracking=false;
        captionDragging=false;
        showSnapPreview(Placement.FLOATING);
        place(target);
    }

    void cancelCaptionDrag(){
        if(resetCaptionDrag())fitToHost();
    }

    private boolean resetCaptionDrag(){
        captionTracking=false;
        if(!captionDragging)return false;
        captionDragging=false;
        placement=dragStartPlacement;
        showSnapPreview(Placement.FLOATING);
        return true;
    }

    private void showSnapPreview(Placement target){
        if(target==preview)return;
        if(target==Placement.FLOATING)host.getOverlay().remove(snapOutline);
        else {
            snapOutline.setBounds(fittedBounds(target));
            if(preview==Placement.FLOATING)host.getOverlay().add(snapOutline);
        }
        preview=target;
    }

    private int hostWidth(){return Math.max(1,host.getWidth()-safeRight);}
    private int workHeight(){return Math.max(1,host.getHeight()-safeTop);}
    private int floatingWidth(){return Math.max(1,Math.min(normalW,hostWidth()));}
    private int floatingHeight(){return Math.max(1,Math.min(normalH,workHeight()));}
    void safeRight(int inset){int next=Math.max(0,Math.min(inset,Math.max(0,host.getWidth()-Aero.dp(getContext(),180))));if(safeRight!=next){safeRight=next;fitToHost();}}
    void safeTop(int inset){safeTop=Math.max(0,inset);fitToHost();}

    private Placement effectivePlacement(){
        if((placement==Placement.LEFT||placement==Placement.RIGHT)
                &&!AeroSnap.canSplit(hostWidth(),workHeight(),snapMinWidth,snapMinHeight))return Placement.FLOATING;
        return placement;
    }

    private Rect fittedBounds(Placement target){
        int width=hostWidth(),split=width/2;
        return new Rect(target==Placement.RIGHT?split:0,safeTop,
                target==Placement.LEFT?split:width,safeTop+workHeight());
    }

    void fitToHost(){
        if(getParent()==null)return;
        if(captionTracking&&(dragWidth!=hostWidth()||dragTop!=safeTop||dragHeight!=host.getHeight())){
            resetCaptionDrag();
        }
        if(captionDragging)return;
        Placement target=effectivePlacement();
        updatePlacementButton(target);
        if(target==Placement.FLOATING){
            int width=floatingWidth(),height=floatingHeight();
            setX(positionX*Math.max(0,hostWidth()-width));
            setY(safeTop+positionY*Math.max(0,workHeight()-height));
            resize(width,height);
        } else {
            Rect bounds=fittedBounds(target);
            setX(bounds.left);
            setY(bounds.top);
            resize(bounds.width(),bounds.height());
        }
    }

    private void updatePlacementButton(Placement target){
        String symbol=target==Placement.FLOATING?"□":"❐";
        if(!symbol.contentEquals(maximizeButton.getText()))maximizeButton.setText(symbol);
    }

    private void resize(int width,int height){
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)getLayoutParams();
        // Identical parameters would continually request another host layout.
        if(lp.width!=width||lp.height!=height){lp.width=width;lp.height=height;setLayoutParams(lp);}
    }

    private void place(Placement target){
        placement=target;
        fitToHost();
        saveGeometry();
        changed.run();
    }

    private void toggleMaximize(){
        cancelCaptionDrag();
        place(fitted()?Placement.FLOATING:Placement.MAXIMIZED);
    }

    JSONObject geometry(){
        JSONObject result=new JSONObject();float density=getResources().getDisplayMetrics().density;
        GadgetLayout.put(result,"x",positionX);GadgetLayout.put(result,"y",positionY);GadgetLayout.put(result,"w",normalW/density);GadgetLayout.put(result,"h",normalH/density);GadgetLayout.put(result,"maximized",maximized());
        if(placement==Placement.LEFT||placement==Placement.RIGHT)GadgetLayout.put(result,"snap",placement.name());
        return result;
    }
    void saveGeometry(){store.prefs.edit().putString(geometryKey,geometry().toString()).apply();}
    void orientation(String key){saveGeometry();JSONObject previous=geometry();geometryKey="window_"+key+"_"+toolId;readGeometry(previous);}
    void readGeometry(JSONObject fallback){
        JSONObject value=fallback;String saved=store.prefs.getString(geometryKey,null);if(saved!=null)try{value=new JSONObject(saved);}catch(JSONException ignored){}
        if(value==null)return;
        applyGeometry(value,readPlacement(fallback==null?value:fallback));
    }
    void restoreGeometry(JSONObject value){if(value!=null)applyGeometry(value,readPlacement(value));}
    private static Placement readPlacement(JSONObject value){
        if(value.optBoolean("maximized"))return Placement.MAXIMIZED;
        switch(value.optString("snap")){
            case "LEFT": return Placement.LEFT;
            case "RIGHT": return Placement.RIGHT;
            default: return Placement.FLOATING;
        }
    }
    private void applyGeometry(JSONObject value,Placement target){
        positionX=DesktopGeometry.clamp((float)value.optDouble("x",.5),0,1);positionY=DesktopGeometry.clamp((float)value.optDouble("y",.33),0,1);
        normalW=Aero.dp(getContext(),Math.round(DesktopGeometry.clamp((float)value.optDouble("w",520),180,900)));
        normalH=Aero.dp(getContext(),Math.round(DesktopGeometry.clamp((float)value.optDouble("h",490),180,900)));
        placement=target;fitToHost();
    }
    private float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}
}
