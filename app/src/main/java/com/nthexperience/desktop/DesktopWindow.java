package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.View;
import android.view.PointerIcon;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Arrays;
import java.util.Collections;
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
    private float floatingLeft=Float.NaN,floatingTop=Float.NaN;
    private final DesktopStore store;
    private String geometryKey,captionKind="";
    final String toolId;
    private final GradientDrawable snapOutline = new GradientDrawable();
    private final int[] hostLocation = new int[2];
    private Placement preview = Placement.FLOATING, dragStartPlacement;
    private boolean captionTracking, captionDragging;
    private float downX, downY, gripX, gripY, shakeX;
    private long shakeTime;
    private int shakeDirection, shakeStrokes;
    private Runnable shake=()->{};
    private java.util.function.Consumer<String> placementChanged=value->{};
    private int dragWidth, dragTop, dragHeight;
    private final int touchSlop, snapEdge, snapMinWidth, snapMinHeight;
    private final AeroGlass glass;
    private final TextView minimizeButton,closeButton;
    private boolean active=true,minimizing,peeking;private int beforePeekAccessibility;
    private int transition;
    private DesktopContextMenu systemMenu;
    private static final int LEFT=1, RIGHT=2, TOP=4, BOTTOM=8;
    private static final int MAX_SAVED_SIZE=2000;
    private final Rect resizeStart=new Rect(), resizeArea=new Rect(), resizeBounds=new Rect();
    private final Paint gripPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int resizeCorner;
    private float resizeDownX,resizeDownY;
    DesktopWindow(Context context, FrameLayout host, String title, View body, AeroWallpaper wallpaper,Runnable changed,DesktopStore store,String toolId,String layoutKey) {
        super(context);this.body=body; this.host=host; this.changed=changed;this.store=store;this.toolId=toolId;geometryKey="window_"+layoutKey+"_"+toolId;
        touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();
        snapEdge=Aero.dp(context,24);
        snapMinWidth=Aero.dp(context,AeroSnap.MIN_SIDE_WIDTH_DP);
        snapMinHeight=Aero.dp(context,WindowArrangement.MIN_STACK_HEIGHT_DP);
        snapOutline.setColor(0x3396d9ff);
        snapOutline.setStroke(Aero.dp(context,2),0xffb6e8ff);
        setOrientation(VERTICAL); setElevation(Aero.dp(context,16));
        glass=new AeroGlass(wallpaper,AeroGlass.WINDOW);setBackground(glass);
        setPadding(Aero.dp(context,6),Aero.dp(context,2),Aero.dp(context,6),Aero.dp(context,6));
        LinearLayout bar=new LinearLayout(context); bar.setGravity(Gravity.CENTER_VERTICAL);
        String icon=FolderLinks.id(toolId)||toolId.startsWith("@explorer:")?"files":toolId.equals("@recycle")?"recycle":toolId.equals("@taskmanager")?"taskmanager":toolId.equals("@welcome")?"orb":(toolId.startsWith("folder:")||toolId.startsWith("@organizer:"))?"folderapps":toolId.equals("@backup")?"drive":toolId.equals("@organize")||toolId.equals("@quicklaunch")?"folderapps":toolId.equals("@control")?"control":toolId.equals("@appearance")?"appearance":toolId.equals("@files")?"files":toolId.equals("@filemanagers")?"folderapps":toolId.equals("@computer")||toolId.equals("@system")?"computer":toolId.equals("@notes")?"notes":toolId.equals("@photos")?"pictures":toolId.equals(MediaLibrary.PLAYER)?"mediaplayer":toolId.equals("@browser")?"browser":GadgetCatalog.toolIcon(toolId);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(Aero.dp(context,32),Aero.dp(context,32));iconParams.leftMargin=Aero.dp(context,1);bar.addView(new AeroIcon(context,icon),iconParams);
        TextView label=Aero.text(context,title,13,0xff111b22);titleLabel=label;label.setSingleLine();label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setLayerType(View.LAYER_TYPE_SOFTWARE,null);label.setShadowLayer(Aero.dp(context,6),0,0,0xffffffff); label.setPadding(Aero.dp(context,8),0,0,0);
        bar.addView(label,new LayoutParams(0,Aero.dp(context,48),1));
        TextView minimize=chrome("—",this::minimize);minimizeButton=minimize;
        maximizeButton=chrome("□",this::toggleMaximize);
        TextView close=chrome("×",this::closeWindow);closeButton=close;
        close.setTextColor(Color.WHITE);
        bar.addView(minimize); bar.addView(maximizeButton); bar.addView(close);
        addView(bar,new LayoutParams(-1,Aero.dp(context,48)));
        body.setBackgroundColor(0xfff7f8f9); addView(body,new LayoutParams(-1,0,1));
        bindCaptionIcon(bar.getChildAt(0));
        bar.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            boolean narrow=r-l<Aero.dp(context,225);
            for(View button:new View[]{minimizeButton,maximizeButton,closeButton})captionWidth(button,narrow?32:44);
            captionWidth(bar.getChildAt(0),narrow?24:32);
            label.setPadding(Aero.dp(context,narrow?3:8),0,0,0);
        });
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
    private void captionWidth(View view,int dp){
        ViewGroup.LayoutParams size=view.getLayoutParams();
        int width=Aero.dp(getContext(),dp);
        if(size.width!=width){size.width=width;view.setLayoutParams(size);}
    }
    void onShake(Runnable action){shake=action;}
    void onPlacement(java.util.function.Consumer<String> action){placementChanged=action;}
    boolean snap(Placement target){
        if((target==Placement.LEFT||target==Placement.RIGHT)&&!canSplit())return false;
        cancelCaptionDrag();
        place(target);
        return true;
    }
    private boolean canSplit(){return AeroSnap.canSplit(hostWidth(),workHeight(),snapMinWidth,snapMinHeight);}
    boolean snapped(){return placement==Placement.LEFT||placement==Placement.RIGHT;}
    boolean maximized(){return placement==Placement.MAXIMIZED;}
    private boolean fitted(){return effectivePlacement()!=Placement.FLOATING;}
    void maximize(){if(!maximized())place(Placement.MAXIMIZED);restore();}
    void restoreDown(){settle();setVisibility(VISIBLE);bringToFront();place(Placement.FLOATING);}
    void closeWindow(){if(getParent()==null)return;dismissSystemMenu();settle();hideKeyboard();if(body instanceof VistaShell)((VistaShell)body).close();if(body instanceof WeatherPanel)((WeatherPanel)body).close();if(body instanceof BrowserPanel)((BrowserPanel)body).close();if(body instanceof PhotoGallery)((PhotoGallery)body).close();if(body instanceof TaskManagerPanel)((TaskManagerPanel)body).close();if(body instanceof RecyclePanel)((RecyclePanel)body).close();host.removeView(this);changed.run();}
    void showSystemMenu(View anchor,float x,float y){dismissSystemMenu();systemMenu=new DesktopContextMenu(getContext(),"Window commands");systemMenu.item("Restore","empty",fitted()||getVisibility()!=VISIBLE||minimizing,()->{if(getVisibility()!=VISIBLE||minimizing)restore();else if(fitted())restoreDown();});systemMenu.item("Minimize","empty",getVisibility()==VISIBLE&&!minimizing,this::minimize);systemMenu.item("Maximize","empty",!maximized(),this::maximize);systemMenu.item("Snap left","empty",canSplit(),()->{restore();snap(Placement.LEFT);});
        systemMenu.item("Snap right","empty",canSplit(),()->{restore();snap(Placement.RIGHT);});
        systemMenu.item("Minimize or restore other windows","empty",shake);
        systemMenu.separator();systemMenu.item("Close","close",this::closeWindow);ContextMenus.show(systemMenu,anchor,x,y);}
    void dismissSystemMenu(){if(systemMenu!=null){systemMenu.dismiss();systemMenu=null;}}
    void arrange(int x,int y,int width,int height){settle();placement=Placement.FLOATING;normalW=Math.max(1,width);normalH=Math.max(1,height);floatingLeft=x;floatingTop=Math.max(0,y-safeTop);fitToHost();saveGeometry();}
    String caption(){return titleLabel.getText().toString();}
    void caption(String title){titleLabel.setText(title);titleLabel.setContentDescription(title+". Drag to move window.");}
    boolean minimizing(){return minimizing;}
    boolean active(){return active;}
    void active(boolean value){active=value;glass.active(value);titleLabel.setTextColor(value?0xff111b22:0xff465563);titleLabel.setShadowLayer(Aero.dp(getContext(),value?6:2),0,0,value?0xffffffff:0x99ffffff);minimizeButton.setAlpha(value?1:.65f);maximizeButton.setAlpha(value?1:.65f);closeButton.setAlpha(value?1:.6f);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(peeking)return true;if(e.getActionMasked()==MotionEvent.ACTION_DOWN&&!minimizing){bringToFront();changed.run();}return super.dispatchTouchEvent(e);}
    private int cornerReach(){return Aero.dp(getContext(),22);}
    private int resizeCornerAt(float x,float y){
        if(maximized()||minimizing)return 0;
        int reach=cornerReach();
        int side=x<reach?LEFT:x>=getWidth()-reach?RIGHT:0;
        if(side==0)return 0;
        if(y>=getHeight()-reach)return side|BOTTOM;
        // Keep the caption buttons' main touch targets clear.
        int border=Aero.dp(getContext(),6);
        if(y<reach&&(y<border||x<border||x>=getWidth()-border))return side|TOP;
        return 0;
    }
    @Override public PointerIcon onResolvePointerIcon(MotionEvent event,int pointerIndex){
        int corner=resizeCornerAt(event.getX(pointerIndex),event.getY(pointerIndex));
        if(corner==0)return super.onResolvePointerIcon(event,pointerIndex);
        boolean topLeftToBottomRight=corner==(TOP|LEFT)||corner==(BOTTOM|RIGHT);
        return PointerIcon.getSystemIcon(getContext(),topLeftToBottomRight
                ?PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW:PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW);
    }
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
        super.onSizeChanged(width,height,oldWidth,oldHeight);
        updateCornerGestures(width,height);
    }
    private void updateCornerGestures(int width,int height){
        if(Build.VERSION.SDK_INT<29)return;
        int reach=cornerReach();
        if(maximized()||width<2*reach||height<2*reach){
            setSystemGestureExclusionRects(Collections.emptyList());
            return;
        }
        // A floating window can touch the phone's Back gesture area.
        setSystemGestureExclusionRects(Arrays.asList(
                new Rect(0,0,reach,reach),new Rect(width-reach,0,width,reach),
                new Rect(0,height-reach,reach,height),new Rect(width-reach,height-reach,width,height)));
    }
    boolean isPeeking(){return peeking;}
    void peek(boolean value){
        if(peeking==value)return;
        peeking=value;glass.peek(value);
        if(value){beforePeekAccessibility=getImportantForAccessibility();setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);}
        else setImportantForAccessibility(beforePeekAccessibility);
        invalidate();
    }
    void drawPreview(Canvas canvas){
        boolean was=peeking;
        peeking=false;glass.peek(false);
        try{draw(canvas);}finally{peeking=was;glass.peek(was);}
    }
    @Override protected void dispatchDraw(Canvas canvas){
        if(peeking)return;
        super.dispatchDraw(canvas);
        if(maximized())return;
        float density=getResources().getDisplayMetrics().density;
        gripPaint.setStrokeWidth(density);
        gripPaint.setColor(active?0x99586e80:0x77627480);
        float edge=4*density;
        float bottom=getHeight()-edge;
        for(int size=4;size<=10;size+=3){
            float length=size*density;
            canvas.drawLine(edge,bottom-length,edge+length,bottom,gripPaint);
            float right=getWidth()-edge;
            canvas.drawLine(right-length,bottom,right,bottom-length,gripPaint);
        }
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getActionMasked()!=MotionEvent.ACTION_DOWN||event.isButtonPressed(MotionEvent.BUTTON_SECONDARY))
            return super.onInterceptTouchEvent(event);
        int corner=resizeCornerAt(event.getX(),event.getY());
        if(corner==0)return super.onInterceptTouchEvent(event);

        dismissSystemMenu();
        settle();
        resizeCorner=corner;
        resizeDownX=event.getRawX();
        resizeDownY=event.getRawY();
        int x=Math.round(getX()),y=Math.round(getY());
        resizeStart.set(x,y,x+getWidth(),y+getHeight());
        resizeBounds.set(resizeStart);
        resizeArea.set(0,safeTop,hostWidth(),host.getHeight());
        return true;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(resizeCorner==0)return super.onTouchEvent(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_MOVE:
                dragCorner(event.getRawX(),event.getRawY());
                break;
            case MotionEvent.ACTION_UP:
                dragCorner(event.getRawX(),event.getRawY());
                finishResize();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if(event.getActionIndex()==0)finishResize();
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelResize();
                break;
            default:break;
        }
        return true;
    }
    private void dragCorner(float rawX,float rawY){
        int dx=Math.round(rawX-resizeDownX),dy=Math.round(rawY-resizeDownY);
        int minWidth=Math.min(resizeStart.width(),Aero.dp(getContext(),220));
        int minHeight=Math.min(resizeStart.height(),Aero.dp(getContext(),180));
        int maxSize=Aero.dp(getContext(),MAX_SAVED_SIZE);
        if((resizeCorner&LEFT)!=0){
            resizeBounds.left=(int)clamp(resizeStart.left+dx,Math.max(resizeArea.left,resizeStart.right-maxSize),resizeStart.right-minWidth);
        }else{
            resizeBounds.right=(int)clamp(resizeStart.right+dx,resizeStart.left+minWidth,Math.min(resizeArea.right,resizeStart.left+maxSize));
        }
        if((resizeCorner&TOP)!=0){
            resizeBounds.top=(int)clamp(resizeStart.top+dy,Math.max(resizeArea.top,resizeStart.bottom-maxSize),resizeStart.bottom-minHeight);
        }else{
            resizeBounds.bottom=(int)clamp(resizeStart.bottom+dy,resizeStart.top+minHeight,Math.min(resizeArea.bottom,resizeStart.top+maxSize));
        }
        setX(resizeBounds.left);
        setY(resizeBounds.top);
        resize(resizeBounds.width(),resizeBounds.height());
        invalidate();
    }
    private void finishResize(){
        resizeCorner=0;
        if(resizeBounds.equals(resizeStart))return;
        if(placement!=Placement.FLOATING)placementChanged.accept(Placement.FLOATING.name());
        placement=Placement.FLOATING;
        updatePlacementButton(placement);
        normalW=resizeBounds.width();
        normalH=resizeBounds.height();
        rememberFloatingPosition();
        saveGeometry();
        changed.run();
    }
    void cancelResize(){
        if(resizeCorner==0)return;
        resizeCorner=0;
        fitToHost();
    }
    private TextView chrome(String symbol,Runnable action){
        TextView view=new WindowButton(getContext(),symbol,action);
        view.setTextSize(16);view.setMinHeight(0);view.setPadding(0,0,0,0);LayoutParams lp=new LayoutParams(Aero.dp(getContext(),44),Aero.dp(getContext(),44));lp.leftMargin=Aero.dp(getContext(),1);view.setLayoutParams(lp);
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
    void settle(){peek(false);cancelResize();cancelCaptionDrag();transition++;animate().withEndAction(null).cancel();minimizing=false;setAlpha(1);setScaleX(1);setScaleY(1);setPivotX(getWidth()/2f);setPivotY(getHeight()/2f);}
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
        shakeX=downX;shakeTime=e.getEventTime();shakeDirection=0;shakeStrokes=0;
        gripX=downX-getX();
        gripY=downY-getY();
    }

    private void moveCaption(MotionEvent e){
        host.getLocationOnScreen(hostLocation);
        float x=e.getRawX()-hostLocation[0],y=e.getRawY()-hostLocation[1];
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&detectShake(x,y,e.getEventTime())){
            cancelCaptionDrag();
            shake.run();
            return;
        }
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

    private boolean detectShake(float x,float y,long time){
        if(time-shakeTime>650||Math.abs(y-downY)>Aero.dp(getContext(),36)){
            shakeX=x;shakeTime=time;shakeDirection=0;shakeStrokes=0;return false;
        }
        float distance=x-shakeX;
        if(Math.abs(distance)<Aero.dp(getContext(),40))return false;
        int direction=distance>0?1:-1;
        if(direction==shakeDirection)return false;
        shakeX=x;
        shakeDirection=direction;
        return ++shakeStrokes>=4;
    }

    private void finishCaptionDrag(MotionEvent e){
        moveCaption(e);
        Placement target=preview;
        if(target==Placement.FLOATING){
            rememberFloatingPosition();
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
        if(resizeCorner!=0){
            if(resizeArea.top==safeTop&&resizeArea.right==hostWidth()&&resizeArea.bottom==host.getHeight())return;
            resizeCorner=0;
        }
        if(captionTracking&&(dragWidth!=hostWidth()||dragTop!=safeTop||dragHeight!=host.getHeight())){
            dragWidth=hostWidth();dragTop=safeTop;dragHeight=host.getHeight();
            if(captionDragging){
                setX(clamp(getX(),0,Math.max(0,hostWidth()-floatingWidth())));
                setY(clamp(getY(),safeTop,Math.max(safeTop,host.getHeight()-floatingHeight())));
                resize(floatingWidth(),floatingHeight());
                if(preview!=Placement.FLOATING)snapOutline.setBounds(fittedBounds(preview));
            }
        }
        if(captionDragging)return;
        Placement target=effectivePlacement();
        updatePlacementButton(target);
        if(target==Placement.FLOATING){
            int width=floatingWidth(),height=floatingHeight();
            if(Float.isNaN(floatingLeft))floatingLeft=positionX*Math.max(0,hostWidth()-width);
            if(Float.isNaN(floatingTop))floatingTop=positionY*Math.max(0,workHeight()-height);
            setX(clamp(floatingLeft,0,Math.max(0,hostWidth()-width)));
            setY(safeTop+clamp(floatingTop,0,Math.max(0,workHeight()-height)));
            resize(width,height);
        } else {
            Rect bounds=fittedBounds(target);
            setX(bounds.left);
            setY(bounds.top);
            resize(bounds.width(),bounds.height());
        }
    }

    private void rememberFloatingPosition(){
        floatingLeft=getX();floatingTop=getY()-safeTop;
        positionX=DesktopGeometry.fraction(floatingLeft,hostWidth()-floatingWidth());
        positionY=DesktopGeometry.fraction(floatingTop,workHeight()-floatingHeight());
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
        if(target!=Placement.FLOATING||placement!=Placement.FLOATING)placementChanged.accept(target.name());
        placement=target;
        updateCornerGestures(getWidth(),getHeight());
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
        float x=Float.isNaN(floatingLeft)?positionX:DesktopGeometry.fraction(floatingLeft,hostWidth()-floatingWidth());
        float y=Float.isNaN(floatingTop)?positionY:DesktopGeometry.fraction(floatingTop,workHeight()-floatingHeight());
        GadgetLayout.put(result,"x",x);GadgetLayout.put(result,"y",y);GadgetLayout.put(result,"w",normalW/density);GadgetLayout.put(result,"h",normalH/density);GadgetLayout.put(result,"maximized",maximized());
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
        floatingLeft=Float.NaN;floatingTop=Float.NaN;
        positionX=DesktopGeometry.clamp((float)value.optDouble("x",.5),0,1);positionY=DesktopGeometry.clamp((float)value.optDouble("y",.33),0,1);
        normalW=Aero.dp(getContext(),DesktopGeometry.clamp((float)value.optDouble("w",520),180,MAX_SAVED_SIZE));
        normalH=Aero.dp(getContext(),DesktopGeometry.clamp((float)value.optDouble("h",490),180,MAX_SAVED_SIZE));
        placement=target;updateCornerGestures(getWidth(),getHeight());fitToHost();
    }
    private float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}
}
