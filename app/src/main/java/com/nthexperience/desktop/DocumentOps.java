package com.nthexperience.desktop;

import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.security.*;
import java.util.*;

/** Real SAF operations. Copies never overwrite; cuts delete only after a verified complete copy. */
final class DocumentOps {
    static final int MAX_NODES=5000,MAX_DEPTH=32;
    static final String[] COLUMNS={"document_id","_display_name","mime_type","_size","last_modified","flags"};
    static final class Ref {
        final Uri uri,parent;final DocumentFolders.Item item;
        Ref(Uri uri,Uri parent,DocumentFolders.Item item){this.uri=uri;this.parent=parent;this.item=item;}
    }
    static final class Cancel {
        final CancellationSignal signal=new CancellationSignal();
        private final List<ParcelFileDescriptor> opened=new ArrayList<>();
        void check(){signal.throwIfCanceled();}
        synchronized void track(ParcelFileDescriptor descriptor){check();opened.add(descriptor);}
        synchronized void untrack(ParcelFileDescriptor descriptor){opened.remove(descriptor);}
        synchronized void cancel(){signal.cancel();for(ParcelFileDescriptor descriptor:opened)try{descriptor.close();}catch(IOException ignored){}opened.clear();}
    }
    private static final class Node {final Ref source;final List<Node> children=new ArrayList<>();byte[] hash;Node(Ref source){this.source=source;}}
    static final class CopyUndo {private final List<Node> copies;private CopyUndo(List<Node> copies){this.copies=copies;}void undo(Context context,Cancel cancel)throws IOException{for(Node node:copies){verifyUnchanged(context,node,cancel);verifyReceipt(context,node,cancel);require(context,node.source.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,cancel);}for(Node node:copies){cancel.check();if(!DocumentsContract.deleteDocument(context.getContentResolver(),node.source.uri))throw new IOException("A copied item could not be removed. Refresh the folder.");}}}
    static boolean writable(Context context,Uri uri,int flag,DocumentFolders.Item item){return item!=null&&(item.flags&flag)!=0&&(StorageAccess.ours(uri)?StorageAccess.has(context):context.checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==PackageManager.PERMISSION_GRANTED);}
    static String name(String value){String name=value==null?"":value.trim();if(name.isEmpty()||name.equals(".")||name.equals("..")||name.length()>240||name.indexOf('/')>=0||name.indexOf('\\')>=0||name.chars().anyMatch(x->x<32||x==127))throw new IllegalArgumentException("Use a name of 1–240 characters without slashes or control characters.");return name;}
    static Uri current(String page)throws org.json.JSONException {org.json.JSONObject path=DocumentFolders.path(page);org.json.JSONArray crumbs=path.getJSONArray("crumbs");return DocumentsContract.buildDocumentUriUsingTree(Uri.parse(path.getString("tree")),crumbs.getJSONObject(crumbs.length()-1).getString("id"));}
    static DocumentFolders.Item stat(Context context,Uri uri,Cancel cancel)throws IOException {
        cancel.check();try(Cursor cursor=context.getContentResolver().query(uri,COLUMNS,null,null,null,cancel.signal)){if(cursor==null||!cursor.moveToFirst())throw new FileNotFoundException("The file or folder is unavailable.");return item(cursor);}catch(SecurityException ex){throw new IOException("Access was removed. Link the folder again.",ex);}
    }
    private static DocumentFolders.Item item(Cursor cursor){return new DocumentFolders.Item(cursor.getString(0),cursor.isNull(1)?"Unnamed item":cursor.getString(1),cursor.isNull(2)?"application/octet-stream":cursor.getString(2),cursor.isNull(3)?-1:cursor.getLong(3),cursor.isNull(4)?0:cursor.getLong(4),cursor.isNull(5)?0:cursor.getInt(5));}
    static List<Ref> children(Context context,Uri parent,Cancel cancel)throws IOException {
        return children(context,parent,cancel,false);
    }
    static List<Ref> children(Context context,Uri parent,Cancel cancel,boolean requireComplete)throws IOException {
        Uri query=DocumentsContract.buildChildDocumentsUriUsingTree(parent,DocumentsContract.getDocumentId(parent));List<Ref> result=new ArrayList<>();cancel.check();
        try(Cursor cursor=context.getContentResolver().query(query,COLUMNS,null,null,null,cancel.signal)){if(cursor==null)throw new IOException("The folder cannot be read.");while(cursor.moveToNext()){cancel.check();if(result.size()>=10000)throw new IOException("This operation is limited to folders with at most 10,000 immediate entries.");DocumentFolders.Item item=item(cursor);result.add(new Ref(DocumentsContract.buildDocumentUriUsingTree(parent,item.id),parent,item));}
            if(requireComplete&&(cursor.getExtras().getBoolean(DeviceDocumentsProvider.INCOMPLETE_CHILDREN)
                    ||cursor.getExtras().getBoolean(DocumentsContract.EXTRA_LOADING)
                    ||cursor.getExtras().containsKey(DocumentsContract.EXTRA_ERROR)))
                throw new IOException("The complete folder could not be read. Original kept.");
        }return result;
    }
    static boolean same(Uri a,Uri b){return a!=null&&b!=null&&Objects.equals(a.getAuthority(),b.getAuthority())&&DocumentsContract.getDocumentId(a).equals(DocumentsContract.getDocumentId(b));}
    private static void require(Context context,Uri uri,int flag,Cancel cancel)throws IOException {if(!writable(context,uri,flag,stat(context,uri,cancel)))throw new IOException("This location is read-only or does not support that action. Link the folder again to request changes.");}
    static String available(Context context,Uri parent,String requested,boolean keepBoth,Cancel cancel)throws IOException {
        String clean=name(requested);Set<String> names=new HashSet<>();for(Ref entry:children(context,parent,cancel))names.add(entry.item.name.toLowerCase(Locale.ROOT));if(!names.contains(clean.toLowerCase(Locale.ROOT)))return clean;
        if(!keepBoth)throw new IOException("An item with that name already exists. Choose another name.");int dot=clean.lastIndexOf('.');String stem=dot>0?clean.substring(0,dot):clean,ext=dot>0?clean.substring(dot):"";if(stem.length()>200)stem=stem.substring(0,200);
        for(int n=1;n<10000;n++){String candidate=stem+" - Copy"+(n==1?"":" ("+n+")")+ext;if(!names.contains(candidate.toLowerCase(Locale.ROOT)))return candidate;}throw new IOException("Choose a different destination folder.");
    }
    static Uri createFolder(Context context,Uri parent,String requested,Cancel cancel)throws IOException {require(context,parent,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,cancel);String clean=available(context,parent,requested,false,cancel);Uri created=DocumentsContract.createDocument(context.getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,clean);if(created==null)throw new IOException("The folder could not be created.");return created;}
    static Uri rename(Context context,Ref source,String requested,Cancel cancel)throws IOException {
        require(context,source.uri,DocumentsContract.Document.FLAG_SUPPORTS_RENAME,cancel);String clean=name(requested);if(clean.equals(source.item.name))return source.uri;if(source.parent==null)throw new IOException("Rename items inside a linked folder.");available(context,source.parent,clean,false,cancel);Uri result=DocumentsContract.renameDocument(context.getContentResolver(),source.uri,clean);if(result==null)throw new IOException("The item could not be renamed.");return result;
    }
    static void delete(Context context,List<Ref> sources,Cancel cancel)throws IOException {for(Ref source:sources){cancel.check();require(context,source.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,cancel);}for(Ref source:sources){cancel.check();if(!DocumentsContract.deleteDocument(context.getContentResolver(),source.uri))throw new IOException("An item could not be deleted. Refresh to see what remains.");}}
    private static Node plan(Context context,Ref source,Uri target,Cancel cancel,Set<String> visited,int[] count,int depth)throws IOException {
        cancel.check();if(depth>MAX_DEPTH||++count[0]>MAX_NODES)throw new IOException("Copy supports at most 5,000 items and 32 nested levels per operation.");if(same(source.uri,target))throw new IOException("A folder cannot be pasted into itself or one of its subfolders.");String identity=source.uri.getAuthority()+"|"+DocumentsContract.getDocumentId(source.uri);if(!visited.add(identity))throw new IOException("This folder contains a repeated or circular document reference.");
        DocumentFolders.Item fresh=stat(context,source.uri,cancel);if((fresh.flags&DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)!=0)throw new IOException("Export this virtual document from its owning app before copying.");Node node=new Node(new Ref(source.uri,source.parent,fresh));
        if(fresh.directory())for(Ref child:children(context,source.uri,cancel))node.children.add(plan(context,child,target,cancel,visited,count,depth+1));return node;
    }
    static String paste(Context context,List<Ref> sources,Uri target,boolean cut,Cancel cancel,java.util.function.Consumer<String> progress)throws IOException {return paste(context,sources,target,cut,cancel,progress,null);}
    static String paste(Context context,List<Ref> sources,Uri target,boolean cut,Cancel cancel,java.util.function.Consumer<String> progress,java.util.function.Consumer<CopyUndo> receipt)throws IOException {
        return paste(context,sources,target,cut,cancel,progress,receipt,value->{});
    }
    static String paste(Context context,List<Ref> sources,Uri target,boolean cut,Cancel cancel,java.util.function.Consumer<String> progress,java.util.function.Consumer<CopyUndo> receipt,java.util.function.IntConsumer percent)throws IOException {
        if(sources.isEmpty())throw new IOException("Copy or cut an item first.");require(context,target,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,cancel);
        List<Node> plans=new ArrayList<>();Set<String> visited=new HashSet<>();int[] count={0};
        for(Ref source:sources){if(cut&&same(source.parent,target))throw new IOException("The selected item is already in this folder.");if(cut)require(context,source.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,cancel);plans.add(plan(context,source,target,cancel,visited,count,0));}
        List<Uri> created=new ArrayList<>();boolean complete=false;
        try{
            for(Node node:plans){
                cancel.check();progress.accept("Copying "+node.source.item.name+"…");
                String unique=available(context,target,node.source.item.name,true,cancel);
                Uri destination=create(context,target,node.source.item.mime,unique);
                created.add(destination);
                copy(context,node,destination,cancel,progress);
                if(plans.size()>1)percent.accept(Math.min(99,created.size()*100/plans.size()));
            }
            cancel.check();complete=true;
        }finally{if(!complete)for(Uri uri:created)try{DocumentsContract.deleteDocument(context.getContentResolver(),uri);}catch(Exception ignored){}}
        if(cut){for(Node node:plans)verifyUnchanged(context,node,cancel);for(Node node:plans){cancel.check();progress.accept("Moving "+node.source.item.name+"…");try{require(context,node.source.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,cancel);if(!DocumentsContract.deleteDocument(context.getContentResolver(),node.source.uri))throw new IOException();}catch(Exception ex){throw new IOException("Copies were verified, but an original could not be removed. Check both folders before trying again.",ex);}}}
        if(!cut&&receipt!=null){List<Node> snapshots=new ArrayList<>();Set<String> seen=new HashSet<>();int[] nodes={0};for(Uri uri:created){Ref reference=new Ref(uri,target,stat(context,uri,cancel));Node snapshot=plan(context,reference,null,cancel,seen,nodes,0);hashReceipt(context,snapshot,cancel);snapshots.add(snapshot);}receipt.accept(new CopyUndo(snapshots));}
        return (cut?"Moved ":"Copied ")+sources.size()+" item"+(sources.size()==1?"":"s")+". Existing destination items were kept.";
    }
    static Uri createText(Context context,Uri parent,String requested,String text,Cancel cancel)throws IOException{require(context,parent,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,cancel);String clean=available(context,parent,requested,false,cancel);Uri result=create(context,parent,"text/plain",clean);try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(result,"w",cancel.signal)){if(fd==null)throw new IOException("The document could not be opened for writing.");cancel.track(fd);try(OutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(fd)){cancel.check();output.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));output.flush();}finally{cancel.untrack(fd);}}catch(Exception failure){try{DocumentsContract.deleteDocument(context.getContentResolver(),result);}catch(Exception ignored){}throw new IOException("The text document could not be created.",failure);}return result;}
    private static byte[] hash(Context context,Uri uri,Cancel cancel)throws IOException{MessageDigest digest=digest();try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r",cancel.signal)){if(fd==null)throw new IOException("A copied file is unavailable.");cancel.track(fd);try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(fd)){byte[] buffer=new byte[65536];int n;while((n=input.read(buffer))!=-1){cancel.check();digest.update(buffer,0,n);}}finally{cancel.untrack(fd);}}return digest.digest();}
    private static void hashReceipt(Context context,Node node,Cancel cancel)throws IOException{if(node.source.item.directory()){for(Node child:node.children)hashReceipt(context,child,cancel);}else node.hash=hash(context,node.source.uri,cancel);}
    private static void verifyReceipt(Context context,Node node,Cancel cancel)throws IOException{if(node.source.item.directory()){for(Node child:node.children)verifyReceipt(context,child,cancel);}else if(!Arrays.equals(node.hash,hash(context,node.source.uri,cancel)))throw new IOException("A copied file has changed. Undo kept your files.");}
    private static Uri create(Context context,Uri parent,String mime,String name)throws IOException {Uri uri=DocumentsContract.createDocument(context.getContentResolver(),parent,mime,name);if(uri==null)throw new IOException("The destination provider could not create the copy.");return uri;}
    private static void copy(Context context,Node node,Uri destination,Cancel cancel,java.util.function.Consumer<String> progress)throws IOException {
        if(node.source.item.directory()){for(Node child:node.children){cancel.check();progress.accept("Copying "+child.source.item.name+"…");Uri next=create(context,destination,child.source.item.mime,available(context,destination,child.source.item.name,true,cancel));copy(context,child,next,cancel,progress);}return;}
        MessageDigest sourceHash=digest();long count=0;ParcelFileDescriptor input=null,output=null;
        try{input=context.getContentResolver().openFileDescriptor(node.source.uri,"r",cancel.signal);output=context.getContentResolver().openFileDescriptor(destination,"w",cancel.signal);if(input==null||output==null)throw new IOException("A stream could not be opened.");cancel.track(input);cancel.track(output);
            try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(input);OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(output)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){cancel.check();out.write(buffer,0,n);sourceHash.update(buffer,0,n);count+=n;}out.flush();}
        }finally{if(input!=null){cancel.untrack(input);try{input.close();}catch(IOException ignored){}}if(output!=null){cancel.untrack(output);try{output.close();}catch(IOException ignored){}}}
        if(node.source.item.size>=0&&node.source.item.size!=count)throw new IOException("The source changed during copying. Its original was kept.");
        MessageDigest copiedHash=digest();long copied=0;ParcelFileDescriptor verify=context.getContentResolver().openFileDescriptor(destination,"r",cancel.signal);if(verify==null)throw new IOException("The copy could not be verified.");cancel.track(verify);
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(verify)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){cancel.check();copiedHash.update(buffer,0,n);copied+=n;}}finally{cancel.untrack(verify);try{verify.close();}catch(IOException ignored){}}
        if(copied!=count||!Arrays.equals(sourceHash.digest(),copiedHash.digest()))throw new IOException("Copy verification failed. The original was kept.");
    }
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException ex){throw new AssertionError(ex);}}
    private static void verifyUnchanged(Context context,Node node,Cancel cancel)throws IOException {
        DocumentFolders.Item now=stat(context,node.source.uri,cancel);DocumentFolders.Item before=node.source.item;if(!now.name.equals(before.name)||now.size!=before.size||now.modified!=before.modified)throw new IOException("Copies were made, but a source changed. Originals were kept.");
        if(before.directory()){List<Ref> current=children(context,node.source.uri,cancel);Set<String> ids=new HashSet<>();for(Ref child:current)ids.add(child.item.id);if(current.size()!=node.children.size())throw new IOException("The source folder changed. Originals were kept.");for(Node child:node.children){if(!ids.contains(child.source.item.id))throw new IOException("The source folder changed. Originals were kept.");verifyUnchanged(context,child,cancel);}}
    }
}
