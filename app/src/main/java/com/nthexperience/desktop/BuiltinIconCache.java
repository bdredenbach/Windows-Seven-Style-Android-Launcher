package com.nthexperience.desktop;

import android.graphics.Bitmap;
import android.util.LruCache;

/** Shared, bounded cache of static Nth icon artwork. Live gadget faces are never cached here. */
final class BuiltinIconCache {
    private static final LruCache<String,Bitmap> images=new LruCache<String,Bitmap>(4*1024*1024){
        @Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}
    };
    static Bitmap get(String key){return images.get(key);}
    static void put(String key,Bitmap bitmap){if(bitmap.getAllocationByteCount()<=images.maxSize())images.put(key,bitmap);}
    static void clear(){images.evictAll();}
    static int bytes(){return images.size();}
    private BuiltinIconCache(){}
}
