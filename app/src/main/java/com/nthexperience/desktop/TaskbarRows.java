package com.nthexperience.desktop;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Explicit row choices survive running/idle transitions and portable backups. */
final class TaskbarRows {
    static final String KEY="seven_taskbar_rows";
    static Map<String,Integer> read(SharedPreferences prefs){try{return parse(prefs.getString(KEY,"{}"));}catch(JSONException ignored){return Collections.emptyMap();}}
    static Map<String,Integer> parse(String raw)throws JSONException{
        JSONObject object=new JSONObject(raw);if(object.length()>TaskbarOrder.LIMIT)throw new JSONException("Too many taskbar row choices");
        Map<String,Integer> rows=new LinkedHashMap<>();Iterator<String> keys=object.keys();
        while(keys.hasNext()){String id=keys.next();Object row=object.get(id);if(!TaskbarOrder.valid(id)||!(row instanceof Integer)||((int)row!=0&&(int)row!=1))throw new JSONException("Invalid taskbar row");rows.put(id,(int)row);}return rows;
    }
    static void save(SharedPreferences prefs,String id,int row){
        if(!TaskbarOrder.valid(id)||row<0||row>1)return;
        Map<String,Integer> rows=new LinkedHashMap<>(read(prefs));rows.remove(id);while(rows.size()>=TaskbarOrder.LIMIT)rows.remove(rows.keySet().iterator().next());rows.put(id,row);prefs.edit().putString(KEY,new JSONObject(rows).toString()).apply();
    }
    private TaskbarRows(){}
}
