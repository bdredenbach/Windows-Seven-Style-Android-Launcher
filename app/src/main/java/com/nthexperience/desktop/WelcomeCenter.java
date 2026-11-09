package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.widget.*;

/** A compact first-run starting point; every tile opens an existing launcher function. */
final class WelcomeCenter extends VistaShell {
    WelcomeCenter(Context c,DesktopStore store,Actions actions){super(c,store,actions,"Welcome Center");search.setVisibility(GONE);start("welcome");}
    @Override void render(){side.removeAllViews();sideLink("Personalization",()->actions.open("@appearance"));sideLink("Computer",()->actions.open("@computer"));sideLink("Backup and Restore",()->actions.open("@backup"));address.setText("Welcome Center");LinearLayout column=column();
        LinearLayout brand=new LinearLayout(getContext());brand.setGravity(android.view.Gravity.CENTER_VERTICAL);brand.addView(new AeroIcon(getContext(),"orb"),new LayoutParams(dp(58),dp(58)));TextView title=Aero.text(getContext(),"Welcome to "+store.prefs.getString("desktop_name",getContext().getString(R.string.app_name)),20,0xff234f6d);title.setPadding(dp(12),0,0,0);brand.addView(title,new LayoutParams(0,-2,1));column.addView(brand,new LayoutParams(-1,dp(88)));
        heading(column,"Make yourself at home","Your apps, your gadgets, your desktop.");task(column,"files","Set up file browsing",StorageAccess.has(getContext())?"Device folders are ready in Nth Explorer":"Choose device access or connect individual folders",()->actions.action("storage-access"));task(column,"appearance","Personalize your desktop","Choose wallpaper, glass color and startup behavior",()->actions.open("@appearance"));task(column,"folderapps","Arrange apps and folders","Rename shortcuts and choose your Quick Launch icons",()->actions.open("@organize"));task(column,"clock","Choose desktop gadgets","Add clocks, weather, notes and a photo frame",()->actions.open("@gadgets"));task(column,"drive","Restore a desktop","Open a desktop backup and review what can be restored",()->actions.open("@backup"));
        if(!((MainActivity)getContext()).isHome())task(column,"computer","Make Nth Desktop your home","Choose it as Android’s default home app",()->actions.action("home"));
        heading(column,"A few familiar gestures","Hold blank desktop space for its menu. Drag a shortcut onto another to make a folder. Hold a gadget to move it. Use the taskbar buttons to restore your Nth windows. Hold the taskbar grip for Switch Windows, Flip 3D, Task Manager and window arrangements; hold a caption for window commands. The sun icon adjusts brightness. On a phone, the up chevron opens battery, network and clock controls.");CheckBox show=Aero.check(getContext(),"Open Welcome Center when Nth Desktop starts",store.prefs.getBoolean("welcome_on_start",false));show.setOnCheckedChangeListener((v,checked)->store.prefs.edit().putBoolean("welcome_on_start",checked).apply());column.addView(show);status.setText("Welcome Center  •  Nth Experience");body(scrolling(column));}
    @Override void refreshApps(){render();}
    @Override void showFolders(){actions.open("@control");}
    @Override void goUp(){actions.open("@control");}
}
