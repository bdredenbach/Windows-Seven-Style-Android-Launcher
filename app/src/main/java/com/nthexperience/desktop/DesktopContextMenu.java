package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.view.*;
import android.widget.*;

/** A pointer-anchored Vista menu, shared by desktop long-press and mouse right-click. */
final class DesktopContextMenu {
    private final Context context;
    private final LinearLayout items;
    private final PopupWindow popup;
    private View owner;
    private final int rowHeight,menuWidth;
    private final View.OnLayoutChangeListener bounds=(v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)dismiss();};
    private final View.OnAttachStateChangeListener lifetime=new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){dismiss();}};
    DesktopContextMenu(Context context){this(context,"Desktop context menu");}
    DesktopContextMenu(Context context,String description){this(context,description,36,238);}
    DesktopContextMenu(Context context,String description,int rowHeight,int menuWidth){
        this.context=context;this.rowHeight=rowHeight;this.menuWidth=menuWidth;items=Aero.column(context);items.setContentDescription(description);
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.setBackground(Aero.panel(context,0xfffafafa,0xfff0f0f0,1,0xff777777));scroll.setPadding(dp(2),dp(2),dp(2),dp(2));scroll.addView(items);
        popup=new PopupWindow(scroll,dp(menuWidth),-2,true);popup.setBackgroundDrawable(Aero.panel(context,0xfff8f8f8,0xffeeeeee,1,0xff777777));popup.setOutsideTouchable(true);popup.setElevation(dp(8));popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);popup.setOnDismissListener(()->{if(owner!=null){owner.removeOnAttachStateChangeListener(lifetime);owner.removeOnLayoutChangeListener(bounds);owner=null;}});
    }
    void item(String title,String icon,Runnable action){item(title,icon,true,action);}
    void item(String title,String icon,boolean enabled,Runnable action){
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setFocusable(true);row.setContentDescription(title);
        StateListDrawable hover=new StateListDrawable();for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_focused,android.R.attr.state_hovered})hover.addState(new int[]{state},Aero.panel(context,0xffedf7ff,0xffcce5f7,2,0xff80aed0));hover.addState(new int[]{},Aero.panel(context,0x00ffffff,0x00ffffff,0,0));row.setBackground(hover);
        FrameLayout gutter=new FrameLayout(context);gutter.setBackground(Aero.panel(context,0xfff0f2f4,0xffe8ecef,0,0));View image;
        if(icon.equals("empty")){image=new View(context);}else if(icon.equals("checked")){TextView mark=Aero.text(context,"✓",18,0xff235f91);mark.setGravity(Gravity.CENTER);image=mark;}else if(icon.equals("refresh")){TextView glyph=Aero.text(context,"↻",20,0xff4b86ab);glyph.setGravity(Gravity.CENTER);image=glyph;}else image=new AeroIcon(context,icon);
        gutter.addView(image,new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));row.addView(gutter,new LinearLayout.LayoutParams(dp(31),-1));
        TextView label=Aero.text(context,title,rowHeight<36?12:13,0xff151a1f);label.setPadding(dp(9),0,dp(12),0);row.addView(label,new LinearLayout.LayoutParams(0,-1,1));row.setEnabled(enabled);row.setAlpha(enabled?1:.42f);row.setOnClickListener(v->{if(enabled){dismiss();action.run();}});items.addView(row,new LinearLayout.LayoutParams(-1,dp(rowHeight)));
    }
    void section(String title){
        TextView label=Aero.text(context,title,11,0xff345974);
        label.setPadding(dp(10),0,dp(6),0);
        label.setBackgroundColor(0xffe3edf5);
        items.addView(label,new LinearLayout.LayoutParams(-1,dp(23)));
    }
    void checked(String title,boolean checked,Runnable action){item(title,checked?"checked":"empty",action);}
    void submenu(String title,String icon,Runnable action){item(title,icon,action);LinearLayout row=(LinearLayout)items.getChildAt(items.getChildCount()-1);TextView arrow=Aero.text(context,"›",18,0xff476277);row.addView(arrow,new LinearLayout.LayoutParams(dp(20),-1));}
    void separator(){LinearLayout row=new LinearLayout(context);row.setPadding(dp(34),dp(3),dp(5),dp(3));View line=new View(context);line.setBackgroundColor(0xffc6cbcf);row.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));items.addView(row,new LinearLayout.LayoutParams(-1,dp(7)));}
    void show(View root,float screenX,float screenY,int leftInset,int topInset,int rightInset,int bottomInset){
        if(owner!=null){owner.removeOnAttachStateChangeListener(lifetime);owner.removeOnLayoutChangeListener(bounds);}owner=root;root.addOnAttachStateChangeListener(lifetime);root.addOnLayoutChangeListener(bounds);int[] location=new int[2];root.getLocationOnScreen(location);int left=location[0]+leftInset+dp(3),top=location[1]+topInset+dp(3),right=location[0]+root.getWidth()-rightInset-dp(3),bottom=location[1]+root.getHeight()-bottomInset-dp(3);
        int width=Math.min(dp(menuWidth),Math.max(1,right-left));popup.getContentView().measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.max(1,bottom-top),View.MeasureSpec.AT_MOST));int height=Math.min(popup.getContentView().getMeasuredHeight(),Math.max(1,bottom-top));popup.setWidth(width);popup.setHeight(height);
        int x=Math.max(left,Math.min(Math.round(screenX),right-width)),y=Math.max(top,Math.min(Math.round(screenY),bottom-height));popup.showAtLocation(root,Gravity.TOP|Gravity.LEFT,x,y);
    }
    boolean showing(){return popup.isShowing();}
    void dismiss(){popup.dismiss();}
    private int dp(int value){return Aero.dp(context,value);}
}
