package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.TextView;

/** Consistent caption controls, independent of the device's symbol font. */
final class WindowButton extends TextView {
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
    WindowButton(Context context,String symbol,Runnable action){
        super(context);setText(symbol);setGravity(Gravity.CENTER);setFocusable(true);
        setMinHeight(0);setPadding(0,0,0,0);setOnClickListener(v->action.run());
        boolean close="×".equals(symbol);
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},Aero.gloss(context,close?new int[]{0xffd36c4e,0xff9c2f19,0xffb84422}:new int[]{0xff8ec7e6,0xff5295bc,0xffa2d9ef},3,0xff416780));
        for(int state:new int[]{android.R.attr.state_hovered,android.R.attr.state_focused})
            states.addState(new int[]{state},Aero.gloss(context,close?new int[]{0xffffc4aa,0xffec6c38,0xffc43a13,0xffee8b54}:new int[]{0xffdcf5ff,0xff8bd3f2,0xff60b4df,0xffc4edff},3,close?0xff9c442a:0xff4b99c4));
        states.addState(new int[]{},Aero.gloss(context,close?new int[]{0xffefb2a3,0xffcf6550,0xffa92d18,0xffc14f33}:new int[]{0xbbc4deeb,0xaa8daabd,0x99738c9e,0xaab0c9d6},3,close?0xff64362f:0xff627b8c));
        setBackground(states);
    }
    @Override protected void onDraw(Canvas canvas){
        float d=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f;
        String symbol=getText().toString();boolean close="×".equals(symbol);
        ink.setColor(close?Color.WHITE:0xff172c3c);ink.setAlpha(isEnabled()?255:100);
        ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(Math.max(d,1));
        if(close){ink.setStrokeWidth(1.6f*d);canvas.drawLine(cx-4*d,cy-4*d,cx+4*d,cy+4*d,ink);canvas.drawLine(cx+4*d,cy-4*d,cx-4*d,cy+4*d,ink);}
        else if("—".equals(symbol)){ink.setStrokeWidth(2*d);canvas.drawLine(cx-5*d,cy+4*d,cx+5*d,cy+4*d,ink);}
        else if("❐".equals(symbol)||"▣".equals(symbol)){
            canvas.drawRect(cx-2*d,cy-5*d,cx+5*d,cy+2*d,ink);
            ink.setStyle(Paint.Style.FILL);ink.setColor(0xffc0d9e7);canvas.drawRect(cx-5*d,cy-2*d,cx+2*d,cy+5*d,ink);
            ink.setStyle(Paint.Style.STROKE);ink.setColor(0xff172c3c);canvas.drawRect(cx-5*d,cy-2*d,cx+2*d,cy+5*d,ink);canvas.drawLine(cx-5*d,cy,cx+2*d,cy,ink);
        }else{canvas.drawRect(cx-5*d,cy-4*d,cx+5*d,cy+4*d,ink);canvas.drawLine(cx-5*d,cy-2*d,cx+5*d,cy-2*d,ink);}
    }
}
