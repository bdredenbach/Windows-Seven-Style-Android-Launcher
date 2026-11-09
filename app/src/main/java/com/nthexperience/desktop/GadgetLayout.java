package com.nthexperience.desktop;

import org.json.*;
import java.util.*;

/** A separate arrangement per orientation; enabled gadgets and their data are shared. */
final class GadgetLayout {
    final DesktopStore store;
    final String key;
    private final JSONObject data;
    GadgetLayout(DesktopStore store,String key){
        this.store=store;this.key=key;
        String saved=store.prefs.getString("gadget_layout_"+key,null);
        if(saved==null)saved=store.prefs.getString("gadget_layout_"+(key.equals("landscape")?"portrait":"landscape"),null);
        JSONObject parsed;try{parsed=new JSONObject(saved==null?"{}":saved);}catch(JSONException ex){parsed=new JSONObject();}data=parsed;
        // Use the established portrait choice when older orientations disagree.
        boolean show=data.optBoolean("visible",store.prefs.getBoolean("sidebar_visible",true));
        try{JSONObject portrait=new JSONObject(store.prefs.getString("gadget_layout_portrait","{}"));if(portrait.has("visible"))show=portrait.getBoolean("visible");}catch(JSONException ignored){}
        sharedVisibility(show);
        put(data,"visible",show);
        save();
    }
    JSONObject placement(String id){JSONObject value=data.optJSONObject(id);if(value==null){value=new JSONObject();put(value,"docked",true);put(value,"x",.4);put(value,"y",.15);put(value,"scale",1);put(data,id,value);}return value;}
    // Older backups use "docked" for automatic placement at the edge.
    boolean docked(String id){return placement(id).optBoolean("docked",true);}
    float x(String id){return (float)placement(id).optDouble("x",.4);}
    float y(String id){return (float)placement(id).optDouble("y",.15);}
    float scale(String id){return DesktopGeometry.clamp((float)placement(id).optDouble("scale",1),.65f,1.8f);}
    void position(String id,float x,float y,float scale){JSONObject p=placement(id);put(p,"docked",false);put(p,"x",DesktopGeometry.clamp(x,0,1));put(p,"y",DesktopGeometry.clamp(y,0,1));put(p,"scale",DesktopGeometry.clamp(scale,.65f,1.8f));save();}
    void dock(String id){put(placement(id),"docked",true);save();}
    boolean visible(){return store.prefs.getBoolean("sidebar_visible",true);}
    void visible(boolean show){sharedVisibility(show);put(data,"visible",show);save();}
    private void sharedVisibility(boolean show){
        android.content.SharedPreferences.Editor edit=store.prefs.edit().putBoolean("sidebar_visible",show);
        for(String axis:new String[]{"portrait","landscape"}){String saved=store.prefs.getString("gadget_layout_"+axis,null);if(saved==null)continue;try{JSONObject row=new JSONObject(saved);put(row,"visible",show);edit.putString("gadget_layout_"+axis,row.toString());}catch(JSONException ignored){}}
        edit.apply();
    }
    List<String> ordered(){List<String> enabled=store.gadgets(),result=new ArrayList<>();JSONArray order=data.optJSONArray("order");if(order!=null)for(int i=0;i<order.length();i++){String id=order.optString(i);if(enabled.contains(id)&&!result.contains(id))result.add(id);}for(String id:enabled)if(!result.contains(id))result.add(id);return result;}
    void save(){put(data,"visible",visible());store.prefs.edit().putString("gadget_layout_"+key,data.toString()).apply();}
    static void put(JSONObject target,String name,Object value){try{target.put(name,value);}catch(JSONException ignored){}}
}
