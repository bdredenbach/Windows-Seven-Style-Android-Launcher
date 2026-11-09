package com.nthexperience.desktop;

import org.json.*;
import java.util.*;

/** Translate portable items before the destination's existing strict validation. */
final class BackupCompatibility {
    static final String[] WALLPAPERS = DesktopBackgrounds.NAMES;
    static void normalize(Map<String,Object> values, JSONObject backup, Set<String> skipped) throws JSONException {
        JSONObject folders = new JSONObject((String) values.get("app_folders"));
        for (Iterator<String> keys = folders.keys(); keys.hasNext();) {
            JSONObject folder = folders.getJSONObject(keys.next());
            folder.put("items", shortcuts(folder.getJSONArray("items"), folders, false, DesktopStore.MAX_ITEMS, "app_folders", skipped));
        }
        values.put("app_folders", folders.toString());
        for (String key : Arrays.asList("shortcuts", "quick_launch", "file_manager_apps", "recents", "start_pins")) if (values.containsKey(key)) {
            boolean allowFolder = !key.equals("file_manager_apps") && !key.equals("recents");
            int limit = key.equals("quick_launch") ? DesktopStore.MAX_QUICK : key.equals("recents") ? 4 : key.equals("start_pins") ? 32 : 512;
            values.put(key, shortcuts(new JSONArray((String)values.get(key)), folders, allowFolder, limit, key, skipped).toString());
        }
        for (String key : Arrays.asList("shortcut_names", "desktop_positions_portrait", "desktop_positions_landscape")) if (values.containsKey(key)) {
            JSONObject entries = new JSONObject((String)values.get(key));
            filterObject(entries, folders, key, skipped); values.put(key, entries.toString());
        }
        if (values.containsKey("ten_desktop_items")) {
            JSONObject desktops = new JSONObject((String)values.get("ten_desktop_items"));
            for (Iterator<String> keys = desktops.keys(); keys.hasNext();) {
                JSONObject row = desktops.getJSONObject(keys.next());
                row.put("shortcuts", shortcuts(row.getJSONArray("shortcuts"), folders, true, 512, "ten_desktop_items", skipped));
                for (String axis : Arrays.asList("portrait", "landscape")) filterObject(row.getJSONObject(axis), folders, "ten_desktop_items", skipped);
            }
            values.put("ten_desktop_items", desktops.toString());
        }
        if (values.containsKey("seven_saved_themes")) {
            JSONArray themes = new JSONArray((String)values.get("seven_saved_themes")), kept = new JSONArray();
            for (int i = 0; i < themes.length(); i++) {
                JSONObject theme = themes.getJSONObject(i); JSONArray backgrounds = theme.getJSONArray("backgrounds"), pictures = new JSONArray();
                for (int j = 0; j < backgrounds.length(); j++) {
                    String id = backgrounds.getString(j);
                    if (id.startsWith("#")) {
                        int mapped; try { mapped = wallpaper(Integer.parseInt(id.substring(1)), backup); } catch (NumberFormatException invalid) { mapped = -1; }
                        if (mapped < 0) { skipped.add("seven_saved_themes (background unavailable in this desktop)"); continue; }
                        id = "#" + mapped;
                    }
                    pictures.put(id);
                }
                if (pictures.length() > 0) kept.put(theme.put("backgrounds", pictures));
                else skipped.add("seven_saved_themes (theme has no compatible background)");
            }
            values.put("seven_saved_themes", kept.toString());
        }
        if (values.containsKey("wallpaper")) {
            int mapped = wallpaper((Integer)values.get("wallpaper"), backup);
            if (mapped < 0) { values.remove("wallpaper"); skipped.add("wallpaper (background unavailable in this desktop)"); }
            else values.put("wallpaper", mapped);
        }
        if (values.containsKey("wallpaper_slides")) {
            JSONArray slides = new JSONArray((String)values.get("wallpaper_slides")), kept = new JSONArray();
            for (int i = 0; i < slides.length(); i++) {
                String id = slides.getString(i);
                if (id.startsWith("#")) {
                    int mapped; try { mapped = wallpaper(Integer.parseInt(id.substring(1)), backup); } catch (NumberFormatException invalid) { mapped = -1; }
                    if (mapped < 0) { skipped.add("wallpaper_slides (background unavailable in this desktop)"); continue; }
                    id = "#" + mapped;
                }
                kept.put(id);
            }
            if (kept.length() == 0) values.remove("wallpaper_slides"); else values.put("wallpaper_slides", kept.toString());
        }
    }
    private static int wallpaper(int index, JSONObject backup) {
        JSONArray names = backup.optJSONArray("wallpapers");
        if (names == null) return index >= 0 && index < Math.min(WALLPAPERS.length, 11) ? index : -1;
        if (index < 0 || index >= names.length()) return -1;
        String name = names.optString(index, "");
        for (int i = 0; i < WALLPAPERS.length; i++) if (WALLPAPERS[i].equals(name)) return i;
        return -1;
    }
    private static JSONArray shortcuts(JSONArray source, JSONObject folders, boolean allowFolder, int limit, String key, Set<String> skipped) throws JSONException {
        if (source.length() > 2048) throw new JSONException("Shortcut list is too large");
        JSONArray kept = new JSONArray(); Set<String> seen = new HashSet<>();
        for (int i = 0; i < source.length(); i++) {
            Object raw = source.get(i); if (!(raw instanceof String) || !seen.add((String)raw)) throw new JSONException("Invalid or duplicate list item");
            String id = (String)raw; if (id.length() > 512) throw new JSONException("Shortcut identifier is too long");
            if (unsupported(id, folders, allowFolder)) { skipped.add(key + ": " + id); continue; }
            if (kept.length() >= limit) { skipped.add(key + " (destination limit)"); continue; }
            kept.put(id);
        }
        return kept;
    }
    private static void filterObject(JSONObject entries, JSONObject folders, String key, Set<String> skipped) {
        Iterator<String> ids = entries.keys();
        while (ids.hasNext()) { String id = ids.next(); if (unsupported(id, folders, true)) { ids.remove(); skipped.add(key + ": " + id); } }
    }
    private static boolean unsupported(String id, JSONObject folders, boolean allowFolder) {
        return !DesktopBackup.shortcut(id, folders, allowFolder) && (id.startsWith("@") || id.startsWith("location:"));
    }
    private BackupCompatibility() {}
}
