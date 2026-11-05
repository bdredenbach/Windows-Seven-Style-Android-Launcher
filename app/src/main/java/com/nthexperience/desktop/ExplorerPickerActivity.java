package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** A Vista file chooser for GET_CONTENT and explicit Nth integrations. Only selected files are granted. */
public final class ExplorerPickerActivity extends Activity {
    static final String EXTRA_WALLPAPER_FOLDER=BuildConfig.APPLICATION_ID+".extra.WALLPAPER_FOLDER";
    private VistaExplorer explorer;private DesktopStore store;private PickerSpec spec;private TextView choose,notice;private final List<Uri> selected=new ArrayList<>();
    private boolean wallpaper,wallpaperFolder;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());private final DocumentOps.Cancel cancel=new DocumentOps.Cancel();private boolean completing,closed,storageAllowed;private android.window.OnBackInvokedCallback back;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);setResult(RESULT_CANCELED);try{spec=new PickerSpec(getIntent());}catch(RuntimeException ex){finish();return;}store=new DesktopStore(this);storageAllowed=StorageAccess.has(this);if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);getWindow().setStatusBarColor(0xff173446);getWindow().setNavigationBarColor(0xff173446);
        wallpaper=getPackageName().equals(getCallingPackage())&&getIntent().hasExtra(EXTRA_WALLPAPER_FOLDER);
        wallpaperFolder=wallpaper&&getIntent().getBooleanExtra(EXTRA_WALLPAPER_FOLDER,false);
        LinearLayout root=Aero.column(this);root.setBackground(Aero.gloss(this,new int[]{0xff6d8798,0xff29495d},3,0xffb2cad6));root.setPadding(dp(6),dp(4),dp(6),dp(6));
        root.setOnApplyWindowInsetsListener((v,insets)->{int l,t,r,b;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());l=safe.left;t=safe.top;r=safe.right;b=safe.bottom;}else{l=insets.getSystemWindowInsetLeft();t=insets.getSystemWindowInsetTop();r=insets.getSystemWindowInsetRight();b=insets.getSystemWindowInsetBottom();}root.setPadding(l+dp(6),t+dp(4),r+dp(6),b+dp(6));return insets;});
        String purpose=wallpaperFolder?"Choose a background folder":wallpaper?"Choose background pictures":"Choose files";
        LinearLayout title=new LinearLayout(this);title.setGravity(Gravity.CENTER_VERTICAL);title.addView(new AeroIcon(this,"files"),new LinearLayout.LayoutParams(dp(28),dp(28)));TextView caption=Aero.text(this,"Nth Explorer — "+purpose,16,0xffeef9ff);caption.setPadding(dp(10),0,0,0);title.addView(caption,new LinearLayout.LayoutParams(0,dp(45),1));TextView close=Aero.button(this,"×",this::finish);close.setContentDescription("Cancel file selection");title.addView(close,new LinearLayout.LayoutParams(dp(44),dp(36)));root.addView(title);
        String instructions=wallpaperFolder?"Open a folder and choose Use this folder to add its pictures."
                :wallpaper?"Check the pictures you want to add, then choose Open."
                :"Choose a linked folder. Only the files you select are shared with the requesting app.";
        notice=Aero.text(this,instructions,12,0xff263c4b);notice.setPadding(dp(10),dp(8),dp(10),dp(8));notice.setBackgroundColor(0xffe6eff5);root.addView(notice,new LinearLayout.LayoutParams(-1,-2));
        VistaShell.Actions actions=new VistaShell.Actions(){public void open(String id){openLauncher(id);}public void action(String id){if(id.equals("android:storage"))try{startActivity(new Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS));}catch(ActivityNotFoundException ignored){}else openLauncher("@control");}};
        explorer=new VistaExplorer(this,store,actions,new VistaExplorer.Files(){
            public void chooseFolder(){folder(null);}
            public void requestFolder(Uri initial){folder(initial);}
            public void requestDeviceAccess(){StorageAccess.request(ExplorerPickerActivity.this);}
            public void linkFile(){}
            public void pickFile(String kind){if(!wallpaper)systemFile();}
            public void openDocument(Uri uri,String mime){selected.clear();selected.add(uri);if(spec.multiple)update();else complete();}
            public void app(String id){openLauncher(id);}
            public void caption(String title){if(wallpaperFolder)update();}
        },Collections::emptyList,"home",new VistaExplorer.Selection(){
            public boolean accepts(String mime){return !wallpaperFolder&&spec.accepts(mime);}
            public boolean multiple(){return spec.multiple;}
            public void changed(List<Uri> files){selected.clear();selected.addAll(files);update();}
            public boolean foldersOnly(){return wallpaperFolder;}
        });
        explorer.pickerMode();
        if(saved==null&&wallpaper){
            if(!wallpaperFolder&&LibraryLocations.locations(store,"pictures").size()>1)explorer.openLibrary("pictures");
            else if(storageAllowed)explorer.openFavorite("pictures");
        }
        root.addView(explorer,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);footer.setPadding(dp(8),dp(8),dp(8),dp(8));footer.setBackgroundColor(0xffe7eef4);TextView dismiss=Aero.button(this,"Cancel",this::finish);dismiss.setContentDescription("Cancel file selection");footer.addView(dismiss,new LinearLayout.LayoutParams(dp(90),dp(42)));
        choose=Aero.button(this,wallpaperFolder?"Use this folder":"Open",wallpaperFolder?this::completeFolder:this::complete);
        choose.setContentDescription(wallpaperFolder?"Use this folder":"Return selected files");
        footer.addView(choose,new LinearLayout.LayoutParams(dp(wallpaperFolder?148:100),dp(42)));root.addView(footer,new LinearLayout.LayoutParams(-1,-2));setContentView(root);root.requestApplyInsets();
        if(saved!=null)try{explorer.restore(new org.json.JSONObject(saved.getString("explorer","{}")));}catch(org.json.JSONException ignored){}update();if(Build.VERSION.SDK_INT>=33){back=this::back;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,back);}
    }
    private void update(){
        if(choose==null)return;
        if(wallpaperFolder){choose.setEnabled(explorer.availableFolder()!=null);return;}
        choose.setEnabled(!completing&&!selected.isEmpty());choose.setText(selected.size()>1?"Open ("+selected.size()+")":"Open");
    }
    private void completeFolder(){
        Uri folder=explorer.availableFolder();
        if(folder==null)return;
        setResult(RESULT_OK,new Intent().setData(folder));
        finish();
    }
    private void complete(){if(completing||selected.isEmpty())return;List<Uri> files=new ArrayList<>(selected);int limit=wallpaper?DesktopBackgrounds.LIMIT:256;if(files.size()>limit){notice.setText("Choose at most "+limit+" files at a time.");return;}if(!spec.multiple&&files.size()!=1)return;completing=true;update();notice.setText("Preparing selected files…");worker.execute(()->{try{for(Uri uri:files){cancel.check();if(!"content".equals(uri.getScheme())||!spec.accepts(getContentResolver().getType(uri)))throw new java.io.IOException("A selected file no longer matches this request.");try(ParcelFileDescriptor descriptor=getContentResolver().openFileDescriptor(uri,"r",cancel.signal)){if(descriptor==null)throw new java.io.IOException("A selected file cannot be opened.");}}
                Intent result=new Intent();result.setData(files.get(0));ClipData clip=ClipData.newRawUri("Selected files",files.get(0));for(int i=1;i<files.size();i++)clip.addItem(new ClipData.Item(files.get(i)));result.setClipData(clip);if(!wallpaper)result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);main.post(()->{if(!closed){setResult(RESULT_OK,result);finish();}});
            }catch(Exception ex){main.post(()->{if(!closed){completing=false;notice.setText(ex.getMessage()==null?"A file is unavailable. Choose it again.":ex.getMessage());update();}});}});}
    private void folder(Uri initial){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).putExtra(DocumentsContract.EXTRA_INITIAL_URI,initial).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),1);}catch(ActivityNotFoundException ex){notice.setText("No Android folder-access picker is available.");}}
    private void systemFile(){try{Intent request=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES,spec.types.toArray(new String[0])).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,spec.multiple).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(request,2);}catch(ActivityNotFoundException ex){notice.setText("No Android document picker is available.");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==StorageAccess.REQUEST){storageAllowed=StorageAccess.has(this);explorer.refreshFiles();update();return;}if(result!=RESULT_OK||data==null)return;if(request==1&&data.getData()!=null){Uri uri=data.getData();int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);try{if(!DocumentsContract.isTreeUri(uri)||(flags&Intent.FLAG_GRANT_READ_URI_PERMISSION)==0)throw new SecurityException();getContentResolver().takePersistableUriPermission(uri,flags);String name=DocumentFolders.rootName(this,uri);if(!DocumentFolders.add(store,uri,name))throw new IllegalStateException("Remove a linked folder before adding another.");explorer.folderAdded(uri.toString(),name);}catch(RuntimeException ex){notice.setText("Folder access could not be retained. Choose another folder.");}}
        if(request==2){selected.clear();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)selected.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)selected.add(data.getData());complete();}}
    private void openLauncher(String id){startActivity(new Intent(this,MainActivity.class).putExtra("open_tool",id));}
    private void back(){if(explorer==null||!explorer.goBack())finish();}
    @Override public void onBackPressed(){back();}
    @Override protected void onResume(){super.onResume();if(explorer!=null&&storageAllowed!=StorageAccess.has(this)){storageAllowed=StorageAccess.has(this);explorer.refreshFiles();}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==StorageAccess.REQUEST&&explorer!=null){storageAllowed=StorageAccess.has(this);explorer.refreshFiles();}}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(explorer!=null)state.putString("explorer",explorer.state().toString());}
    @Override protected void onDestroy(){closed=true;cancel.cancel();if(explorer!=null)explorer.close();worker.shutdownNow();main.removeCallbacksAndMessages(null);if(Build.VERSION.SDK_INT>=33&&back!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(back);super.onDestroy();}
    private int dp(float value){return Aero.dp(this,value);}
}
