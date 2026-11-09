package com.nthexperience.desktop;

import android.content.Context;
import android.widget.*;
import java.util.List;

/** Live phone notifications for classic tray surfaces; notification contents remain in memory. */
final class PhoneNotifications extends LinearLayout {
    private final Runnable refresh=this::render;
    PhoneNotifications(Context context){super(context);setOrientation(VERTICAL);setContentDescription("Phone notifications");render();}
    private void render(){
        removeAllViews();TextView heading=Aero.text(getContext(),"Phone notifications",16,Aero.INK);addView(heading);
        if(!TenNotifications.allowed(getContext())){addView(Aero.text(getContext(),"Allow Notification access to show your phone's notifications here.",13,Aero.INK));addView(Aero.button(getContext(),"Allow notification access",()->TenNotifications.request(getContext())));return;}
        List<TenNotifications.Row> rows=TenNotifications.rows(getContext());if(rows.isEmpty())addView(Aero.text(getContext(),"No phone notifications.",13,Aero.INK));
        for(TenNotifications.Row row:rows){LinearLayout item=Aero.column(getContext());Aero.pad(item,6);item.addView(Aero.text(getContext(),row.app+"\n"+row.title+"\n"+row.text,13,Aero.INK));item.setContentDescription(row.app+": "+row.title);item.setOnClickListener(v->TenNotifications.open(row));if(row.dismissible)item.addView(Aero.button(getContext(),"Dismiss",()->TenNotifications.dismiss(row.key)));addView(item);}
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();TenNotifications.observe(refresh);render();}
    @Override protected void onDetachedFromWindow(){TenNotifications.unobserve(refresh);super.onDetachedFromWindow();}
}
