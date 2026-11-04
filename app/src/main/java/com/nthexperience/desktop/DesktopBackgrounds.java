package com.nthexperience.desktop;

import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class DesktopBackgrounds {
    static final String[] NAMES={"Aero","Twilight","Meadow","Seven Blue","Harmony","Alpine Morning",
            "Pacific Dusk","Canyon Light","Glass Pavilion","Midnight Circuit","Paper Skies"};
    static final String[] GROUPS={"Aero and Abstract","Landscapes","Architecture","Illustrations"};
    static final int[] INTERVALS={10,30,60,180,600,1800,3600,21600,86400};
    static final String[] INTERVAL_LABELS={"10 seconds","30 seconds","1 minute","3 minutes","10 minutes",
            "30 minutes","1 hour","6 hours","1 day"};
    static final int DEFAULT_INTERVAL=1800;
    static final int LIMIT=24;
    static final String SLIDES="wallpaper_slides",INTERVAL="wallpaper_interval",SHUFFLE="wallpaper_shuffle";

    static int group(int id){return id<5||id==9?0:id<8?1:id==8?2:3;}
    static String stock(int id){return "#"+id;}
    static int stockId(String key){
        if(!key.startsWith("#"))return -1;
        try{int id=Integer.parseInt(key.substring(1));return id>=0&&id<NAMES.length?id:-1;}
        catch(NumberFormatException ex){return -1;}
    }
    static boolean valid(String key){
        return key!=null&&key.length()<=8192&&(stockId(key)>=0||"content".equals(Uri.parse(key).getScheme()));
    }
    static boolean intervalValid(int seconds){for(int value:INTERVALS)if(value==seconds)return true;return false;}
    static List<String> parse(String raw) throws JSONException {
        JSONArray array=new JSONArray(raw);
        if(array.length()>LIMIT)throw new JSONException("Too many desktop backgrounds");
        LinkedHashSet<String> keys=new LinkedHashSet<>();
        for(int i=0;i<array.length();i++){
            String key=array.getString(i);
            if(!valid(key)||!keys.add(key))throw new JSONException("Invalid desktop background");
        }
        return new ArrayList<>(keys);
    }
    static List<String> current(SharedPreferences prefs){
        try{
            List<String> slides=parse(prefs.getString(SLIDES,"[]"));
            if(!slides.isEmpty())return slides;
        }catch(JSONException ignored){}
        String photo=prefs.getString("wallpaper_photo","");
        int id=prefs.getInt("wallpaper",AeroWallpaper.SEVEN);
        return java.util.Collections.singletonList(valid(photo)?photo:stock(id>=0&&id<NAMES.length?id:AeroWallpaper.SEVEN));
    }
    static void choose(SharedPreferences.Editor edit,List<String> keys,int fallback){
        String first=keys.get(0);
        int id=stockId(first);
        edit.putInt("wallpaper",id<0?fallback:id).putString("wallpaper_photo",id<0?first:"");
        if(keys.size()>1)edit.putString(SLIDES,new JSONArray(keys).toString());
        else edit.remove(SLIDES);
    }
}
