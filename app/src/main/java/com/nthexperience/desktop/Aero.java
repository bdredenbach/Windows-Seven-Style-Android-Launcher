package com.nthexperience.desktop;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Aero {
    static final int INK=0xff18232c;
    private static Typeface regular,semibold;
    static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    static Typeface face(Context c){
        if(regular==null){try{regular=Typeface.createFromAsset(c.getAssets(),"fonts/selawk.ttf");}catch(RuntimeException e){regular=Typeface.create("sans-serif",Typeface.NORMAL);}}
        return regular;
    }
    static Typeface strong(Context c){
        if(semibold==null){try{semibold=Typeface.createFromAsset(c.getAssets(),"fonts/selawksb.ttf");}catch(RuntimeException e){semibold=Typeface.create(face(c),Typeface.BOLD);}}
        return semibold;
    }
    static GradientDrawable panel(Context c,int top,int bottom,int radius,int stroke){
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});shape.setCornerRadius(dp(c,radius));if(stroke!=0)shape.setStroke(dp(c,1),stroke);return shape;
    }
    static GradientDrawable gloss(Context c,int[] colors,float radius,int stroke){
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,colors);shape.setCornerRadius(dp(c,radius));if(stroke!=0)shape.setStroke(dp(c,1),stroke);return shape;
    }
    static StateListDrawable selection(Context c){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},gloss(c,new int[]{0xfff0faff,0xffd6eeff,0xffb6defa,0xffd9f1ff},3,0xff83b4d2));
        states.addState(new int[]{android.R.attr.state_hovered},panel(c,0xfff1f9ff,0xffd9edfb,3,0xffb0cddd));
        states.addState(new int[]{android.R.attr.state_focused},panel(c,0xffedf8ff,0xffd4eaff,3,0xff97bcd3));
        states.addState(new int[]{},panel(c,0x00ffffff,0x00ffffff,3,0));return states;
    }
    static TextView text(Context c,String value,float size,int color){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setTypeface(face(c));v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(false);return v;}
    static TextView button(Context c,String label,Runnable click){
        TextView v=text(c,label,13,INK);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(c,48));v.setPadding(dp(c,10),dp(c,5),dp(c,10),dp(c,5));
        v.setBackground(buttonBackground(c));v.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff7b858d,INK}));v.setFocusable(true);v.setOnClickListener(view->click.run());return v;
    }
    static StateListDrawable buttonBackground(Context c){StateListDrawable s=new StateListDrawable();s.addState(new int[]{-android.R.attr.state_enabled},panel(c,0xfff5f5f5,0xffe7e9eb,3,0xffb5bec5));s.addState(new int[]{android.R.attr.state_pressed},gloss(c,new int[]{0xffd4eaf8,0xffb6d9ee,0xff98c5e0,0xffd2eaf7},3,0xff3c7da5));for(int state:new int[]{android.R.attr.state_focused,android.R.attr.state_hovered})s.addState(new int[]{state},gloss(c,new int[]{0xfff4fcff,0xffdcefff,0xffc5e5fa,0xffedf9ff},3,0xff3f9ac8));s.addState(new int[]{},gloss(c,new int[]{0xfffdfefe,0xffedf1f4,0xffdce3e8,0xffeef3f6},3,0xff8599a6));return s;}
    static boolean motion(Context c){return !c.getSharedPreferences("desktop",0).getBoolean("reduce_motion",false)&&android.animation.ValueAnimator.areAnimatorsEnabled();}
    static android.widget.CheckBox check(Context c,String label,boolean checked){android.widget.CheckBox b=new android.widget.CheckBox(c);b.setText(label);b.setContentDescription(label);b.setTypeface(face(c));b.setTextSize(13);b.setTextColor(INK);b.setButtonDrawable(new AeroCheck(c,false));b.setPadding(dp(c,3),0,dp(c,4),0);b.setMinHeight(dp(c,48));b.setChecked(checked);return b;}
    static android.app.AlertDialog.Builder dialog(Context c){return new VistaDialog(c);}
    static void styleDialog(Context c,android.app.AlertDialog dialog){android.view.Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(panel(c,0xfff8fbfd,0xffe8eef3,5,0xff7e9aac));styleText(w.getDecorView());}for(int which:new int[]{-1,-2,-3}){android.widget.Button b=dialog.getButton(which);if(b!=null){b.setTypeface(face(c));b.setAllCaps(false);b.setTextColor(INK);b.setBackground(buttonBackground(c));}}}
    private static void styleText(View v){if(v instanceof TextView)((TextView)v).setTypeface(face(v.getContext()));if(v instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)v).getChildCount();i++)styleText(((android.view.ViewGroup)v).getChildAt(i));}
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static void bold(TextView v){v.setTypeface(strong(v.getContext()));}
    static void pad(View v,int value){int p=dp(v.getContext(),value);v.setPadding(p,p,p,p);}
    private Aero(){}
}
