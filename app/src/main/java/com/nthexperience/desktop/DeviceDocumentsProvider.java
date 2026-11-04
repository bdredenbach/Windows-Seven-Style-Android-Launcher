package com.nthexperience.desktop;

import android.database.*;
import android.os.*;
import android.provider.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Internal Explorer bridge to primary shared storage. Not registered as a system-picker root. */
public final class DeviceDocumentsProvider extends DocumentsProvider {
    private static final String[] COLUMNS={"document_id","_display_name","mime_type","_size","last_modified","flags"};
    @Override public boolean onCreate(){return true;}
    private void access(){if(!StorageAccess.has(getContext()))throw new SecurityException("Allow file access to browse device storage, or connect an individual folder.");}
    private void owner(){if(Binder.getCallingUid()!=android.os.Process.myUid())throw new SecurityException("Only Nth Explorer can manage device files.");}
    private File root(){return Environment.getExternalStorageDirectory();}
    private File file(String id)throws FileNotFoundException{
        access();if(id==null||!id.startsWith("primary:"))throw missing("Unknown storage location");String relative=id.substring(8);if(relative.startsWith("/")||relative.indexOf('\\')>=0||relative.indexOf('\0')>=0)throw missing("Invalid path");
        try{File base=root().getCanonicalFile(),value=base;for(String segment:relative.isEmpty()?new String[0]:relative.split("/",-1)){if(segment.isEmpty()||segment.equals(".")||segment.equals(".."))throw missing("Invalid path");value=new File(value,segment);if(Files.isSymbolicLink(value.toPath()))throw missing("Symbolic links are not available in device storage");}File canonical=value.getCanonicalFile();if(!canonical.toPath().startsWith(base.toPath()))throw missing("Outside shared storage");String path=base.toPath().relativize(canonical.toPath()).toString();if(path.startsWith("Android/")&&!path.equals("Android/media")&&!path.startsWith("Android/media/"))throw missing("Android protects this app-data location");return canonical;}catch(IOException ex){throw missing(ex.getMessage());}
    }
    private String id(File file)throws FileNotFoundException{try{if(Files.isSymbolicLink(file.toPath()))throw missing("Symbolic links are not available");Path base=root().getCanonicalFile().toPath(),value=file.getCanonicalFile().toPath();if(!value.startsWith(base))throw missing("Outside shared storage");return "primary:"+base.relativize(value).toString();}catch(IOException ex){throw missing(ex.getMessage());}}
    private File existing(String id)throws FileNotFoundException{File file=file(id);if(!file.exists())throw missing("Folder or file not found");return file;}
    private boolean protectedRoot(String id){return id.equals("primary:")||id.equals("primary:Android");}
    private int flags(String id,File file){if(!StorageAccess.has(getContext()))return 0;int flags=0;if(file.isDirectory()&&file.canWrite()&&!id.equals("primary:Android"))flags|=DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;if(file.isFile()&&file.canWrite())flags|=DocumentsContract.Document.FLAG_SUPPORTS_WRITE;if(!protectedRoot(id)&&file.getParentFile()!=null&&file.getParentFile().canWrite())flags|=DocumentsContract.Document.FLAG_SUPPORTS_DELETE|DocumentsContract.Document.FLAG_SUPPORTS_RENAME;return flags;}
    private String mime(File file){return file.isDirectory()?DocumentsContract.Document.MIME_TYPE_DIR:FileLaunch.mime(file.getName(),null);}
    private void row(MatrixCursor cursor,String id,File file){MatrixCursor.RowBuilder row=cursor.newRow();for(String column:cursor.getColumnNames())switch(column){case "document_id":row.add(id);break;case "_display_name":row.add(id.equals("primary:")?"Internal storage":file.getName());break;case "mime_type":row.add(mime(file));break;case "_size":row.add(file.isDirectory()?null:file.length());break;case "last_modified":row.add(file.lastModified());break;case "flags":row.add(flags(id,file));break;default:row.add(null);}}
    @Override public Cursor queryRoots(String[] projection){return new MatrixCursor(projection==null?new String[]{"root_id","document_id","title","flags","mime_types"}:projection);}
    @Override public Cursor queryDocument(String documentId,String[] projection)throws FileNotFoundException{File file=existing(documentId);MatrixCursor cursor=new MatrixCursor(projection==null?COLUMNS:projection);row(cursor,documentId,file);return cursor;}
    static final String INCOMPLETE_CHILDREN = BuildConfig.APPLICATION_ID+".INCOMPLETE_CHILDREN";
    @Override public Cursor queryChildDocuments(String parentId,String[] projection,String order)throws FileNotFoundException{
        owner();File parent=existing(parentId);
        if(!parent.isDirectory())throw missing("Not a folder");
        MatrixCursor cursor=new MatrixCursor(projection==null?COLUMNS:projection);
        boolean incomplete=false;
        try(DirectoryStream<Path> stream=Files.newDirectoryStream(parent.toPath())){
            int count=0;
            for(Path child:stream){
                String id=id(child.toFile());
                try{
                    File allowed=file(id);
                    if(!allowed.canRead()){incomplete=true;continue;}
                    row(cursor,id,allowed);
                    if(++count>=10001){incomplete=true;break;}
                }catch(FileNotFoundException ignored){incomplete=true;}
            }
        }catch(IOException ex){throw missing("This folder cannot be read");}
        if(incomplete){Bundle extras=new Bundle();extras.putBoolean(INCOMPLETE_CHILDREN,true);cursor.setExtras(extras);}
        return cursor;
    }
    @Override public ParcelFileDescriptor openDocument(String documentId,String mode,CancellationSignal signal)throws FileNotFoundException{if(signal!=null)signal.throwIfCanceled();File file=existing(documentId);if(!file.isFile())throw missing("Choose a file");if(!mode.equals("r")){owner();if((flags(documentId,file)&DocumentsContract.Document.FLAG_SUPPORTS_WRITE)==0)throw missing("File is read-only");}return ParcelFileDescriptor.open(file,ParcelFileDescriptor.parseMode(mode));}
    private void require(String id,int capability)throws FileNotFoundException{owner();File file=existing(id);if((flags(id,file)&capability)==0)throw missing("This item does not allow that action");}
    @Override public String createDocument(String parentId,String mime,String displayName)throws FileNotFoundException{require(parentId,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE);String name=DocumentOps.name(displayName);File target=new File(existing(parentId),name);String id=id(target);target=file(id);try{boolean made=DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)?target.mkdir():target.createNewFile();if(!made)throw missing("An item with that name already exists, or the folder is read-only");return id;}catch(IOException ex){throw missing(ex.getMessage());}}
    @Override public String renameDocument(String documentId,String displayName)throws FileNotFoundException{require(documentId,DocumentsContract.Document.FLAG_SUPPORTS_RENAME);File source=existing(documentId),destination=new File(source.getParentFile(),DocumentOps.name(displayName));String result=id(destination);destination=file(result);try{Files.move(source.toPath(),destination.toPath());}catch(IOException ex){throw missing("The item could not be renamed; its original was kept");}return result;}
    @Override public void deleteDocument(String documentId)throws FileNotFoundException{require(documentId,DocumentsContract.Document.FLAG_SUPPORTS_DELETE);List<File> postorder=new ArrayList<>();collect(documentId,postorder,new int[]{0},0);for(File value:postorder){access();if(!value.delete())throw missing("Deletion stopped. Refresh to see which items remain");}}
    private void collect(String id,List<File> files,int[] count,int depth)throws FileNotFoundException{if(++count[0]>DocumentOps.MAX_NODES||depth>DocumentOps.MAX_DEPTH)throw missing("Delete supports at most 5,000 items and 32 nested levels");require(id,DocumentsContract.Document.FLAG_SUPPORTS_DELETE);File value=existing(id);if(value.isDirectory())try(DirectoryStream<Path> children=Files.newDirectoryStream(value.toPath())){for(Path child:children)collect(id(child.toFile()),files,count,depth+1);}catch(IOException ex){throw missing("The complete folder could not be checked. Nothing has been deleted");}files.add(value);}
    @Override public boolean isChildDocument(String parentId,String documentId){try{File parent=file(parentId),child=file(documentId);return !child.equals(parent)&&child.toPath().startsWith(parent.toPath());}catch(FileNotFoundException|SecurityException ex){return false;}}
    private static FileNotFoundException missing(String text){return new FileNotFoundException(text==null?"Storage unavailable":text);}
}
