package com.nthexperience.desktop;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.widget.Toast;
import java.util.*;
import org.json.*;

/** Optional primary shared-storage access. Never requests system-settings or root privileges. */
final class StorageAccess {
    static final String AUTHORITY=BuildConfig.APPLICATION_ID+".storage",PREFS="file_access_setup";
    static final int REQUEST=64;
    static final String[][] FAVORITES={{"documents","Documents",Environment.DIRECTORY_DOCUMENTS},{"pictures","Pictures",Environment.DIRECTORY_PICTURES},{"music","Music",Environment.DIRECTORY_MUSIC},{"videos","Videos",Environment.DIRECTORY_MOVIES},{"downloads","Downloads",Environment.DIRECTORY_DOWNLOADS}};
    static boolean has(Context context){if(Build.VERSION.SDK_INT>=30)return Environment.isExternalStorageManager();return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED&&context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;}
    static boolean ours(Uri uri){return uri!=null&&"content".equals(uri.getScheme())&&AUTHORITY.equals(uri.getAuthority());}
    static Uri tree(){return DocumentsContract.buildTreeDocumentUri(AUTHORITY,"primary:");}
    static Uri document(String relative){return DocumentsContract.buildDocumentUriUsingTree(tree(),"primary:"+relative);}
    static String directory(String kind){for(String[] favorite:FAVORITES)if(favorite[0].equals(kind))return favorite[2];throw new IllegalArgumentException("Unknown device folder");}
    static String page(String kind){String root=DocumentFolders.rootPage(tree().toString(),"Internal storage");if(kind.equals("internal"))return root;try{String path=directory(kind);return DocumentFolders.childPage(root,new DocumentFolders.Item("primary:"+path,path,DocumentsContract.Document.MIME_TYPE_DIR,0,0));}catch(JSONException ex){throw new IllegalStateException(ex);}}
    static void request(Activity activity){
        if(Build.VERSION.SDK_INT<30){activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST);return;}
        try{activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+activity.getPackageName())),REQUEST);}catch(ActivityNotFoundException|SecurityException ex){try{activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),REQUEST);}catch(ActivityNotFoundException|SecurityException missing){Toast.makeText(activity,"File access settings are unavailable. You can still connect individual folders.",Toast.LENGTH_LONG).show();}}
    }
    static Intent settingsIntent(Context context){return new Intent(Build.VERSION.SDK_INT>=30?Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION:Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+context.getPackageName()));}
}
