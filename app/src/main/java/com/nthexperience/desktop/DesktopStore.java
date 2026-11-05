package com.nthexperience.desktop;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import org.json.JSONObject;

final class DesktopStore {
    static final int MAX_FOLDERS=64, MAX_ITEMS=256, MAX_QUICK=12;
    static final String TASKBAR_USAGE="taskbar_usage";
    final SharedPreferences prefs,usagePrefs;
    final Context context;
    DesktopStore(Context context) { this.context=context.getApplicationContext();prefs = context.getSharedPreferences("desktop", Context.MODE_PRIVATE);usagePrefs=context.getSharedPreferences("usage_access",Context.MODE_PRIVATE); }
    List<String> desktop() {
        return list("shortcuts", Arrays.asList("@computer", "@browser", "@notes", "@appearance"));
    }
    void migrateDesktop(){
        // Keep this device-local choice across desktop backup restores and existing installs.
        if(prefs.contains(TASKBAR_USAGE)){
            if(usagePrefs.contains(TASKBAR_USAGE)
                    ||usagePrefs.edit().putBoolean(TASKBAR_USAGE,prefs.getBoolean(TASKBAR_USAGE,false)).commit()){
                prefs.edit().remove(TASKBAR_USAGE).apply();
            }
        }
        if(!prefs.getBoolean("desktop_recycle_added",false)){List<String> icons=desktop();if(!icons.contains("@recycle"))icons.add("@recycle");desktop(icons);prefs.edit().putBoolean("desktop_recycle_added",true).apply();}
        if(prefs.getBoolean("desktop_v41",false))return;
        List<String> shortcuts=desktop();shortcuts.remove("@files");shortcuts.remove("@filemanagers");desktop(shortcuts);prefs.edit().putBoolean("desktop_v41",true).apply();
    }
    List<String> gadgets(){return list("gadgets",Arrays.asList("clock","calendar","weather"));}
    void gadgets(List<String> ids){save("gadgets",ids);}
    List<String> fileManagers(){return list("file_manager_apps",new ArrayList<>());}
    void fileManagers(List<String> ids){save("file_manager_apps",ids);}
    List<String> photos(){return list("gadget_photos",new ArrayList<>());}
    void photos(List<String> ids){save("gadget_photos",ids);}
    List<String> startPins(){return list("start_pins",Arrays.asList("@browser","@notes"));}
    void startPins(List<String> ids){save("start_pins",ids);}
    List<String> recents() { return list("recents", new ArrayList<>()); }
    List<String> taskbarOrder(){return list(TaskbarOrder.KEY,Collections.emptyList());}
    void taskbarOrder(List<String> ids){save(TaskbarOrder.KEY,TaskbarOrder.clean(ids));}
    List<String> quickLaunch(){return list("quick_launch",Arrays.asList("@browser","@files",MediaLibrary.PLAYER));}
    void quickLaunch(List<String> ids){prefs.edit().remove(TaskbarOrder.KEY).apply();save("quick_launch",ids.subList(0,Math.min(MAX_QUICK,ids.size())));}
    JSONObject folders(){return object("app_folders");}
    JSONObject folder(String id){return folders().optJSONObject(id);}
    boolean isFolder(String id){return id!=null&&id.startsWith("folder:")&&folder(id)!=null;}
    List<String> folderItems(String id){JSONObject folder=folder(id);return array(folder==null?null:folder.optJSONArray("items"));}
    String name(String id,String fallback){JSONObject folder=folder(id);if(folder!=null)return folder.optString("name","Folder");return object("shortcut_names").optString(id,fallback);}
    void rename(String id,String name){
        name=cleanName(name);if(name.isEmpty())throw new IllegalArgumentException("Enter a name");
        JSONObject all=folders(),folder=all.optJSONObject(id);
        if(folder!=null){GadgetLayout.put(folder,"name",name);prefs.edit().putString("app_folders",all.toString()).apply();}
        else{JSONObject names=object("shortcut_names");GadgetLayout.put(names,id,name);prefs.edit().putString("shortcut_names",names.toString()).apply();}
    }
    void originalName(String id){JSONObject names=object("shortcut_names");names.remove(id);prefs.edit().putString("shortcut_names",names.toString()).apply();}
    String createFolder(String name,List<String> members){
        JSONObject all=folders();if(all.length()>=MAX_FOLDERS)throw new IllegalArgumentException("The desktop already has 64 folders");
        for(String member:members)if(member.startsWith("folder:"))throw new IllegalArgumentException("Folders cannot contain other folders");
        String title=cleanName(name);if(title.isEmpty())throw new IllegalArgumentException("Enter a folder name");String id="folder:"+UUID.randomUUID();
        JSONObject folder=new JSONObject();GadgetLayout.put(folder,"name",title);GadgetLayout.put(folder,"items",new JSONArray(leafItems(members)));GadgetLayout.put(all,id,folder);
        List<String> desktop=desktop();int position=desktop.size();for(String member:members){int at=desktop.indexOf(member);if(at>=0)position=Math.min(position,at);}desktop.removeAll(members);desktop.add(Math.min(position,desktop.size()),id);
        prefs.edit().putString("app_folders",all.toString()).putString("shortcuts",new JSONArray(desktop).toString()).apply();return id;
    }
    void folderItems(String id,List<String> items){JSONObject all=folders(),folder=all.optJSONObject(id);if(folder==null)return;GadgetLayout.put(folder,"items",new JSONArray(leafItems(items)));prefs.edit().putString("app_folders",all.toString()).apply();}
    String drop(String moving,String target,boolean group){
        if(moving.equals(target))return null;List<String> ids=desktop();if(!ids.contains(moving)||!ids.contains(target))return null;
        if(group&&!isFolder(moving)){
            if(isFolder(target)){List<String> members=folderItems(target);if(!members.contains(moving)){if(members.size()>=MAX_ITEMS)throw new IllegalArgumentException("This folder is full");members.add(moving);}JSONObject all=folders();GadgetLayout.put(all.optJSONObject(target),"items",new JSONArray(members));ids.remove(moving);prefs.edit().putString("app_folders",all.toString()).putString("shortcuts",new JSONArray(ids).toString()).apply();return target;}
            return createFolder("New folder",Arrays.asList(target,moving));
        }
        desktop(LauncherState.move(ids,moving,target));return null;
    }
    void dissolve(String id){
        JSONObject all=folders();if(!all.has(id))return;List<String> members=folderItems(id),ids=desktop();int position=ids.indexOf(id);ids.remove(id);if(position<0)position=ids.size();for(String member:members)if(!ids.contains(member))ids.add(Math.min(position++,ids.size()),member);all.remove(id);
        List<String> quick=quickLaunch();quick.remove(id);JSONObject names=object("shortcut_names");names.remove(id);
        prefs.edit().putString("app_folders",all.toString()).putString("shortcuts",new JSONArray(ids).toString()).putString("quick_launch",new JSONArray(quick).toString()).putString("shortcut_names",names.toString()).apply();
    }
    JSONObject folderPositions(String id) {
        JSONObject positions = new JSONObject();
        GadgetLayout.put(positions, "shortcuts", desktop().indexOf(id));
        GadgetLayout.put(positions, "quick_launch", quickLaunch().indexOf(id));
        GadgetLayout.put(positions, "start_pins", startPins().indexOf(id));
        return positions;
    }
    void setRecycledFolder(String id, JSONObject folder, JSONObject positions) throws java.io.IOException {
        JSONObject all = folders();
        if (folder == null) all.remove(id);
        else {
            if (!all.has(id) && all.length() >= MAX_FOLDERS)
                throw new java.io.IOException("The desktop already has 64 folders.");
            GadgetLayout.put(all, id, folder);
        }
        SharedPreferences.Editor edit = prefs.edit().putString("app_folders", all.toString());
        folderMembership(edit, "shortcuts", desktop(), id, positions, 512);
        folderMembership(edit, "quick_launch", quickLaunch(), id, positions, MAX_QUICK);
        folderMembership(edit, "start_pins", startPins(), id, positions, 32);
        if (!edit.commit()) throw new java.io.IOException("The folder change could not be saved. Recovery data was kept.");
    }
    private void folderMembership(SharedPreferences.Editor edit, String key, List<String> ids,
                                  String id, JSONObject positions, int limit) throws java.io.IOException {
        if (positions == null) ids.remove(id);
        else {
            int position = positions.optInt(key, -1);
            if (position >= 0 && !ids.contains(id)) {
                if (ids.size() >= limit) throw new java.io.IOException("The original folder location is full.");
                ids.add(Math.min(position, ids.size()), id);
            }
        }
        edit.putString(key, new JSONArray(ids).toString());
    }
    void shift(List<String> ids,String id,int direction){int at=ids.indexOf(id),to=at+direction;if(at>=0&&to>=0&&to<ids.size())Collections.swap(ids,at,to);}
    int iconPercent(){int old=prefs.getInt("desktop_icon_size",1);return Math.max(60,Math.min(150,prefs.getInt("desktop_icon_percent",old==0?83:old==2?126:100)));}
    int iconSize(){return Math.round(46*iconPercent()/100f);}
    boolean iconLabels(){return prefs.getBoolean("desktop_labels",true);}
    int labelSize(){return Math.max(10,Math.min(16,prefs.getInt("desktop_label_size",iconSize()>46?12:11)));}
    int spacingX(){return Math.max(0,Math.min(24,prefs.getInt("desktop_spacing_x",0)));}
    int spacingY(){return Math.max(0,Math.min(24,prefs.getInt("desktop_spacing_y",0)));}
    int tileWidth(){return iconSize()+34+spacingX();}
    int tileHeight(){return iconSize()+(iconLabels()?48:24)+spacingY();}
    int gridColumns(String orientation){return Math.max(0,Math.min(10,prefs.getInt("desktop_columns_"+orientation,0)));}
    int gridRows(String orientation){return Math.max(0,Math.min(16,prefs.getInt("desktop_rows_"+orientation,0)));}
    static String cleanName(String name){String value=name==null?"":name.replaceAll("[\\p{Cntrl}]"," ").trim();return value.substring(0,Math.min(40,value.length()));}
    static List<String> array(JSONArray values){List<String> result=new ArrayList<>();if(values!=null)for(int i=0;i<values.length();i++){Object item=values.opt(i);if(item instanceof String)result.add((String)item);}return LauncherState.normalize(result);}
    private List<String> leafItems(List<String> items){List<String> result=new ArrayList<>();for(String id:LauncherState.normalize(items))if(!id.startsWith("folder:")&&result.size()<MAX_ITEMS)result.add(id);return result;}
    private JSONObject object(String key){try{return new JSONObject(prefs.getString(key,"{}"));}catch(JSONException|ClassCastException ex){return new JSONObject();}}
    private List<String> list(String key, List<String> fallback) {
        try {
            String saved = prefs.getString(key, null);
            if (saved == null) return new ArrayList<>(fallback);
            JSONArray array = new JSONArray(saved);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) result.add(array.getString(i));
            return LauncherState.normalize(result);
        } catch (JSONException | ClassCastException ex) { return new ArrayList<>(fallback); }
    }
    void desktop(List<String> ids) { save("shortcuts", ids); }
    void launched(String id) { save("recents", LauncherState.recent(recents(), id)); }
    private void save(String key, List<String> ids) {
        prefs.edit().putString(key, new JSONArray(LauncherState.normalize(ids)).toString()).apply();
    }
    boolean pinned(String id) { return desktop().contains(id); }
    void toggle(String id) {
        List<String> values = desktop();
        if (values.contains(id)) values.remove(id); else values.add(id);
        desktop(values);
    }
}
