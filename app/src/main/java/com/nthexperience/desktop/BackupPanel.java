package com.nthexperience.desktop;

import android.content.Context;
import android.view.View;
import android.widget.*;

/** Backup actions and a review step that never changes the desktop just by opening a file. */
final class BackupPanel extends VistaShell {
    interface Host {DesktopBackup.Plan pending();String message();boolean busy();void exportBackup();void importBackup();void restoreBackup(boolean undo);void discardPreview();}
    private final Host host;
    BackupPanel(Context c,DesktopStore store,Actions actions,Host host){super(c,store,actions,"Backup and Restore");this.host=host;search.setVisibility(GONE);start("home");}
    @Override void render(){
        side.removeAllViews();sideLink("Backup and Restore",()->navigate("home"));sideLink("Desktop Organization",()->actions.open("@organize"));sideLink("Control Panel",()->actions.open("@control"));address.setText("Control Panel  ›  Backup and Restore");LinearLayout column=column();DesktopBackup.Plan plan=host.pending();
        if(page().equals("preview")&&plan!=null){
            heading(column,"Review your desktop backup","Created: "+plan.created);details(column,plan);
            heading(column,"Restore this setup?","Compatible shortcuts, notes and settings will be restored. Skipped items keep this desktop’s current settings. A copy of your current setup is kept for Undo last restore.");button(column,"Restore this backup",()->host.restoreBackup(false));button(column,"Cancel restore",()->{host.discardPreview();navigate("home");});
        }else{
            heading(column,"Restore a desktop","Keep your folders, shortcut names, Quick Launch, notes, settings and both portrait and landscape layouts.");
            task(column,"folderapps","Restore a desktop","Choose a backup, then review it before restoring",host::importBackup);
            task(column,"drive","Save a backup","Choose where to save a desktop backup file",host::exportBackup);
            task(column,"files","Fix linked folders","Reconnect folders from a restored backup",()->actions.action("repair-folders"));
            if(plan!=null)task(column,"computer","Review selected backup","Continue reviewing the file you opened",()->navigate("preview"));
            if(DesktopBackup.file(getContext(),DesktopBackup.UNDO).exists())task(column,"refresh","Undo last restore","Recover the setup kept before your most recent restore",()->navigate("undo"));
            if(page().equals("undo")){heading(column,"Undo the last restore?","The setup kept before that restore will replace your current desktop and notes.");button(column,"Undo last restore",()->host.restoreBackup(true));link(column,"Keep current desktop",()->navigate("home"));}
            heading(column,"What is included?","App folders, shortcuts, renamed icons, Quick Launch, Notepad and gadget notes, your profile picture, linked file names, gadget choices, both layouts, wallpaper choices and launcher preferences.");
            heading(column,"Files and photos","Backups keep links to your files and photos. Their contents remain in the original storage. On another phone, choose those folders and photos again to allow access.");
        }
        if(!host.message().isEmpty()){TextView message=Aero.text(getContext(),host.message(),12,0xff355b73);message.setPadding(0,dp(12),0,dp(12));message.setContentDescription("Backup status");column.addView(message);}
        status.setText(host.busy()?"Working…":"Nth Desktop backup  •  review before restore");setEnabled(!host.busy());body(scrolling(column));
    }
    private void button(LinearLayout column,String label,Runnable action){TextView button=Aero.button(getContext(),label,action);button.setContentDescription(label);button.setEnabled(!host.busy());LayoutParams lp=new LayoutParams(-1,dp(46));lp.bottomMargin=dp(8);column.addView(button,lp);}
    private void details(LinearLayout column,DesktopBackup.Plan plan){
        String summary=plan.count("shortcuts")+" desktop shortcuts\n"+plan.folders()+" app folders\n"+plan.count("quick_launch")+" Quick Launch shortcuts\n"+plan.count("gadgets")+" gadgets\nNotepad: "+plan.string("note","").length()+" characters\nGadget note: "+plan.string("gadget_note","").length()+" characters\nPortrait and landscape layouts";
        TextView list=Aero.text(getContext(),summary,13,Aero.INK);list.setPadding(0,dp(8),0,dp(14));list.setContentDescription("Backup preview summary");column.addView(list);int relinks=plan.relinks(getContext());if(relinks>0)heading(column,"File and folder access",relinks+" file or folder link"+(relinks==1?" needs":"s need")+" to be chosen again on this device. Setup will help reconnect folders after restoring. Use Fix linked folders to retry later; choose unavailable files again in Computer.");if(plan.count("gadget_photos")>0)heading(column,"Photo access","Photo links are kept. If a photo is unavailable, choose it again in Photo Frame settings.");if(plan.skipped>0)heading(column,"Items skipped",plan.skipped+" item"+(plan.skipped==1?" was":"s were")+" skipped. Compatible items can still be restored.\n"+skippedSummary(plan)+(plan.skipped>12?"\n… and "+(plan.skipped-12)+" more":""));
    }
    private String skippedSummary(DesktopBackup.Plan plan){
        java.util.List<String> labels=new java.util.ArrayList<>();
        for(String item:plan.skippedItems.subList(0,Math.min(12,plan.skippedItems.size()))){
            String label=item.replace("quick_launch","Quick Launch").replace("wallpaper_slides","Background slideshow").replace("wallpaper","Background").replace("seven_saved_themes","Saved themes").replace("shortcuts","Desktop shortcuts").replace("app_folders","App folders").replace("shortcut_names","Shortcut names").replace("ten_desktop_items","Desktop layouts");
            labels.add(label.length()>180?label.substring(0,180)+"…":label);
        }
        return android.text.TextUtils.join("\n",labels);
    }
    void preview(){navigate("preview");render();}
    @Override void refreshApps(){render();}
    @Override void showFolders(){navigate("home");}
    @Override void goUp(){navigate("home");}
}
