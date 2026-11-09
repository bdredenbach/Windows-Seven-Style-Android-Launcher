package com.nthexperience.desktop;

import android.content.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Shortcut editing plus a separate, explicit Android app-uninstall command. */
final class ShortcutMenus {
    interface Host {void open(String id);void rename(String id);void changed();}
    static void show(android.content.Context context,View anchor,float x,float y,DesktopStore store,String id,String source,String name,Host host){
        DesktopContextMenu menu=new DesktopContextMenu(context,"Shortcut context menu");menu.item("Open","programs",()->host.open(id));menu.separator();
        boolean inSource=ExplorerClipboard.members(store,source).contains(id);
        menu.item("Cut","cut",inSource,()->{ExplorerClipboard.shortcuts(Collections.singletonList(id),source,true);host.changed();});
        menu.item("Copy","copy",()->{ExplorerClipboard.shortcuts(Collections.singletonList(id),source,false);host.changed();});
        String destination=store.isFolder(id)?"contents:"+id:source;menu.item("Paste","paste",!ExplorerClipboard.shortcuts.isEmpty(),()->paste(context,store,destination,host));menu.separator();
        menu.item(store.isFolder(id)?"Delete folder":"Delete shortcut","delete",inSource&&!id.equals("@recycle"),()->{RecycleBin.trashShortcut(context,store,source,id,name);host.changed();});
        menu.item("Rename shortcut…","notes",()->host.rename(id));menu.separator();
        menu.item(store.startPins().contains(id)?"Unpin from Start Menu":"Pin to Start Menu","programs",()->{List<String> pins=store.startPins();if(pins.contains(id))pins.remove(id);else if(pins.size()<32)pins.add(id);store.startPins(pins);host.changed();});
        menu.item(store.pinned(id)?"Remove from desktop":"Add to desktop","computer",()->{if(store.pinned(id)&&!id.equals("@recycle"))RecycleBin.trashShortcut(context,store,"desktop",id,name);else store.toggle(id);host.changed();});
        boolean labels=store.prefs.getBoolean("taskbar_show_labels",false);
        menu.item(labels?"Add to Quick Launch":"Pin to taskbar","programs",!store.quickLaunch().contains(id)&&store.quickLaunch().size()<DesktopStore.MAX_QUICK,()->{List<String> quick=store.quickLaunch();quick.add(id);store.quickLaunch(quick);host.changed();});
        if(source.equals("quick"))menu.item(labels?"Customize Quick Launch…":"Customize pinned apps…","control",()->host.open("@quicklaunch"));
        if(!store.isFolder(id))menu.item("Add to folder  ›","folderapps",()->{DesktopContextMenu folders=new DesktopContextMenu(context,"Choose app folder");java.util.Iterator<String> keys=store.folders().keys();while(keys.hasNext()){String folder=keys.next();folders.item(store.name(folder,"Folder"),"folderapps",()->{List<String> ids=store.folderItems(folder);if(!ids.contains(id)&&ids.size()<DesktopStore.MAX_ITEMS){ids.add(id);store.folderItems(folder,ids);host.changed();}});}folders.item("New folder…","folderapps",()->host.open("@organize"));ContextMenus.show(folders,anchor,x,y);});
        AppUninstaller.add(menu,context,id);
        ComponentName component=ComponentName.unflattenFromString(id);if(component!=null)menu.item("App info","control",()->{try{context.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+component.getPackageName())));}catch(ActivityNotFoundException ignored){}});
        ContextMenus.show(menu,anchor,x,y);
    }
    static void paste(Context context,DesktopStore store,String target,Host host){try{ExplorerClipboard.paste(store,target);host.changed();}catch(IllegalArgumentException error){Toast.makeText(context,error.getMessage(),Toast.LENGTH_LONG).show();}}
}
