package com.nthexperience.desktop;

import android.content.Context;
import android.content.ComponentName;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Rebuildable app/icon cache. All disk and package-manager work stays on the catalog worker. */
final class DesktopCache {
    private static final int LIMIT = 2048, MAX_BYTES = 12 * 1024 * 1024;
    private static List<AppCatalog.Entry> memory;
    private static long refreshed;
    private static String configuration = "";
    static synchronized void invalidate() { refreshed = 0; }
    private static String configuration(Context c) { return c.getResources().getConfiguration().getLocales().toLanguageTags() + ":" + c.getResources().getDisplayMetrics().densityDpi; }
    private static AtomicFile file(Context c) { return new AtomicFile(new File(c.getCacheDir(), "desktop-apps-v1.json")); }
    static synchronized List<AppCatalog.Entry> read(Context c) {
        if (memory != null && configuration.equals(configuration(c))) return new ArrayList<>(memory);
        try (InputStream in = file(c).openRead(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) { if (out.size() + n > MAX_BYTES) return null; out.write(buffer, 0, n); }
            JSONObject root = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            if (root.getInt("version") != 1 || !configuration(c).equals(root.getString("configuration"))) return null;
            JSONArray rows = root.getJSONArray("apps"); if (rows.length() > LIMIT) return null;
            List<AppCatalog.Entry> result = new ArrayList<>(); Set<String> seen = new HashSet<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i); String id = row.getString("id"), label = row.getString("label");
                if (id.length() > 512 || label.length() > 2048 || ComponentName.unflattenFromString(id) == null || !seen.add(id)) return null;
                byte[] png = Base64.decode(row.getString("icon"), Base64.NO_WRAP);
                BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(png, 0, png.length, bounds);
                if (bounds.outWidth < 1 || bounds.outWidth > 96 || bounds.outHeight < 1 || bounds.outHeight > 96) return null;
                Bitmap bitmap = BitmapFactory.decodeByteArray(png, 0, png.length); if (bitmap == null) return null;
                result.add(new AppCatalog.Entry(id, label, new BitmapDrawable(c.getResources(), bitmap), row.optLong("installed"), row.optInt("category", -1)));
            }
            memory = result; configuration = configuration(c); refreshed = 0;
            return new ArrayList<>(result);
        } catch (IOException | JSONException | RuntimeException ignored) { return null; }
    }
    static synchronized List<AppCatalog.Entry> load(Context c) {
        long now = SystemClock.elapsedRealtime(); String key = configuration(c);
        if (memory != null && configuration.equals(key) && refreshed > 0 && now - refreshed < 60000) return new ArrayList<>(memory);
        List<AppCatalog.Entry> result;
        try { result = AppCatalog.load(c); } catch (RuntimeException failed) { return memory == null ? null : new ArrayList<>(memory); }
        // An empty/transitional scan must not erase the last working desktop inventory.
        if (result.isEmpty() && memory != null && !memory.isEmpty()) return new ArrayList<>(memory);
        memory = new ArrayList<>(result); configuration = key; refreshed = now;
        if (result.size() <= LIMIT) write(c, result, key);
        return result;
    }
    private static void write(Context c, List<AppCatalog.Entry> apps, String key) {
        AtomicFile file = file(c); FileOutputStream stream = null;
        try {
            JSONArray rows = new JSONArray();
            for (AppCatalog.Entry app : apps) {
                Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888); Canvas canvas = new Canvas(bitmap);
                Drawable icon = app.icon.getConstantState() == null ? app.icon : app.icon.getConstantState().newDrawable(c.getResources()).mutate();
                android.graphics.Rect original = new android.graphics.Rect(icon.getBounds()); icon.setBounds(0, 0, 64, 64); icon.draw(canvas); icon.setBounds(original);
                ByteArrayOutputStream png = new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.PNG, 100, png); bitmap.recycle();
                rows.put(new JSONObject().put("id", app.id).put("label", app.label).put("installed", app.installed).put("category", app.category).put("icon", Base64.encodeToString(png.toByteArray(), Base64.NO_WRAP)));
            }
            byte[] bytes = new JSONObject().put("version", 1).put("configuration", key).put("apps", rows).toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_BYTES) return;
            stream = file.startWrite(); stream.write(bytes); file.finishWrite(stream);
        } catch (IOException | JSONException | RuntimeException ignored) { if (stream != null) file.failWrite(stream); }
    }
    static boolean matches(Map<String,AppCatalog.Entry> current,List<AppCatalog.Entry> next){
        if(next==null||current.size()!=next.size())return false;for(AppCatalog.Entry app:next)if(current.get(app.id)!=app)return false;return true;
    }
    private DesktopCache() {}
}
