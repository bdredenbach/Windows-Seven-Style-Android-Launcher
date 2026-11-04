package com.nthexperience.desktop;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

final class SevenThemes extends GridLayout {
    private static final Theme[] THEMES={
            new Theme("Seven Blue",AeroWallpaper.SEVEN,0,true),
            new Theme("Twilight",1,4,true),
            new Theme("Meadow",2,3,true),
            new Theme("Seven Basic",AeroWallpaper.SEVEN,0,false)
    };

    SevenThemes(Context context,DesktopStore store,int columns,Runnable applied){
        super(context);
        setColumnCount(columns);
        setContentDescription("Desktop themes");
        for(Theme theme:THEMES){
            boolean selected=theme.matches(store);
            LinearLayout card=Aero.column(context);
            Aero.pad(card,5);
            card.setContentDescription("Apply theme: "+theme.name);
            card.setFocusable(true);
            card.setSelected(selected);
            card.setBackground(selected?Aero.panel(context,0xffe3f3ff,0xffcfe5f6,3,0xff72a8ce):Aero.selection(context));

            FrameLayout preview=new FrameLayout(context);
            preview.addView(new AeroWallpaper(context,theme.wallpaper,false),new FrameLayout.LayoutParams(-1,-1));
            View window=new View(context);
            int tint=theme.glass?AeroGlass.TINTS[theme.color]:0xffa5aeb7;
            window.setBackground(Aero.gloss(context,new int[]{0xfff4faff,tint,0xffd9e8f0},3,0xfff6fbff));
            FrameLayout.LayoutParams sample=new FrameLayout.LayoutParams(Aero.dp(context,39),Aero.dp(context,23),Gravity.BOTTOM|Gravity.START);
            sample.setMargins(Aero.dp(context,7),0,0,Aero.dp(context,7));
            preview.addView(window,sample);
            card.addView(preview,new LinearLayout.LayoutParams(-1,Aero.dp(context,75)));
            TextView caption=Aero.text(context,theme.name,12,0xff234764);
            caption.setGravity(Gravity.CENTER);
            card.addView(caption,new LinearLayout.LayoutParams(-1,Aero.dp(context,28)));
            card.setOnClickListener(v->{theme.apply(store);applied.run();});

            LayoutParams cell=new LayoutParams();
            cell.columnSpec=spec(UNDEFINED,1f);
            cell.width=0;
            cell.setMargins(0,0,Aero.dp(context,6),Aero.dp(context,7));
            addView(card,cell);
        }
    }

    private static final class Theme {
        final String name;
        final int wallpaper,color;
        final boolean glass;

        Theme(String name,int wallpaper,int color,boolean glass){
            this.name=name;this.wallpaper=wallpaper;this.color=color;this.glass=glass;
        }

        boolean matches(DesktopStore store){
            return store.prefs.getString("wallpaper_photo","").isEmpty()
                    &&store.prefs.getInt("wallpaper",AeroWallpaper.SEVEN)==wallpaper
                    &&store.prefs.getInt("aero_color",0)==color
                    &&store.prefs.getBoolean("aero_transparency",true)==glass;
        }

        void apply(DesktopStore store){
            store.prefs.edit().putInt("wallpaper",wallpaper).remove("wallpaper_photo")
                    .putInt("aero_color",color).putBoolean("aero_transparency",glass).apply();
        }
    }
}
