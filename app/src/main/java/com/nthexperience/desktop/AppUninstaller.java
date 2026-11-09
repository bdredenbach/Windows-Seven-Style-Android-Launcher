package com.nthexperience.desktop;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.view.Menu;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Explicit Android uninstall confirmation, independent of removing launcher shortcuts. */
final class AppUninstaller {
    static final int REQUEST=626,MENU_ID=0x01020064;
    private static final String PREFS="launcher_uninstall";
    static String packageName(String id){ComponentName component=id==null?null:ComponentName.unflattenFromString(id);return component==null?null:component.getPackageName();}
    static boolean available(Context context,String id){String pkg=packageName(id);if(pkg==null||pkg.equals(context.getPackageName()))return false;try{ApplicationInfo app=context.getPackageManager().getApplicationInfo(pkg,0);return(app.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0;}catch(PackageManager.NameNotFoundException unavailable){return false;}}
    static void add(DesktopContextMenu menu,Context context,String id){if(available(context,id))menu.item("Uninstall","delete",()->request(context,id));}
    static void add(PopupMenu menu,Context context,String id){if(available(context,id))menu.getMenu().add(Menu.NONE,MENU_ID,Menu.NONE,"Uninstall").setOnMenuItemClickListener(item->{request(context,id);return true;});}
    static boolean request(Context context,String id){
        if(!available(context,id)){Toast.makeText(context,"This app cannot be uninstalled from here.",Toast.LENGTH_LONG).show();return false;}
        String pkg=packageName(id);SharedPreferences pending=context.getSharedPreferences(PREFS,0);pending.edit().putString("package",pkg).commit();Intent intent=new Intent(Intent.ACTION_DELETE,Uri.fromParts("package",pkg,null));Context owner=context;while(owner instanceof ContextWrapper&&!(owner instanceof Activity)){Context base=((ContextWrapper)owner).getBaseContext();if(base==owner)break;owner=base;}
        try{if(owner instanceof Activity)((Activity)owner).startActivityForResult(intent,REQUEST);else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;}catch(ActivityNotFoundException|SecurityException unavailable){pending.edit().remove("package").apply();Toast.makeText(context,"Android could not open uninstall confirmation. Your app and shortcuts have been kept.",Toast.LENGTH_LONG).show();return false;}
    }
    static String returned(Context context,DesktopStore store){SharedPreferences pending=context.getSharedPreferences(PREFS,0);String pkg=pending.getString("package",null);pending.edit().remove("package").apply();if(pkg==null)return null;try{context.getPackageManager().getApplicationInfo(pkg,0);return null;}catch(PackageManager.NameNotFoundException removed){removed(store,pkg);return pkg;}}
    static String removedPackage(Intent intent){return Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())&&!intent.getBooleanExtra(Intent.EXTRA_REPLACING,false)&&intent.getData()!=null?intent.getData().getSchemeSpecificPart():null;}
    static void removed(DesktopStore store,String pkg){
        if(pkg==null||pkg.isEmpty())return;List<String> pins=store.startPins();if(pins.removeIf(id->pkg.equals(packageName(id))))store.startPins(pins);
        // Eight has independent tile banks for 8 and 8.1. Keep group names and all other tiles.
        for(String key:new String[]{"eight_start_8","eight_start_81"}){String raw=store.prefs.getString(key,null);if(raw==null)continue;try{JSONObject root=new JSONObject(raw);JSONArray groups=root.getJSONArray("groups");boolean changed=false;for(int n=0;n<groups.length();n++){JSONObject group=groups.getJSONObject(n);JSONArray items=group.getJSONArray("items"),kept=new JSONArray();for(int i=0;i<items.length();i++){JSONObject tile=items.getJSONObject(i);if(pkg.equals(packageName(tile.getString("id"))))changed=true;else kept.put(tile);}group.put("items",kept);}if(changed)store.prefs.edit().putString(key,root.toString()).apply();}catch(JSONException invalid){/* Keep an unreadable bank intact rather than replacing a saved layout. */}}
    }
    private AppUninstaller(){}
}
