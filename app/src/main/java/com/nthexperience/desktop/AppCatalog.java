package com.nthexperience.desktop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AppCatalog {
    static final class Entry {
        final String id, label;
        final Drawable icon; final long installed; final int category;
        Entry(String id, String label, Drawable icon) { this(id,label,icon,0,-1); }
        Entry(String id,String label,Drawable icon,long installed,int category) { this.id=id;this.label=label;this.icon=icon;this.installed=installed;this.category=category; }
    }
    static List<Entry> load(Context context) {
        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : manager.queryIntentActivities(intent, 0)) {
            if (context.getPackageName().equals(info.activityInfo.packageName)) continue;
            ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            String id = component.flattenToString();
            if (!seen.add(id)) continue;
            try {
                entries.add(new Entry(id, info.loadLabel(manager).toString(), info.loadIcon(manager)));
            } catch (RuntimeException ignored) { /* Package may be changing during this scan. */ }
        }
        Collator collator = Collator.getInstance();
        entries.sort((a, b) -> {
            int order = collator.compare(a.label, b.label);
            return order == 0 ? a.id.compareTo(b.id) : order;
        });
        return entries;
    }
}
