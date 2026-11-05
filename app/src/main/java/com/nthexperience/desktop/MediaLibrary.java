package com.nthexperience.desktop;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class MediaLibrary {
    static final int LIMIT = 128;
    static final String PLAYER = "@mediaplayer", CENTER = "@mediacenter";
    static final String PLAYER_TITLE = "Nth Media Player", CENTER_TITLE = "Nth Media Center";
    static final String[] TYPES = {"audio/mpeg", "video/mp4", "audio/mp4", "audio/wav", "audio/x-wav",
            "audio/flac", "video/webm", "audio/webm"};

    static final class Entry {
        final Uri uri;
        final String name, mime;
        Entry(Uri uri, String name, String mime) {
            this.uri = uri;
            this.name = name == null || name.isEmpty() ? "Untitled media" : name;
            this.mime = mime;
        }
        boolean video() { return mime.startsWith("video/"); }
    }

    static Entry entry(Uri uri, String name, String providerType) {
        if (uri == null || !"content".equals(uri.getScheme())) return null;
        String file = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String mime = FileLaunch.mime(name, providerType);
        if (file.endsWith(".mp3")) mime = "audio/mpeg";
        else if (file.endsWith(".mp4")) mime = "video/mp4";
        else if (file.endsWith(".m4a")) mime = "audio/mp4";
        else if (file.endsWith(".wav")) mime = "audio/wav";
        else if (file.endsWith(".flac")) mime = "audio/flac";
        else if (file.endsWith(".webm") && !mime.equals("audio/webm")) mime = "video/webm";
        for (String type : TYPES) if (type.equals(mime)) return new Entry(uri, name, mime);
        return null;
    }

    static Entry describe(Context context, Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme())) return null;
        String name = null;
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
            return entry(uri, name, context.getContentResolver().getType(uri));
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private final SharedPreferences preferences;
    private final LinkedHashMap<Uri, Entry> entries = new LinkedHashMap<>();
    MediaLibrary(DesktopStore store) {
        preferences = store.context.getSharedPreferences("media", Context.MODE_PRIVATE);
        try {
            JSONArray saved = new JSONArray(preferences.getString("library", "[]"));
            for (int i = 0; i < Math.min(saved.length(), LIMIT); i++) {
                JSONObject value = saved.optJSONObject(i);
                if (value == null) continue;
                Entry item = entry(Uri.parse(value.optString("uri")), value.optString("name"), value.optString("mime"));
                if (item != null) entries.put(item.uri, item);
            }
        } catch (JSONException ignored) {}
    }
    List<Entry> items() {
        List<Entry> result = new ArrayList<>(entries.values());
        result.sort(Comparator.comparing(item -> item.name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }
    List<Entry> recent(){
        List<Entry> result=new ArrayList<>();
        try{
            JSONArray saved=new JSONArray(preferences.getString("recent","[]"));
            for(int i=0;i<Math.min(8,saved.length());i++){
                JSONObject item=saved.optJSONObject(i);
                if(item==null)continue;
                Entry entry=entry(Uri.parse(item.optString("uri")),item.optString("name"),item.optString("mime"));
                if(entry!=null)result.add(entry);
            }
        }catch(JSONException invalid){}
        return result;
    }
    void played(Entry item){
        if(item==null)return;
        List<Entry> history=recent();
        history.removeIf(entry->entry.uri.equals(item.uri));
        history.add(0,item);
        JSONArray saved=new JSONArray();
        for(int i=0;i<Math.min(8,history.size());i++){
            Entry entry=history.get(i);
            JSONObject value=new JSONObject();
            GadgetLayout.put(value,"uri",entry.uri.toString());
            GadgetLayout.put(value,"name",entry.name);
            GadgetLayout.put(value,"mime",entry.mime);
            saved.put(value);
        }
        preferences.edit().putString("recent",saved.toString()).apply();
    }
    boolean contains(Uri uri) { return entries.containsKey(uri); }
    int remaining() { return LIMIT - entries.size(); }
    int add(List<Entry> files) {
        int added = 0;
        for (Entry item : files) {
            if (remaining() == 0) break;
            if (entries.putIfAbsent(item.uri, item) == null) added++;
        }
        if (added > 0) save();
        return added;
    }
    void remove(Entry item) {
        if (entries.remove(item.uri) != null) save();
    }
    private void save() {
        JSONArray saved = new JSONArray();
        for (Entry item : entries.values()) {
            JSONObject value = new JSONObject();
            GadgetLayout.put(value, "uri", item.uri.toString());
            GadgetLayout.put(value, "name", item.name);
            GadgetLayout.put(value, "mime", item.mime);
            saved.put(value);
        }
        preferences.edit().putString("library", saved.toString()).apply();
    }
}
