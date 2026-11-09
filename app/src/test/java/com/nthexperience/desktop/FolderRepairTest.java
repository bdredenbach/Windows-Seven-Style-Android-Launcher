package com.nthexperience.desktop;

import android.content.*;
import android.content.pm.ProviderInfo;
import android.database.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class FolderRepairTest {
    private Context context;private DesktopStore store;
    private static final String OLD="content://example.documents/tree/old",NEW="content://example.documents/tree/new",ID="location:12345678-1234-1234-1234-123456789abc";
    @Before public void setup(){context=RuntimeEnvironment.getApplication();store=new DesktopStore(context);store.prefs.edit().clear().commit();}
    private String page(String uri,String root)throws Exception{return DocumentFolders.childPage(DocumentFolders.rootPage(uri,"Saved root"),new DocumentFolders.Item(root+"/reports","Reports",DocumentsContract.Document.MIME_TYPE_DIR,0,0));}
    private void saved()throws Exception{store.prefs.edit().putString("linked_folders",new JSONArray().put(new JSONObject().put("uri",OLD).put("name","Saved root")).toString()).putString("folder_links",new JSONObject().put(ID,new JSONObject().put("page",page(OLD,"old")).put("title","My reports")).toString()).putString("explorer_favorites",new JSONObject().put("documents",OLD).toString()).putString("shortcuts",new JSONArray().put(ID).toString()).putString("desktop_positions_portrait","{\"saved\":true}").putString("note","Keep this note").commit();}
    @Test public void reconnectUpdatesRootFavoriteAndExistingShortcutWithoutMovingIt()throws Exception{
        saved();String positions=store.prefs.getString("desktop_positions_portrait","");String shortcuts=store.prefs.getString("shortcuts","");FolderRepair.Plan plan=FolderRepair.reconnect(store,OLD,Uri.parse(NEW),(original,tree)->{try{return page(NEW,"new");}catch(Exception ex){throw new RuntimeException(ex);}});assertTrue(plan.apply(store));assertEquals(0,plan.unresolved);assertEquals(NEW,DocumentFolders.links(store).getJSONObject(0).getString("uri"));assertEquals(NEW,new JSONObject(store.prefs.getString("explorer_favorites","{}")).getString("documents"));JSONObject link=FolderLinks.get(store,ID);assertEquals("My reports",link.getString("title"));assertEquals(NEW,DocumentFolders.path(link.getString("page")).getString("tree"));assertEquals(shortcuts,store.prefs.getString("shortcuts",""));assertEquals(positions,store.prefs.getString("desktop_positions_portrait",""));assertEquals("Keep this note",store.prefs.getString("note",""));
    }
    @Test public void unavailableSubfolderIsKeptForAnotherRepair()throws Exception{
        saved();String original=FolderLinks.get(store,ID).getString("page");FolderRepair.Plan plan=FolderRepair.reconnect(store,OLD,Uri.parse(NEW),(page,tree)->null);assertTrue(plan.apply(store));assertEquals(1,plan.unresolved);assertEquals(original,FolderLinks.get(store,ID).getString("page"));List<FolderRepair.Target> missing=FolderRepair.missing(store,page->{try{return DocumentFolders.path(page).getString("tree").equals(NEW);}catch(JSONException ex){return false;}});assertEquals(1,missing.size());assertEquals(OLD,missing.get(0).uri);
    }
    @Test public void concurrentFolderChangesAreNotOverwritten()throws Exception{
        saved();FolderRepair.Plan plan=FolderRepair.reconnect(store,OLD,Uri.parse(NEW),(page,tree)->null);String changed="{\"documents\":\"content://example.documents/tree/other\"}";store.prefs.edit().putString("explorer_favorites",changed).commit();assertFalse(plan.apply(store));assertEquals(changed,store.prefs.getString("explorer_favorites",""));assertEquals(OLD,DocumentFolders.links(store).getJSONObject(0).getString("uri"));
    }
    @Test public void missingRootShortcutAndFavoriteAreOfferedOnce()throws Exception{saved();List<FolderRepair.Target> missing=FolderRepair.missing(store,page->false);assertEquals(1,missing.size());assertEquals(OLD,missing.get(0).uri);assertTrue(FolderRepair.missing(store,page->true).isEmpty());}
    @Test public void reconnectDoesNotCreateDuplicateRoots()throws Exception{saved();JSONArray roots=DocumentFolders.links(store);roots.put(new JSONObject().put("uri",NEW).put("name","Existing"));store.prefs.edit().putString("linked_folders",roots.toString()).commit();assertTrue(FolderRepair.reconnect(store,OLD,Uri.parse(NEW),(page,tree)->null).apply(store));assertEquals(1,DocumentFolders.links(store).length());}
    public static class Folders extends ContentProvider {
        boolean duplicate,missing;
        @Override public boolean onCreate(){return true;}
        @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){MatrixCursor cursor=new MatrixCursor(projection);boolean children="children".equals(uri.getLastPathSegment());if(children){if(!missing){row(cursor,projection,"new-child","Reports",DocumentsContract.Document.MIME_TYPE_DIR);if(duplicate)row(cursor,projection,"ambiguous-child","Reports",DocumentsContract.Document.MIME_TYPE_DIR);}}else row(cursor,projection,DocumentsContract.getDocumentId(uri),"Folder",DocumentsContract.Document.MIME_TYPE_DIR);return cursor;}
        private void row(MatrixCursor cursor,String[] columns,String id,String name,String mime){Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++)values[i]=columns[i].equals(DocumentsContract.Document.COLUMN_DOCUMENT_ID)?id:columns[i].equals(DocumentsContract.Document.COLUMN_DISPLAY_NAME)?name:mime;cursor.addRow(values);}
        @Override public String getType(Uri uri){return DocumentsContract.Document.MIME_TYPE_DIR;}@Override public Uri insert(Uri uri,ContentValues values){return null;}@Override public int delete(Uri uri,String where,String[] args){return 0;}@Override public int update(Uri uri,ContentValues values,String where,String[] args){return 0;}
    }
    private Folders provider(){Folders provider=new Folders();ProviderInfo info=new ProviderInfo();info.authority="example.documents";provider.attachInfo(context,info);ShadowContentResolver.registerProviderInternal(info.authority,provider);return provider;}
    @Test public void actualProviderNeedsGrantAndResolvesChildByName()throws Exception{provider();String root=DocumentFolders.rootPage(NEW,"Saved root");assertFalse(FolderRepair.readable(context,root));context.getContentResolver().takePersistableUriPermission(Uri.parse(NEW),Intent.FLAG_GRANT_READ_URI_PERMISSION);assertTrue(FolderRepair.readable(context,root));String resolved=FolderRepair.resolve(context,page(OLD,"old"),Uri.parse(NEW));assertNotNull(resolved);JSONObject path=DocumentFolders.path(resolved);assertEquals("new",path.getJSONArray("crumbs").getJSONObject(0).getString("id"));assertEquals("new-child",path.getJSONArray("crumbs").getJSONObject(1).getString("id"));}
    @Test public void missingOrAmbiguousProviderFolderDoesNotGuessAReplacement()throws Exception{Folders provider=provider();context.getContentResolver().takePersistableUriPermission(Uri.parse(NEW),Intent.FLAG_GRANT_READ_URI_PERMISSION);provider.duplicate=true;assertNull(FolderRepair.resolve(context,page(OLD,"old"),Uri.parse(NEW)));provider.duplicate=false;provider.missing=true;assertNull(FolderRepair.resolve(context,page(OLD,"old"),Uri.parse(NEW)));}
    @Test public void onlyKnownDesktopProviderAuthoritiesCanBeTranslated(){
        Uri local=FolderRepair.localTree("content://com.nthexperience.desktop.storage/tree/primary%3ADocuments");assertNotNull(local);assertEquals(StorageAccess.AUTHORITY,local.getAuthority());assertEquals("primary:Documents",DocumentsContract.getTreeDocumentId(local));assertNull(FolderRepair.localTree("content://unrelated.documents/tree/primary%3ADocuments"));assertNull(FolderRepair.localTree("content://com.nthexperience.desktop.storage/tree/other"));
    }

}
