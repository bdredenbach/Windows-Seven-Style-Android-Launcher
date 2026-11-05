package com.nthexperience.desktop;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

/** App-private, immutable wallpaper images carried by portable theme files. */
public final class ThemePictures extends ContentProvider {
    private static final String AUTHORITY=BuildConfig.APPLICATION_ID+".theme-pictures";
    private static final long MAX_STORAGE=64L*1024*1024;

    private static File directory(Context context){return new File(context.getFilesDir(),"theme-pictures");}

    static synchronized Uri save(Context context,byte[] bytes)throws Exception{
        StringBuilder digest=new StringBuilder();
        for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes))digest.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
        File directory=directory(context);
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Theme picture storage is unavailable.");
        File file=new File(directory,digest+".jpg");
        if(!file.exists()){
            long used=0;
            File[] entries=directory.listFiles();
            if(entries!=null)for(File entry:entries)used+=entry.length();
            if(used+bytes.length>MAX_STORAGE)throw new IOException("The theme picture storage limit has been reached.");
            AtomicFile target=new AtomicFile(file);
            FileOutputStream stream=target.startWrite();
            try{stream.write(bytes);target.finishWrite(stream);}
            catch(IOException failure){target.failWrite(stream);throw failure;}
        }
        return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(digest.toString()).build();
    }

    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "image/jpeg";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        String digest=uri.getLastPathSegment();
        if(!"r".equals(mode)||uri.getPathSegments().size()!=1||digest==null||!digest.matches("[0-9a-f]{64}"))throw new FileNotFoundException();
        return ParcelFileDescriptor.open(new File(directory(getContext()),digest+".jpg"),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] arguments,String order){return null;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] arguments){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] arguments){throw new UnsupportedOperationException();}
}
