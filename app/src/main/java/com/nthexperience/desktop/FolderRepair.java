package com.nthexperience.desktop;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.*;
import java.util.*;

/** Reconnects restored folder references without changing shortcut identities or dropping missing paths. */
final class FolderRepair {
    private static final String[] KEYS={"linked_folders","folder_links","explorer_favorites"};
    interface Access {boolean readable(String page);}
    interface Resolver {String resolve(String page,Uri tree);}
    static final class Target {final String uri,name;Target(String uri,String name){this.uri=uri;this.name=name;}}
    static List<Target> missing(Context context,DesktopStore store){return missing(store,page->readable(context,page));}
    static List<Target> missing(DesktopStore store,Access access){
        LinkedHashMap<String,Target> result=new LinkedHashMap<>();
        JSONArray roots=DocumentFolders.links(store);
        for(int i=0;i<roots.length();i++){JSONObject root=roots.optJSONObject(i);if(root!=null)add(result,root.optString("uri"),root.optString("name","Linked folder"),access);}
        JSONObject shortcuts=FolderLinks.list(store);Iterator<String> ids=shortcuts.keys();
        while(ids.hasNext()){JSONObject link=shortcuts.optJSONObject(ids.next());if(link==null)continue;String page=link.optString("page");try{JSONObject path=DocumentFolders.path(page);if(!access.readable(page)){String uri=path.getString("tree"),name=path.getJSONArray("crumbs").getJSONObject(0).optString("name",link.optString("title","Linked folder"));result.putIfAbsent(uri,new Target(uri,name));}}catch(JSONException|RuntimeException ignored){}}
        JSONObject favorites=object(store.prefs.getString("explorer_favorites","{}"));Iterator<String> kinds=favorites.keys();while(kinds.hasNext()){String kind=kinds.next();add(result,favorites.optString(kind),kind,access);}
        return new ArrayList<>(result.values());
    }
    private static void add(Map<String,Target> targets,String uri,String name,Access access){try{Uri tree=Uri.parse(uri);if("content".equals(tree.getScheme())&&DocumentsContract.isTreeUri(tree)&&!access.readable(DocumentFolders.rootPage(uri,name)))targets.putIfAbsent(uri,new Target(uri,name));}catch(RuntimeException ignored){}}
    private static boolean desktopAuthority(String authority){return Arrays.asList("com.nthexperience.desktop.storage","com.nthexperience.desktop.seven.storage","com.nthexperience.desktop.eight.storage","com.nthexperience.desktop.ten.storage","com.nthexperience.desktop.eleven.storage").contains(authority);}
    static Uri localTree(String uri){try{Uri old=Uri.parse(uri);String id=DocumentsContract.getTreeDocumentId(old);return "content".equals(old.getScheme())&&desktopAuthority(old.getAuthority())&&id.startsWith("primary:")?DocumentsContract.buildTreeDocumentUri(StorageAccess.AUTHORITY,id):null;}catch(RuntimeException invalid){return null;}}
    static boolean readable(Context context,String page){
        try{JSONObject path=DocumentFolders.path(page);Uri tree=Uri.parse(path.getString("tree"));boolean allowed=StorageAccess.ours(tree)&&StorageAccess.has(context);for(UriPermission permission:context.getContentResolver().getPersistedUriPermissions())if(permission.isReadPermission()&&permission.getUri().equals(tree))allowed=true;if(!allowed)return false;
            JSONArray crumbs=path.getJSONArray("crumbs");Uri document=DocumentsContract.buildDocumentUriUsingTree(tree,crumbs.getJSONObject(crumbs.length()-1).getString("id"));try(Cursor cursor=context.getContentResolver().query(document,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},null,null,null)){return cursor!=null&&cursor.moveToFirst();}
        }catch(JSONException|RuntimeException inaccessible){return false;}
    }
    static String resolve(Context context,String page,Uri tree){
        try{JSONObject path=DocumentFolders.path(page),copy=new JSONObject(path.toString());JSONArray old=path.getJSONArray("crumbs"),crumbs=new JSONArray();String root=DocumentsContract.getTreeDocumentId(tree);copy.put("tree",tree.toString());
            if((Uri.parse(path.getString("tree")).getAuthority().equals(tree.getAuthority())||desktopAuthority(Uri.parse(path.getString("tree")).getAuthority())&&desktopAuthority(tree.getAuthority()))&&old.getJSONObject(0).getString("id").equals(root)&&readable(context,"folder:"+copy))return "folder:"+copy;
            crumbs.put(new JSONObject(old.getJSONObject(0).toString()).put("id",root));String parent=root;
            for(int i=1;i<old.length();i++){String name=old.getJSONObject(i).getString("name"),found=null;Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent);try(Cursor cursor=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(cursor==null)return null;while(cursor.moveToNext())if(name.equals(cursor.getString(1))&&DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))){if(found!=null)return null;found=cursor.getString(0);}}if(found==null)return null;crumbs.put(new JSONObject(old.getJSONObject(i).toString()).put("id",found));parent=found;}
            copy.put("crumbs",crumbs);String resolved="folder:"+copy;return readable(context,resolved)?resolved:null;
        }catch(JSONException|RuntimeException inaccessible){return null;}
    }
    static final class Plan {
        final Map<String,String> before,after;final int unresolved;
        Plan(Map<String,String> before,Map<String,String> after,int unresolved){this.before=before;this.after=after;this.unresolved=unresolved;}
        boolean changes(){for(Map.Entry<String,String> row:after.entrySet())if(!Objects.equals(before.get(row.getKey()),row.getValue()))return true;return false;}
        synchronized boolean apply(DesktopStore store){
            for(String key:KEYS)if(!Objects.equals(before.get(key),store.prefs.getString(key,null)))return false;
            SharedPreferences.Editor edit=store.prefs.edit();for(Map.Entry<String,String> row:after.entrySet())if(!Objects.equals(before.get(row.getKey()),row.getValue()))edit.putString(row.getKey(),row.getValue());
            if(edit.commit())return true;
            SharedPreferences.Editor rollback=store.prefs.edit();for(String key:KEYS)if(before.get(key)==null)rollback.remove(key);else rollback.putString(key,before.get(key));rollback.commit();return false;
        }
    }
    static Plan reconnect(DesktopStore store,String oldUri,Uri tree,Resolver resolver)throws JSONException{
        Map<String,String> before=new LinkedHashMap<>(),after=new LinkedHashMap<>();for(String key:KEYS)before.put(key,store.prefs.getString(key,null));String replacement=tree.toString();
        JSONArray roots=new JSONArray(value(before.get("linked_folders"),"[]")),updated=new JSONArray();Set<String> seen=new HashSet<>();
        for(int i=0;i<roots.length();i++){JSONObject row=new JSONObject(roots.getJSONObject(i).toString());if(oldUri.equals(row.optString("uri")))row.put("uri",replacement);if(seen.add(row.getString("uri")))updated.put(row);}if(before.get("linked_folders")!=null)after.put("linked_folders",updated.toString());
        JSONObject links=object(value(before.get("folder_links"),"{}"));Iterator<String> ids=links.keys();int unresolved=0;
        while(ids.hasNext()){JSONObject link=links.optJSONObject(ids.next());if(link==null)continue;String page=link.optString("page");if(oldUri.equals(DocumentFolders.path(page).optString("tree"))){String resolved=resolver.resolve(page,tree);if(resolved==null)unresolved++;else link.put("page",resolved);}}
        if(before.get("folder_links")!=null)after.put("folder_links",links.toString());
        JSONObject favorites=object(value(before.get("explorer_favorites"),"{}"));Iterator<String> kinds=favorites.keys();while(kinds.hasNext()){String kind=kinds.next();if(oldUri.equals(favorites.optString(kind)))favorites.put(kind,replacement);}if(before.get("explorer_favorites")!=null)after.put("explorer_favorites",favorites.toString());
        return new Plan(before,after,unresolved);
    }
    private static String value(String value,String fallback){return value==null?fallback:value;}
    private static JSONObject object(String text){try{return new JSONObject(text);}catch(JSONException invalid){return new JSONObject();}}
    private FolderRepair(){}
}
