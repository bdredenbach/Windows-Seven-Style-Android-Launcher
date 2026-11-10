package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Non-interactive App info illustration using this installed launcher's identity. */
final class UsageAccessExample extends LinearLayout {
    UsageAccessExample(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(0xff191c19);
        Aero.pad(this,16);
        // This illustrates Android Settings, so launcher accents must not recolor it.
        setContentDescription("Android App info menu showing Allow restricted settings");
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        TextView caption=label("Example",11,0xffc2c8c2);
        addView(caption);

        LinearLayout header=new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0,Aero.dp(context,8),0,Aero.dp(context,8));
        TextView title=label("\u2190   App info",20,0xfff1f4ed);
        header.addView(title,new LayoutParams(0,-2,1));
        TextView menu=label("\u22ee",26,0xfff1f4ed);
        menu.setGravity(Gravity.CENTER);
        GradientDrawable menuBackground=new GradientDrawable();
        menuBackground.setColor(0xff2b302a);
        menuBackground.setCornerRadius(Aero.dp(context,24));
        menu.setBackground(menuBackground);
        header.addView(menu,new LayoutParams(Aero.dp(context,44),Aero.dp(context,44)));
        addView(header,new LayoutParams(-1,-2));

        TextView command=label("Allow restricted settings",14,0xfff1f4ed);
        command.setGravity(Gravity.END);
        command.setPadding(0,Aero.dp(context,6),0,Aero.dp(context,14));
        addView(command,new LayoutParams(-1,-2));

        ImageView icon=new ImageView(context);
        icon.setImageDrawable(context.getApplicationInfo().loadIcon(context.getPackageManager()));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams iconBounds=new LayoutParams(Aero.dp(context,76),Aero.dp(context,76));
        iconBounds.gravity=Gravity.CENTER_HORIZONTAL;
        addView(icon,iconBounds);

        TextView app=label(context.getString(R.string.app_name),19,0xfff1f4ed);
        app.setGravity(Gravity.CENTER);
        app.setPadding(0,Aero.dp(context,14),0,Aero.dp(context,8));
        addView(app,new LayoutParams(-1,-2));
    }

    private TextView label(String text,int size,int color) {
        TextView view=Aero.text(getContext(),text,size,color);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }
}
