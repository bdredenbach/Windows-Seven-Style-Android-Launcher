package com.nthexperience.desktop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ordered folder references. The first location is the default destination. */
final class LibraryLocations {
    static final String KEY="seven_libraries";
    static final String[] KINDS={"documents","pictures","music","videos"};
    static final int LIMIT=8;

    static boolean known(String kind){return Arrays.asList(KINDS).contains(kind);}

    private static JSONObject read(DesktopStore store){
        try{return new JSONObject(store.prefs.getString(KEY,"{}"));}
        catch(JSONException invalid){return new JSONObject();}
    }

    static List<String> locations(DesktopStore store,String kind){
        List<String> result=new ArrayList<>();
        if(!known(kind))return result;
        JSONArray saved=read(store).optJSONArray(kind);
        if(saved==null){
            if(StorageAccess.has(store.context))result.add(StorageAccess.page(kind));
            return result;
        }
        for(int i=0;i<Math.min(saved.length(),LIMIT);i++){
            String page=saved.optString(i);
            try{FolderLinks.validatePage(page);result.add(page);}
            catch(JSONException invalid){}
        }
        return result;
    }

    static void save(DesktopStore store,String kind,List<String> pages)throws JSONException{
        JSONObject all=read(store).put(kind,new JSONArray(pages));
        validate(all);
        store.prefs.edit().putString(KEY,all.toString()).apply();
    }

    static void validate(JSONObject data)throws JSONException{
        if(data.length()>KINDS.length)throw new JSONException("Too many libraries");
        for(java.util.Iterator<String> keys=data.keys();keys.hasNext();){
            String kind=keys.next();
            if(!known(kind))throw new JSONException("Unknown library");
            JSONArray pages=data.getJSONArray(kind);
            if(pages.length()>LIMIT)throw new JSONException("A library supports up to eight locations.");
            Set<String> distinct=new HashSet<>();
            for(int i=0;i<pages.length();i++){
                String page=pages.getString(i);
                FolderLinks.validatePage(page);
                if(!distinct.add(DocumentOps.current(page).toString()))throw new JSONException("Folder is already included.");
            }
        }
    }
}
