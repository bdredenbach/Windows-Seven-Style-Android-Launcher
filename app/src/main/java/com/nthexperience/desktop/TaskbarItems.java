package com.nthexperience.desktop;

import android.content.Context;
import android.view.View;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/** Two 48dp rows keep every button at its normal touch size; overflow still scrolls. */
final class TaskbarItems extends LinearLayout {
    boolean expanded;
    private boolean touching;
    boolean touching(){return touching;}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN)touching=true;
        try{boolean handled=super.dispatchTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&!handled)touching=false;return handled;}finally{if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)touching=false;}
    }
    @Override protected void onDetachedFromWindow(){touching=false;super.onDetachedFromWindow();}
    TaskbarItems(Context context,boolean expanded){super(context);this.expanded=expanded;}
    void expanded(boolean value){if(expanded!=value){expanded=value;requestLayout();}}
    @Override protected void onMeasure(int width,int height){
        if(!expanded){super.onMeasure(width,height);return;}
        int total=0,column=0;
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)child.getLayoutParams();
            child.measure(MeasureSpec.makeMeasureSpec(Math.max(0,lp.width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Aero.dp(getContext(),48),MeasureSpec.EXACTLY));
            column=Math.max(column,child.getMeasuredWidth()+lp.rightMargin);
            if(i%2==1||i==getChildCount()-1){total+=column;column=0;}
        }
        setMeasuredDimension(Math.max(total,MeasureSpec.getMode(width)==MeasureSpec.EXACTLY?MeasureSpec.getSize(width):getSuggestedMinimumWidth()),resolveSize(Aero.dp(getContext(),96),height));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        if(!expanded){super.onLayout(changed,left,top,right,bottom);return;}
        int x=0,column=0;
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)child.getLayoutParams();int y=i%2*Aero.dp(getContext(),48);
            child.layout(x,y,x+child.getMeasuredWidth(),y+child.getMeasuredHeight());column=Math.max(column,child.getMeasuredWidth()+lp.rightMargin);
            if(i%2==1){x+=column;column=0;}
        }
    }
}
