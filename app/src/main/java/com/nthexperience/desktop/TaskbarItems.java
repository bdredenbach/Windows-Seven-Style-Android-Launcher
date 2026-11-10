package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Canvas;
import android.view.*;
import android.widget.*;
import java.util.Map;

/** Two independent horizontal viewports, sharing stable task identities and drag/drop. */
final class TaskbarItems extends LinearLayout {
    boolean expanded;
    private boolean touching;
    boolean touching(){return touching;}
    private int bottomOffset,bottomEnd=Integer.MAX_VALUE,viewport;
    private final int[] rowWidth={0,0},rowScroll={0,0};
    private final OverScroller[] fling;
    private boolean scrollable=true,panning;private int touchRow;private float downX,downY,lastX;private VelocityTracker velocity;
    private static final class Slot extends LinearLayout.LayoutParams {
        final boolean running;final int preferredWidth;int row;
        Slot(LinearLayout.LayoutParams source,boolean running){super(source);this.running=running;preferredWidth=source.width;row=running?0:1;}
    }
    TaskbarItems(Context context,boolean expanded){super(context);this.expanded=expanded;fling=new OverScroller[]{new OverScroller(context),new OverScroller(context)};}
    static void slot(View child,boolean running){child.setLayoutParams(new Slot((LinearLayout.LayoutParams)child.getLayoutParams(),running));}
    static boolean running(View child){return child.getLayoutParams() instanceof Slot&&((Slot)child.getLayoutParams()).running;}
    static int row(View child){return child.getLayoutParams() instanceof Slot?((Slot)child.getLayoutParams()).row:0;}
    void assignRows(Map<String,Integer> choices){for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getLayoutParams() instanceof Slot){Slot p=(Slot)child.getLayoutParams();p.row=choices.getOrDefault(TaskbarReorder.key(child),p.running?0:1);}}requestLayout();}
    void expanded(boolean value){if(expanded!=value){expanded=value;stopScrolling();requestLayout();}}
    void scrollable(boolean value){scrollable=value;if(!value){stopScrolling();rowScroll[0]=rowScroll[1]=0;requestLayout();}}
    int bottomOffset(){return bottomOffset;}
    int bottomRoom(int width){return Math.max(0,Math.min(width,bottomEnd)-bottomOffset);}
    boolean bottomBounds(int start,int end,int width){if(bottomOffset==start&&bottomEnd==end&&viewport==width)return false;bottomOffset=start;bottomEnd=end;viewport=width;requestLayout();invalidate();return true;}
    int rowAt(float y){return expanded&&y>=Aero.dp(getContext(),48)?1:0;}
    int rowStart(int row){return row==1?bottomOffset:0;}
    int rowEnd(int row){return row==1?Math.min(getWidth(),bottomEnd):getWidth();}
    int rowScroll(int row){return rowScroll[row];}
    int rowRange(int row){return Math.max(0,rowWidth[row]-Math.max(0,rowEnd(row)-rowStart(row)));}
    boolean scrollRowBy(int row,int delta){return scrollRowTo(row,rowScroll[row]+delta);}
    boolean scrollRowTo(int row,int value){int next=scrollable?Math.max(0,Math.min(rowRange(row),value)):0;if(next==rowScroll[row])return false;rowScroll[row]=next;layoutRows();invalidate();return true;}
    void stopScrolling(){for(OverScroller scroller:fling)scroller.abortAnimation();}
    int[] scrollPositions(){return rowScroll.clone();}
    void restoreScroll(int[] positions){if(positions!=null&&positions.length==2){rowScroll[0]=positions[0];rowScroll[1]=positions[1];requestLayout();}}
    int singleRowWidth(){int total=0;for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE)continue;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)child.getLayoutParams();total+=(p instanceof Slot?((Slot)p).preferredWidth:p.width)+p.rightMargin;}return total;}
    @Override protected void onMeasure(int width,int height){
        if(!expanded){super.onMeasure(width,height);return;}
        rowWidth[0]=rowWidth[1]=0;
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);if(child.getVisibility()==GONE)continue;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)child.getLayoutParams();
            child.measure(MeasureSpec.makeMeasureSpec(Math.max(0,p.width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Aero.dp(getContext(),48),MeasureSpec.EXACTLY));rowWidth[row(child)]+=child.getMeasuredWidth()+p.rightMargin;
        }
        int measured=MeasureSpec.getMode(width)==MeasureSpec.UNSPECIFIED?viewport:MeasureSpec.getSize(width);
        setMeasuredDimension(Math.max(0,measured),resolveSize(Aero.dp(getContext(),96),height));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){if(!expanded){super.onLayout(changed,left,top,right,bottom);return;}layoutRows();}
    private void layoutRows(){
        if(!expanded)return;
        for(int row=0;row<2;row++)rowScroll[row]=scrollable?Math.max(0,Math.min(rowRange(row),rowScroll[row])):0;
        int[] x={-rowScroll[0],bottomOffset-rowScroll[1]};int height=Aero.dp(getContext(),48);
        for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE)continue;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)child.getLayoutParams();int row=row(child),y=row*height;child.layout(x[row],y,x[row]+child.getMeasuredWidth(),y+child.getMeasuredHeight());x[row]+=child.getMeasuredWidth()+p.rightMargin;}
    }
    @Override protected boolean drawChild(Canvas canvas,View child,long time){if(!expanded)return super.drawChild(canvas,child,time);int row=row(child),save=canvas.save();canvas.clipRect(rowStart(row),row*Aero.dp(getContext(),48),rowEnd(row),(row+1)*Aero.dp(getContext(),48));boolean drawn=super.drawChild(canvas,child,time);canvas.restoreToCount(save);return drawn;}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(expanded&&event.getActionMasked()==MotionEvent.ACTION_DOWN){int row=rowAt(event.getY());if(event.getX()<rowStart(row)||event.getX()>=rowEnd(row))return false;}
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN)touching=true;
        try{boolean handled=super.dispatchTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&!handled)touching=false;return handled;}finally{if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)touching=false;}
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(!expanded||!scrollable)return false;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:touchRow=rowAt(event.getY());downX=lastX=event.getX();downY=event.getY();panning=false;fling[touchRow].abortAnimation();track(event);return false;
            case MotionEvent.ACTION_MOVE:
                if(Math.abs(event.getX()-downX)>Aero.dp(getContext(),12)&&Math.abs(event.getX()-downX)>Math.abs(event.getY()-downY)*1.35f){panning=true;getParent().requestDisallowInterceptTouchEvent(true);return true;}return false;
            case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:recycleVelocity();panning=false;return false;
            default:return false;
        }
    }
    private void track(MotionEvent event){if(velocity==null)velocity=VelocityTracker.obtain();velocity.addMovement(event);}
    private void recycleVelocity(){if(velocity!=null){velocity.recycle();velocity=null;}}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!expanded||!scrollable)return super.onTouchEvent(event);track(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:touchRow=rowAt(event.getY());downX=lastX=event.getX();downY=event.getY();panning=false;fling[touchRow].abortAnimation();return true;
            case MotionEvent.ACTION_MOVE:scrollRowBy(touchRow,Math.round(lastX-event.getX()));lastX=event.getX();panning=true;return true;
            case MotionEvent.ACTION_UP:
                if(panning){velocity.computeCurrentVelocity(1000,ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());int speed=Math.round(-velocity.getXVelocity());if(Math.abs(speed)>ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity()){fling[touchRow].fling(rowScroll[touchRow],0,speed,0,0,rowRange(touchRow),0,0);postInvalidateOnAnimation();}}else performClick();recycleVelocity();panning=false;return true;
            case MotionEvent.ACTION_CANCEL:recycleVelocity();panning=false;return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){return super.performClick();}
    @Override public boolean onGenericMotionEvent(MotionEvent event){if(expanded&&scrollable&&event.getAction()==MotionEvent.ACTION_SCROLL){float axis=event.getAxisValue(MotionEvent.AXIS_HSCROLL);if(axis==0)axis=event.getAxisValue(MotionEvent.AXIS_VSCROLL);return scrollRowBy(rowAt(event.getY()),Math.round(-axis*Aero.dp(getContext(),48)));}return super.onGenericMotionEvent(event);}
    @Override public void computeScroll(){super.computeScroll();if(!expanded)return;boolean active=false;for(int row=0;row<2;row++)if(fling[row].computeScrollOffset()){scrollRowTo(row,fling[row].getCurrX());active=true;}if(active)postInvalidateOnAnimation();}
    @Override public void requestChildFocus(View child,View focused){super.requestChildFocus(child,focused);if(expanded)post(()->{if(child.getParent()==this&&child.hasFocus())requestChildRectangleOnScreen(child,new android.graphics.Rect(0,0,child.getWidth(),child.getHeight()),true);});}
    @Override public boolean requestChildRectangleOnScreen(View child,android.graphics.Rect rect,boolean immediate){if(expanded){int row=row(child),left=child.getLeft()+rect.left,right=child.getLeft()+rect.right;int dx=left<rowStart(row)?left-rowStart(row):right>rowEnd(row)?right-rowEnd(row):0;return scrollRowBy(row,dx);}return super.requestChildRectangleOnScreen(child,rect,immediate);}
    @Override protected void onDetachedFromWindow(){touching=false;stopScrolling();recycleVelocity();super.onDetachedFromWindow();}
}
