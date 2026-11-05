package com.nthexperience.desktop;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.CancellationSignal;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

final class ThemeProfiles {
    static final String KEY="seven_saved_themes";
    static final int LIMIT=12;
    private static final int MAX_BYTES=8*1024*1024;
    private static final int MAX_PICTURE=512*1024;
    private static final String FORMAT="nth-desktop-theme";
    private static final String[] SWITCHES={"aero_transparency",DesktopBackgrounds.SHUFFLE,"desktop_sounds",
            "sound_complete","sound_error","sound_notice","startup_sound"};
    private static final boolean[] SWITCH_DEFAULTS={true,false,false,true,true,true,true};
    private static final String[] NUMBERS={"aero_color","aero_intensity","aero_opacity","wallpaper_bg","desktop_sound_volume",DesktopBackgrounds.INTERVAL};
    private static final int[] DEFAULTS={0,45,AeroGlass.DEFAULT_OPACITY,0,65,DesktopBackgrounds.DEFAULT_INTERVAL};
    private static final int[] MAXIMUM={AeroGlass.TINTS.length-1,100,100,BackgroundPicker.COLORS.length-1,100,86400};

    static JSONArray saved(DesktopStore store){
        try{return new JSONArray(store.prefs.getString(KEY,"[]"));}
        catch(JSONException invalid){return new JSONArray();}
    }
    static JSONObject capture(DesktopStore store,String title)throws JSONException{
        JSONObject profile=new JSONObject().put("name",title.trim())
                .put("backgrounds",new JSONArray(DesktopBackgrounds.current(store.prefs)))
                .put("position",store.prefs.getString("wallpaper_position","Fill")).put(DesktopSounds.SCHEME,DesktopSounds.scheme(store));
        for(int i=0;i<SWITCHES.length;i++)profile.put(SWITCHES[i],store.prefs.getBoolean(SWITCHES[i],SWITCH_DEFAULTS[i]));
        for(int i=0;i<NUMBERS.length;i++)profile.put(NUMBERS[i],store.prefs.getInt(NUMBERS[i],DEFAULTS[i]));
        validate(profile);
        return profile;
    }
    static void validateList(JSONArray items)throws JSONException{
        if(items.length()>LIMIT)throw new JSONException("Up to twelve saved themes are supported.");
        for(int i=0;i<items.length();i++)validate(items.getJSONObject(i));
    }
    private static List<String> validate(JSONObject profile)throws JSONException{
        List<String> keys=DesktopBackgrounds.parse(profile.getJSONArray("backgrounds").toString());
        validateStyle(profile);
        if(keys.isEmpty())throw new JSONException("This theme has no pictures.");
        return keys;
    }
    private static void validateStyle(JSONObject profile)throws JSONException{
        String title=profile.getString("name");
        if(title.trim().isEmpty()||title.length()>60||title.chars().anyMatch(Character::isISOControl))throw new JSONException("Enter a theme name of up to 60 characters.");
        if(!Arrays.asList(BackgroundPicker.POSITIONS).contains(profile.getString("position")))throw new JSONException("Invalid picture position.");
        if(profile.has(DesktopSounds.SCHEME)&&(!(profile.get(DesktopSounds.SCHEME) instanceof String)||!SoundSchemes.valid(profile.getString(DesktopSounds.SCHEME))))throw new JSONException("Invalid sound scheme.");
        for(String key:SWITCHES)if(!(profile.get(key) instanceof Boolean))throw new JSONException("Invalid theme switch.");
        for(int i=0;i<NUMBERS.length;i++){
            Object value=profile.get(NUMBERS[i]);
            if(!(value instanceof Integer)||(int)value<0||(int)value>MAXIMUM[i])throw new JSONException("Invalid theme setting.");
        }
        if(!DesktopBackgrounds.intervalValid(profile.getInt(DesktopBackgrounds.INTERVAL)))throw new JSONException("Invalid slideshow interval.");
    }
    static void add(DesktopStore store,JSONObject profile)throws JSONException{
        JSONArray current=saved(store),next=new JSONArray();
        boolean replaced=false;
        for(int i=0;i<current.length();i++){
            JSONObject item=current.getJSONObject(i);
            if(item.optString("name").equalsIgnoreCase(profile.getString("name"))){if(!replaced)next.put(profile);replaced=true;}
            else next.put(item);
        }
        if(!replaced)next.put(profile);
        validateList(next);
        store.prefs.edit().putString(KEY,next.toString()).apply();
    }
    static void remove(DesktopStore store,int index){
        JSONArray items=saved(store);items.remove(index);store.prefs.edit().putString(KEY,items.toString()).apply();
    }
    static void apply(DesktopStore store,JSONObject profile)throws JSONException{
        List<String> pictures=validate(profile);
        SharedPreferences.Editor edit=store.prefs.edit();
        DesktopBackgrounds.choose(edit,pictures,AeroWallpaper.SEVEN);
        edit.putString("wallpaper_position",profile.getString("position")).putString(DesktopSounds.SCHEME,profile.optString(DesktopSounds.SCHEME,"classic"));
        for(String key:SWITCHES)edit.putBoolean(key,profile.getBoolean(key));
        for(String key:NUMBERS)edit.putInt(key,profile.getInt(key));
        edit.apply();
    }
    static byte[] pack(Context context,JSONObject profile)throws Exception{
        List<String> pictures=validate(profile);
        JSONObject portable=new JSONObject(profile.toString());
        JSONArray keys=new JSONArray(),assets=new JSONArray();
        int total=0;
        for(String key:pictures){
            if(DesktopBackgrounds.stockId(key)>=0){keys.put(key);continue;}
            Bitmap image=PhotoImage.read(context,Uri.parse(key),new CancellationSignal(),1024,1024L*1024).bitmap;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try{if(!image.compress(Bitmap.CompressFormat.JPEG,82,bytes))throw new IOException("A theme picture could not be encoded.");}
            finally{image.recycle();}
            total+=bytes.size();
            if(bytes.size()>MAX_PICTURE||total>MAX_BYTES*3/5)throw new IOException("This theme has too many large pictures to share.");
            keys.put("asset:"+assets.length());assets.put(Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP));
        }
        portable.put("backgrounds",keys);
        return new JSONObject().put("format",FORMAT).put("version",1).put("theme",portable).put("pictures",assets)
                .toString().getBytes(StandardCharsets.UTF_8);
    }
    static JSONObject unpack(Context context,InputStream source)throws Exception{
        if(source==null)throw new IOException("This theme could not be opened.");
        ByteArrayOutputStream data=new ByteArrayOutputStream();
        byte[] buffer=new byte[8192];
        int count;
        while((count=source.read(buffer))!=-1){
            if(data.size()+count>MAX_BYTES)throw new IOException("Theme files must be 8 MB or smaller.");
            data.write(buffer,0,count);
        }
        String json=new String(data.toByteArray(),StandardCharsets.UTF_8);
        DesktopBackup.checkDepth(json);
        JSONObject root=new JSONObject(json);
        if(!FORMAT.equals(root.optString("format"))||root.optInt("version")!=1)throw new JSONException("Unsupported theme format.");
        JSONObject profile=root.getJSONObject("theme");
        validateStyle(profile);
        JSONArray keys=profile.getJSONArray("backgrounds"),assets=root.getJSONArray("pictures"),resolved=new JSONArray();
        if(keys.length()<1||keys.length()>DesktopBackgrounds.LIMIT||assets.length()>DesktopBackgrounds.LIMIT)throw new JSONException("Invalid number of theme pictures.");
        java.util.Map<Integer,byte[]> validated=new java.util.HashMap<>();
        for(int i=0;i<keys.length();i++){
            String key=keys.getString(i);
            if(DesktopBackgrounds.stockId(key)>=0)continue;
            int index=assetIndex(key,assets.length());
            if(validated.containsKey(index))throw new JSONException("Duplicate theme picture.");
            String encoded=assets.getString(index);
            if(encoded.length()>MAX_PICTURE*4/3+4)throw new IOException("A theme picture is too large.");
            byte[] image=Base64.decode(encoded,Base64.NO_WRAP);
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(image,0,image.length,bounds);
            if(image.length>MAX_PICTURE||bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>1024||bounds.outHeight>1024
                    ||!"image/jpeg".equals(bounds.outMimeType))throw new IOException("A theme picture is invalid.");
            validated.put(index,image);
        }
        for(int i=0;i<keys.length();i++){
            String key=keys.getString(i);
            String uri=DesktopBackgrounds.stockId(key)>=0?key:ThemePictures.save(context,validated.get(assetIndex(key,assets.length()))).toString();
            if(!contains(resolved,uri))resolved.put(uri);
        }
        profile.put("backgrounds",resolved);validate(profile);
        return profile;
    }
    private static boolean contains(JSONArray items,String key){
        for(int i=0;i<items.length();i++)if(key.equals(items.optString(i)))return true;
        return false;
    }
    private static int assetIndex(String key,int length)throws JSONException{
        if(!key.matches("asset:[0-9]{1,2}"))throw new JSONException("Invalid theme picture reference.");
        int index=Integer.parseInt(key.substring(6));
        if(index>=length)throw new JSONException("Missing theme picture.");
        return index;
    }
}
