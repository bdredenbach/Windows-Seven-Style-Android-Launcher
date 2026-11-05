package com.nthexperience.desktop;

import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Vista navigation and real, permission-scoped document operations, also reused by the external picker. */
final class VistaExplorer extends VistaShell {
    interface Files {
        void chooseFolder();void linkFile();void pickFile(String kind);void openDocument(Uri uri,String mime);void app(String id);
        default void openDocument(Uri uri,String mime,String name){openDocument(uri,mime);}
        default void openWith(Uri uri,String mime,String name){openDocument(uri,mime,name);}
        default void openPicture(Uri uri,String mime,String name,JSONObject source){openDocument(uri,mime,name);}
        default void openFolder(String page){}default void desktopFolder(String page,String name){}
        default void operationFailed(){}default void operationStarted(){}
        default void requestFolder(Uri initial){chooseFolder();}default void requestDeviceAccess(){}default void changed(){}default void caption(String title){}
    }
    interface Selection {boolean accepts(String mime);boolean multiple();void changed(List<Uri> files);default boolean foldersOnly(){return false;}}
    interface Work {String run(DocumentOps.Cancel cancel)throws Exception;}
    private final ExplorerThumbnails thumbnails;
    private final Files files;private final Supplier<List<AppCatalog.Entry>> apps;private final Selection picker;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(),operations=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());private CancellationSignal cancellation;
    private String loadedPage="",pendingFavorite="",sort="Name",message="";private DocumentFolders.Listing listing;private DocumentFolders.Item parentInfo;
    private boolean details,closed,failed,selectionMode,busy,descending,restoredSelection;private int generation;
    private int operationProgress=-2;
    int taskProgress(){return operationProgress;}
    private void copyProgress(int value){main.post(()->{if(!closed&&busy)operationProgress=value;});}
    private String viewMode,groupBy="None";private boolean recentOnly,showHidden;private final Set<String> collapsedGroups=new HashSet<>();private DocumentOps.CopyUndo undoCopy;private FrameLayout detailIcon;private LinearLayout detailsPane;private TextView sendTo;
    private final LinkedHashMap<String,DocumentOps.Ref> selected=new LinkedHashMap<>();
    private final LinkedHashSet<String> expanded=new LinkedHashSet<>();private final Map<String,List<DocumentFolders.Item>> treeCache=new HashMap<>();private final Set<String> treeLoading=new HashSet<>();
    private final View menuBar;
    private String libraryLoaded="";
    private final List<LibraryRow> libraryRows=new ArrayList<>();
    private final Map<String,DocumentFolders.Item> libraryFolders=new LinkedHashMap<>();
    private final Set<String> unavailableLibraries=new HashSet<>();
    private boolean libraryLoading;
    private boolean previewEnabled;
    private String previewPage="";
    private ExplorerPreview preview;
    private static final class LibraryRow {
        final String page,name;
        final DocumentOps.Ref ref;
        LibraryRow(String page,String name,DocumentOps.Ref ref){this.page=page;this.name=name;this.ref=ref;}
    }

    private final LinearLayout crumbs;private final TextView cancelWork;private DocumentOps.Cancel operation;private DesktopContextMenu contextMenu;
    VistaExplorer(Context c,DesktopStore store,Actions actions,Files files,Supplier<List<AppCatalog.Entry>> apps,String initial){this(c,store,actions,files,apps,initial,null);}
    VistaExplorer(Context c,DesktopStore store,Actions actions,Files files,Supplier<List<AppCatalog.Entry>> apps,String initial,Selection picker){
        super(c,store,actions,"Search this folder");thumbnails=new ExplorerThumbnails(c);this.files=files;this.apps=apps;this.picker=picker;search.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(512)});viewMode=store.prefs.getString("explorer_view_mode",store.prefs.getBoolean("explorer_details",true)?"Details":"Tiles");details=viewMode.equals("Details");showHidden=store.prefs.getBoolean("explorer_hidden",false);
        TextView organize=command("Organize ▾",this::organize),views=command("Views ▾",this::views);
        commands.addView(organize,new LayoutParams(-2,dp(36)));commands.addView(views,new LayoutParams(-2,dp(36)));
        if(picker==null){
            TextView pane=command("▤",this::togglePreview);pane.setContentDescription("Toggle Preview Pane");
            commands.addView(pane,new LayoutParams(dp(30),dp(36)));
        }
        cancelWork=command("Cancel operation",()->{if(operation!=null)operation.cancel();});cancelWork.setContentDescription("Cancel file operation");cancelWork.setVisibility(GONE);commands.addView(cancelWork,new LayoutParams(-2,dp(36)));
        LinearLayout location=(LinearLayout)address.getParent();int at=location.indexOfChild(address);location.removeView(address);crumbs=new LinearLayout(c);crumbs.setGravity(Gravity.CENTER_VERTICAL);crumbs.setBackground(Aero.panel(c,0xffffffff,0xfff1f7fb,1,0xff8ea6b8));crumbs.setContentDescription("Address bar");location.addView(crumbs,at,new LayoutParams(0,dp(32),1));
        menuBar=explorerChrome(this::classicMenu);
        menuBar.setVisibility(store.prefs.getBoolean("explorer_menu_bar",false)?VISIBLE:GONE);
        ((View)commands.getParent()).setBackground(Aero.gloss(c,new int[]{0xfff6faff,0xffeaf2fb,0xffdce8f6},0,0xffbacddd));
        folders.setTextColor(0xff214567);
        for(int i=0;i<commands.getChildCount();i++)((TextView)commands.getChildAt(i)).setTextColor(0xff214567);
        side.setBackground(Aero.panel(c,0xfffdfefe,0xfff2f7fd,0,0xffd1dce8));
        sendTo=command("Send to ▾",this::sendSelected);sendTo.setTextColor(0xff214567);sendTo.setContentDescription("Send selected files");if(picker==null)commands.addView(sendTo,new LayoutParams(-2,dp(36)));
        TextView refresh=command("↻",this::refreshFiles);refresh.setTextSize(19);refresh.setContentDescription("Refresh folder");location.addView(refresh,new LayoutParams(dp(28),dp(32)));
        FrameLayout queryBox=new FrameLayout(c);ViewGroup queryParent=(ViewGroup)search.getParent();ViewGroup.LayoutParams queryLayout=search.getLayoutParams();queryParent.removeView(search);queryBox.addView(search,new FrameLayout.LayoutParams(-1,-1));search.setPadding(dp(7),0,dp(26),0);TextView clear=command("×",()->search.setText(""));clear.setTextSize(18);clear.setContentDescription("Clear folder search");queryBox.addView(clear,new FrameLayout.LayoutParams(dp(26),-1,Gravity.END));queryParent.addView(queryBox,queryLayout);
        int statusIndex=indexOfChild(status);removeView(status);detailsPane=new LinearLayout(c);detailsPane.setContentDescription("Explorer details pane");detailsPane.setGravity(Gravity.CENTER_VERTICAL);detailsPane.setPadding(dp(10),dp(3),dp(8),dp(3));detailsPane.setBackground(Aero.panel(c,0xfff4f8fd,0xffe4edf7,0,0xffc3d4e3));detailIcon=new FrameLayout(c);detailsPane.addView(detailIcon,new LayoutParams(dp(42),dp(42)));status.setSingleLine(false);status.setMaxLines(3);status.setTextSize(11);status.setBackgroundColor(Color.TRANSPARENT);detailsPane.addView(status,new LayoutParams(0,-1,1));status.setGravity(Gravity.CENTER_VERTICAL);addView(detailsPane,statusIndex,new LayoutParams(-1,dp(54)));
        enableNavigationToggle();addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{boolean wide=r-l>=dp(620);ViewGroup target=wide?location:queryParent;if(queryBox.getParent()!=target){((ViewGroup)queryBox.getParent()).removeView(queryBox);target.addView(queryBox,wide?new LayoutParams(dp(190),dp(32)):queryLayout);}detailsPane.setVisibility(b-t<dp(290)?GONE:VISIBLE);
            boolean narrow=r-l<dp(260);
            folders.setText(narrow?"☰":"Folders");organize.setText(narrow?"⋯":"Organize ▾");views.setText(narrow?"▦":"Views ▾");
            sendTo.setVisibility(narrow?GONE:VISIBLE);
            if(previewEnabled&&((r-l>=dp(560))!=(or-ol>=dp(560))))render();
        });
        back.setOnLongClickListener(v->{historyMenu(back);return true;});forward.setOnLongClickListener(v->{historyMenu(forward);return true;});start(initial);
    }
    @Override void render(){
        if(closed)return;renderTree();updateAddress();String page=page();
        if(!page.equals(previewPage)){previewPage=page;if(preview!=null)preview.clear();}if(!page.startsWith("folder:")&&!page.startsWith("library:")){selected.clear();selectionChanged();}
        if(!page.startsWith("library:"))libraryLoaded="";
        if(page.startsWith("library:")&&LibraryLocations.known(page.substring(8))){showLibrary(page.substring(8));return;}
        if(page.equals("searches")||page.equals("search-all")){if(cancellation!=null)cancellation.cancel();loadedPage="";listing=null;parentInfo=null;selected.clear();selectionChanged();if(sendTo!=null){sendTo.setEnabled(false);sendTo.setAlpha(.5f);}if(page.equals("searches"))savedSearchPage();else globalSearchPage();return;}
        if(page.startsWith("device:")){if(StorageAccess.has(getContext()))replacePage(StorageAccess.page(page.substring(7)));else accessPage(page.substring(7));return;}
        if(page.startsWith("folder:")){if(!loadedPage.equals(page)){if(!restoredSelection)selected.clear();restoredSelection=false;selectionChanged();load(page);return;}if(failed){error();return;}if(listing==null){loadingMessage();return;}fileList();return;}
        if(cancellation!=null)cancellation.cancel();loadedPage="";listing=null;parentInfo=null;selected.clear();selectionChanged();
        if(page.equals("managers")||page.equals("add-apps")){appList(page.equals("add-apps"));return;}
        if(page.equals("libraries")){libraries();return;}
        LinearLayout column=column();if(page.equals("computer")){heading(column,"Computer","Storage and folders on your Android device");drive(column);link(column,"System properties",()->actions.open("@system"));link(column,"Open Control Panel",()->actions.open("@control"));}
        else{heading(column,picker==null?"Your files and folders":picker.foldersOnly()?"Choose a folder":"Choose a file","Browse device storage or a folder you connected.");drive(column);}
        for(String[] favorite:new String[][]{{"documents","Documents"},{"pictures","Pictures"},{"music","Music"},{"videos","Videos"},{"downloads","Downloads"}}){if(!matches(favorite[1]))continue;int before=column.getChildCount();task(column,favorite[0],favorite[1],StorageAccess.has(getContext())?"Browse in Nth Explorer":"Browse device files or connect a folder",()->favorite(favorite[0],favorite[1]));View row=column.getChildAt(before);ContextMenus.bind(row,(x,y)->favoriteMenu(row,favorite[0],favorite[1],x,y));}
        if(picker!=null&&!picker.foldersOnly())link(column,"Libraries",()->navigate("libraries"));
        if(picker==null&&matches("File Managers")){int before=column.getChildCount();task(column,"folderapps","File Managers","Your chosen file manager apps",()->navigate("managers"));View row=column.getChildAt(before);ContextMenus.bind(row,(x,y)->background(row,x,y));}
        if(picker==null||!picker.foldersOnly()){
            heading(column,"Start search files","Files you explicitly linked in Computer.");JSONArray bookmarks=LinkedFiles.list(store);
            for(int i=0;i<bookmarks.length();i++){JSONObject file=bookmarks.optJSONObject(i);if(file==null||!matches(file.optString("name"))||picker!=null&&!picker.accepts(file.optString("mime")))continue;Uri uri=Uri.parse(file.optString("uri"));int before=column.getChildCount();task(column,"documentfile",file.optString("name"),"Linked file",()->files.openDocument(uri,file.optString("mime"),file.optString("name")));View row=column.getChildAt(before);ContextMenus.bind(row,(x,y)->bookmarkMenu(row,file,x,y));}
        }
        if(picker==null)link(column,"Add a file to Start search…",files::linkFile).setContentDescription("Link a file for Start search");
        heading(column,"Linked folders","Choose a folder to browse it here.");JSONArray links=DocumentFolders.links(store);int shown=0;
        for(int i=0;i<links.length();i++){JSONObject folder=links.optJSONObject(i);if(folder==null||!matches(folder.optString("name")))continue;String uri=folder.optString("uri"),name=folder.optString("name","Linked folder");shown++;int before=column.getChildCount();task(column,"files",name,"Browse folder",()->openLinked(uri,name));View row=column.getChildAt(before);ContextMenus.bind(row,(x,y)->linkMenu(row,uri,name,x,y));}
        link(column,"Add a folder…",files::chooseFolder).setContentDescription("Link a folder");status.setText(message.isEmpty()?(page.equals("computer")?storageSummary()+"\n":"")+shown+" linked folder"+(shown==1?"":"s"):message);ScrollView scroll=scrolling(column);ContextMenus.blank(scroll,(x,y)->background(scroll,x,y));body(scroll);
    }
    private void libraries(){
        LinearLayout column=column();
        heading(column,"Libraries","Open your document, picture, music and video folders.");
        LinearLayout tiles=column;
        if(content.getWidth()>=dp(390)){
            tiles=new LinearLayout(getContext());
            column.addView(tiles,new LayoutParams(-1,-2));
        }
        int count=0;
        for(String kind:LibraryLocations.KINDS){
            String label=deviceTitle(kind);
            if(!matches(label))continue;
            if(tiles!=column&&count>0&&count%2==0){
                tiles=new LinearLayout(getContext());
                column.addView(tiles,new LayoutParams(-1,-2));
            }
            LinearLayout row=row(kind,null,label,"Library folder");
            row.setBackground(Aero.selection(getContext()));row.setFocusable(true);
            row.setOnClickListener(v->navigate("library:"+kind));
            tiles.addView(row,tiles==column?new LayoutParams(-1,dp(59)):new LayoutParams(0,dp(59),1));
            ContextMenus.bind(row,(x,y)->favoriteMenu(row,kind,label,x,y));
            count++;
        }
        status.setText("Libraries");
        body(scrolling(column));
    }
    void openLibrary(String kind){if(LibraryLocations.known(kind))navigate("library:"+kind);}
    private void showLibrary(String kind){
        String current="library:"+kind;
        List<String> locations=LibraryLocations.locations(store,kind);
        if(!current.equals(libraryLoaded)){
            if(cancellation!=null)cancellation.cancel();
            cancellation=new CancellationSignal();
            CancellationSignal signal=cancellation;
            libraryLoaded=current;loadedPage="";listing=null;parentInfo=null;
            if(!restoredSelection)selected.clear();
            restoredSelection=false;selectionChanged();
            libraryLoading=true;libraryRows.clear();libraryFolders.clear();unavailableLibraries.clear();
            worker.execute(()->{
                List<LibraryRow> rows=new ArrayList<>();
                Map<String,DocumentFolders.Item> folders=new LinkedHashMap<>();
                Set<String> unavailable=new HashSet<>();
                Set<String> seen=new HashSet<>();
                for(String location:locations){
                    if(signal.isCanceled())return;
                    try{
                        Uri root=DocumentOps.current(location);
                        DocumentFolders.Listing children=DocumentFolders.read(getContext(),location,signal);
                        DocumentFolders.Item info=DocumentOps.stat(getContext(),root,new DocumentOps.Cancel());
                        folders.put(location,info);
                        rows.add(new LibraryRow(location,info.name,null));
                        for(DocumentFolders.Item item:children.items){
                            Uri uri=DocumentFolders.documentUri(location,item.id);
                            String identity=uri.getAuthority()+"|"+item.id;
                            if(seen.add(identity))rows.add(new LibraryRow(location,item.name,new DocumentOps.Ref(uri,root,item)));
                        }
                        if(children.limited)rows.add(new LibraryRow(null,"Showing the first 2,000 items in this folder.",null));
                    }catch(Exception unavailableFolder){unavailable.add(location);}
                }
                main.post(()->{
                    if(closed||signal.isCanceled()||!current.equals(page()))return;
                    libraryLoading=false;libraryRows.addAll(rows);libraryFolders.putAll(folders);unavailableLibraries.addAll(unavailable);
                    showLibrary(kind);
                });
            });
        }
        LinearLayout panel=Aero.column(getContext());
        LinearLayout header=column();
        heading(header,deviceTitle(kind)+" Library","Browse included folders without moving their files.");
        if(picker==null){
            link(header,"Manage library locations…",()->manageLibrary(kind));
            if(!locations.isEmpty()&&!libraryLoading){
                String destination=locations.get(0);
                Uri uri=libraryUri(destination);
                if(DocumentOps.writable(getContext(),uri,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,libraryFolders.get(destination))){
                    link(header,"New text document in default folder…",()->newText(uri));
                    if(!ExplorerClipboard.files.isEmpty())link(header,"Paste into default folder",()->pasteInto(uri));
                }
            }
        }
        if(locations.isEmpty()){
            link(header,"Open "+deviceTitle(kind)+" folder",()->openFavorite(kind));
            link(header,"Connect a folder…",files::chooseFolder);
        }
        if(libraryLoading)header.addView(Aero.text(getContext(),"Loading library…",13,Aero.INK));
        else if(!unavailableLibraries.isEmpty())header.addView(Aero.text(getContext(),"Some locations are unavailable. Review their access or remove them in Manage library locations.",12,Aero.INK));
        List<LibraryRow> visible=new ArrayList<>();
        for(LibraryRow row:libraryRows)if(row.ref==null||visibleItem(row.ref.item))visible.add(row);
        ListView list=new ListView(getContext());list.setDividerHeight(0);list.setContentDescription("Library contents");
        list.addHeaderView(header,null,false);
        list.setAdapter(new BaseAdapter(){
            public int getCount(){return visible.size();}
            public Object getItem(int index){return visible.get(index);}
            public long getItemId(int index){return index;}
            public View getView(int index,View recycled,ViewGroup parent){
                LibraryRow item=visible.get(index);
                if(item.ref==null){
                    TextView label=command(item.name+(item.page!=null&&locations.indexOf(item.page)==0?"  • Default save location":""),
                            ()->{if(item.page!=null)navigate(item.page);});
                    label.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);label.setMinHeight(dp(36));
                    return label;
                }
                DocumentOps.Ref ref=item.ref;
                LinearLayout entry=compactRow(ref.item);
                if(selectionMode&&!ref.item.directory()){
                    CheckBox check=Aero.check(getContext(),"",selected.containsKey(ref.uri.toString()));
                    check.setContentDescription("Select "+item.name);
                    check.setOnCheckedChangeListener((view,checked)->select(ref,checked));
                    entry.addView(check,new LayoutParams(dp(38),dp(38)));
                }
                entry.setOnClickListener(v->openLibraryItem(item));
                ContextMenus.bind(entry,(x,y)->{
                    String child=ref.item.directory()?libraryChild(item):null;
                    fileMenu(entry,ref,x,y,child);
                });
                return entry;
            }
        });
        list.setOnItemClickListener((parent,view,index,id)->{
            int offset=index-list.getHeaderViewsCount();
            if(offset>=0)openLibraryItem(visible.get(offset));
        });
        status.setText(locations.size()+" included location"+(locations.size()==1?"":"s"));
        addReadingPane(panel,list);
        body(panel);
    }
    private void togglePreview(){
        previewEnabled=!previewEnabled;
        if(!previewEnabled&&preview!=null)preview.clear();
        render();
    }
    private void addReadingPane(LinearLayout panel,View listing){
        if(!previewEnabled){panel.addView(listing,new LayoutParams(-1,0,1));return;}
        if(preview==null)preview=new ExplorerPreview(getContext(),this::togglePreview);
        if(preview.getParent() instanceof ViewGroup)((ViewGroup)preview.getParent()).removeView(preview);
        if(getWidth()>=dp(560)){
            LinearLayout split=new LinearLayout(getContext());
            split.addView(listing,new LayoutParams(0,-1,1));
            split.addView(preview,new LayoutParams(dp(230),-1));
            panel.addView(split,new LayoutParams(-1,0,1));
        }else{
            panel.addView(listing,new LayoutParams(-1,0,1));
            panel.addView(preview,new LayoutParams(-1,0,.56f));
        }
    }
    private Uri libraryUri(String page){
        try{return DocumentOps.current(page);}
        catch(JSONException invalid){return null;}
    }
    private String libraryChild(LibraryRow row){
        try{return DocumentFolders.childPage(row.page,row.ref.item);}
        catch(JSONException invalid){return row.page;}
    }
    private void openLibraryItem(LibraryRow row){
        if(row.ref==null){if(row.page!=null)navigate(row.page);return;}
        DocumentOps.Ref ref=row.ref;
        if(ref.item.directory())navigate(libraryChild(row));
        else if(selectionMode)select(ref,!selected.containsKey(ref.uri.toString()));
        else if(previewEnabled)preview.show(ref,()->openFile(ref));
        else openFile(ref);
    }
    private void manageLibrary(String kind){
        List<String> current=LibraryLocations.locations(store,kind);
        List<String> labels=new ArrayList<>();
        List<Runnable> actions=new ArrayList<>();
        List<String> candidates=new ArrayList<>();
        if(StorageAccess.has(getContext()))candidates.add(StorageAccess.page(kind));
        JSONArray roots=DocumentFolders.links(store);
        for(int i=0;i<roots.length();i++){
            JSONObject folder=roots.optJSONObject(i);
            if(folder!=null)try{candidates.add(DocumentFolders.rootPage(folder.getString("uri"),folder.optString("name","Linked folder")));}catch(Exception invalid){}
        }
        for(String page:candidates)if(!current.contains(page)){
            labels.add("Include "+title(page));
            actions.add(()->includeInLibrary(kind,page));
        }
        for(int i=0;i<current.size();i++){
            String page=current.get(i);
            Uri uri=libraryUri(page);
            if(i>0&&DocumentOps.writable(getContext(),uri,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,libraryFolders.get(page))){
                labels.add("Set default save location to "+title(page));
                actions.add(()->{List<String> locations=LibraryLocations.locations(store,kind);locations.remove(page);locations.add(0,page);saveLibrary(kind,locations);});
            }
            labels.add("Remove "+title(page)+" from library");
            actions.add(()->{List<String> locations=LibraryLocations.locations(store,kind);locations.remove(page);saveLibrary(kind,locations);});
        }
        labels.add("Connect another folder…");actions.add(files::chooseFolder);
        Aero.dialog(getContext()).setTitle(deviceTitle(kind)+" Library locations")
                .setItems(labels.toArray(new String[0]),(dialog,index)->actions.get(index).run()).setNegativeButton("Cancel",null).show();
    }
    private void includeInLibrary(String kind,String page){
        List<String> locations=LibraryLocations.locations(store,kind);
        if(locations.contains(page)){toast("This folder is already in the library.");return;}
        locations.add(page);saveLibrary(kind,locations);
    }
    private void saveLibrary(String kind,List<String> locations){
        try{LibraryLocations.save(store,kind,locations);libraryLoaded="";render();}
        catch(JSONException invalid){toast(invalid.getMessage());}
    }
    private void chooseLibrary(String location){
        String[] labels=new String[LibraryLocations.KINDS.length];
        for(int i=0;i<labels.length;i++)labels[i]=deviceTitle(LibraryLocations.KINDS[i]);
        Aero.dialog(getContext()).setTitle("Include in library")
                .setItems(labels,(dialog,index)->includeInLibrary(LibraryLocations.KINDS[index],location)).setNegativeButton("Cancel",null).show();
    }
    private void drive(LinearLayout column){int index=column.getChildCount();task(column,"drive","Internal storage",storageSummary(),()->openDevice("internal"));View row=column.getChildAt(index);ContextMenus.bind(row,(x,y)->linkMenu(row,StorageAccess.tree().toString(),"Internal storage",x,y));}
    private String storageSummary(){try{StatFs disk=new StatFs(Environment.getExternalStorageDirectory().getPath());return android.text.format.Formatter.formatFileSize(getContext(),disk.getAvailableBytes())+" free of "+android.text.format.Formatter.formatFileSize(getContext(),disk.getTotalBytes());}catch(RuntimeException ex){return "View device storage in Android";}}
    private boolean matches(String name){return query().isEmpty()||name.toLowerCase(Locale.getDefault()).contains(query());}
    private String title(String page){if(page.startsWith("library:"))return deviceTitle(page.substring(8))+" Library";if(page.startsWith("device:"))return deviceTitle(page.substring(7));if(page.startsWith("folder:"))try{JSONArray path=DocumentFolders.path(page).getJSONArray("crumbs");StringBuilder text=new StringBuilder("Computer");for(int i=0;i<path.length();i++)text.append(" › ").append(path.getJSONObject(i).optString("name"));return text.toString();}catch(JSONException ignored){}return page.equals("libraries")?"Libraries":page.equals("search-all")?"Search Results":page.equals("searches")?"Searches":page.equals("computer")?"Computer":page.equals("managers")?"File Managers":page.equals("add-apps")?"Add file manager apps":"Files";}
    private String prefix(JSONObject path,int last)throws JSONException{JSONArray result=new JSONArray(),source=path.getJSONArray("crumbs");for(int i=0;i<=last;i++)result.put(source.getJSONObject(i));return "folder:"+new JSONObject().put("tree",path.getString("tree")).put("crumbs",result);}
    private void updateCaption(){String value=title(page());if(page().startsWith("folder:"))try{JSONArray crumbs=DocumentFolders.path(page()).getJSONArray("crumbs");value=crumbs.getJSONObject(crumbs.length()-1).optString("name",value);}catch(JSONException ignored){}files.caption(value);}
    private void updateAddress(){updateCaption();showDetailIcon(page().equals("computer")?"computer":page().equals("searches")||page().equals("search-all")?"search":"files");address.setText(title(page()));crumbs.removeAllViews();HorizontalScrollView scroll=new HorizontalScrollView(getContext());scroll.setHorizontalScrollBarEnabled(false);LinearLayout segments=new LinearLayout(getContext());segments.setGravity(Gravity.CENTER_VERTICAL);scroll.addView(segments);crumbs.addView(scroll,new LayoutParams(0,-1,1));crumb(segments,page().equals("home")?"Files":"Computer",page().equals("home")?"home":"computer");if(page().startsWith("folder:"))try{JSONObject path=DocumentFolders.path(page());JSONArray values=path.getJSONArray("crumbs");for(int i=0;i<values.length();i++)crumb(segments,values.getJSONObject(i).optString("name"),prefix(path,i));}catch(JSONException ignored){}else if(!page().equals("computer")&&!page().equals("home")){if(page().equals("add-apps"))crumb(segments,"File Managers","managers");crumb(segments,title(page()),page());}scroll.post(()->scroll.fullScroll(View.FOCUS_RIGHT));TextView more=command("▾",this::addressMenu);more.setContentDescription("Address and history options");crumbs.addView(more,new LayoutParams(dp(30),-1));crumbs.setOnLongClickListener(v->{editAddress();return true;});}
    private void crumb(LinearLayout row,String name,String destination){if(row.getChildCount()>0)row.addView(Aero.text(getContext(),"›",15,0xff677e8c));TextView link=command(name,()->navigate(destination));link.setContentDescription("Navigate to "+name);row.addView(link,new LayoutParams(-2,-1));}
    private void addressMenu(){DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Address context menu");menu.item("Edit address…","files",this::editAddress);menu.item("Navigation history…","clock",()->historyMenu(crumbs));menu.separator();for(String target:historyPages())menu.item(title(target),"files",()->navigate(target));ContextMenus.centered(crumbs,(x,y)->ContextMenus.show(menu,crumbs,x,y));}
    private void historyMenu(View anchor){DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Navigation history");List<String> pages=historyPages();for(int i=pages.size()-1;i>=0;i--){int index=i;menu.item(title(pages.get(i)),"files",()->visitHistory(index));}ContextMenus.centered(anchor,(x,y)->ContextMenus.show(menu,anchor,x,y));}
    private void editAddress(){EditText input=edit("Folder address",title(page()).replace(" › ","/"));android.app.AlertDialog dialog=Aero.dialog(getContext()).setTitle("Go to folder").setView(input).setPositiveButton("Go",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(v->{Aero.styleDialog(getContext(),dialog);dialog.getButton(-1).setOnClickListener(w->{String path=input.getText().toString().trim();work("Opening folder…",cancel->{String destination=resolveAddress(path,cancel);main.post(()->{if(!closed)navigate(destination);});return "";},dialog::dismiss);});});dialog.show();}
    String resolveAddress(String value,DocumentOps.Cancel cancel)throws Exception{String clean=value.trim();if(clean.equalsIgnoreCase("Computer"))return "computer";if(clean.equalsIgnoreCase("Files"))return "home";String[] parts=clean.replace(" › ","/").replace('\\','/').split("/");int first=parts.length>0&&parts[0].equalsIgnoreCase("Computer")?1:0;if(first>=parts.length)throw new IllegalArgumentException("Enter a linked folder name.");JSONArray roots=navigationRoots();String page=null;for(int i=0;i<roots.length();i++){JSONObject root=roots.optJSONObject(i);if(root!=null&&root.optString("name").equalsIgnoreCase(parts[first])){page=DocumentFolders.rootPage(root.getString("uri"),root.getString("name"));break;}}if(page==null)throw new IllegalArgumentException("Start with the name of a folder linked in Computer.");for(int i=first+1;i<parts.length;i++){if(parts[i].isEmpty()||parts[i].equals("."))continue;if(parts[i].equals(".."))throw new IllegalArgumentException("Use the Up button to navigate to parent folders.");DocumentFolders.Listing list=DocumentFolders.read(getContext(),page,cancel.signal);DocumentFolders.Item match=null;for(DocumentFolders.Item item:list.items)if(item.directory()&&item.name.equalsIgnoreCase(parts[i])){match=item;break;}if(match==null)throw new IllegalArgumentException("Folder not found: "+parts[i]);page=DocumentFolders.childPage(page,match);}return page;}
    void openLinked(String uri,String name){try{if(!DocumentsContract.isTreeUri(Uri.parse(uri)))throw new IllegalArgumentException();navigate(DocumentFolders.rootPage(uri,name));}catch(RuntimeException ex){toast("Choose this folder again to restore access.");}}
    void folderAdded(String uri,String name){if(!pendingFavorite.isEmpty()){try{JSONObject aliases=favorites();aliases.put(pendingFavorite,uri);store.prefs.edit().putString("explorer_favorites",aliases.toString()).apply();}catch(JSONException ignored){}pendingFavorite="";}openLinked(uri,name);}
    private JSONObject favorites(){try{return new JSONObject(store.prefs.getString("explorer_favorites","{}"));}catch(JSONException ex){return new JSONObject();}}
    private void favorite(String kind,String label){if(kind.equals("documents")&&StorageAccess.has(getContext())){openDevice(kind);return;}String preferred=favorites().optString(kind);JSONArray roots=DocumentFolders.links(store);if(!preferred.isEmpty())for(int i=0;i<roots.length();i++){JSONObject root=roots.optJSONObject(i);if(root!=null&&root.optString("uri").equals(preferred)){openLinked(root.optString("uri"),root.optString("name"));return;}}openDevice(kind);}
    void openFavorite(String kind){favorite(kind,deviceTitle(kind));}
    void openDevice(String kind){if(!kind.equals("internal"))StorageAccess.directory(kind);navigate(StorageAccess.has(getContext())?StorageAccess.page(kind):"device:"+kind);}
    private String deviceTitle(String kind){if(kind.equals("internal"))return "Internal storage";for(String[] entry:StorageAccess.FAVORITES)if(entry[0].equals(kind))return entry[1];return "Device files";}
    private void accessPage(String kind){LinearLayout column=column();heading(column,deviceTitle(kind),"Keep your device files in Nth Explorer's familiar windows.");column.addView(Aero.text(getContext(),"Allow file browsing to open Internal storage and its standard folders here. Android protects system files and other apps’ private data. Your files stay on this device.",14,Aero.INK));link(column,"Enable file browsing…",files::requestDeviceAccess).setContentDescription("Enable device file browsing");link(column,"Choose a folder instead…",()->{pendingFavorite=kind.equals("internal")?"":kind;files.chooseFolder();});link(column,"Back to Computer",()->navigate("computer"));status.setText("File access is optional");body(scrolling(column));}
    private JSONArray navigationRoots(){JSONArray roots=new JSONArray();if(StorageAccess.has(getContext())){JSONObject root=new JSONObject();GadgetLayout.put(root,"uri",StorageAccess.tree().toString());GadgetLayout.put(root,"name","Internal storage");roots.put(root);}JSONArray linked=DocumentFolders.links(store);for(int i=0;i<linked.length();i++)if(linked.optJSONObject(i)!=null)roots.put(linked.optJSONObject(i));return roots;}
    private void favoriteMenu(View anchor,String kind,String label,float x,float y){DesktopContextMenu menu=menu("Folder context menu");menu.item("Open","files",()->favorite(kind,label));menu.item("Use device "+label,"drive",()->{JSONObject aliases=favorites();aliases.remove(kind);store.prefs.edit().putString("explorer_favorites",aliases.toString()).apply();openDevice(kind);});menu.item("Choose a different folder…","files",()->{pendingFavorite=kind;files.chooseFolder();});menu.item("File access…","security",files::requestDeviceAccess);show(menu,anchor,x,y);}
    private void load(String page){if(cancellation!=null)cancellation.cancel();loadedPage=page;loadFolderStyle(page);listing=null;failed=false;parentInfo=null;cancellation=new CancellationSignal();CancellationSignal signal=cancellation;int request=++generation;loadingMessage();worker.execute(()->{DocumentFolders.Listing result=null;DocumentFolders.Item info=null;try{result=DocumentFolders.read(getContext(),page,signal);info=DocumentOps.stat(getContext(),DocumentOps.current(page),new DocumentOps.Cancel());}catch(Exception ignored){}DocumentFolders.Listing found=result;DocumentFolders.Item parent=info;main.post(()->{if(closed||signal.isCanceled()||request!=generation||!page.equals(page()))return;listing=found;parentInfo=parent;failed=found==null;if(failed)error();else fileList();});});}
    private void loadingMessage(){status.setText("Opening "+title(page())+"…");LinearLayout column=column();heading(column,"Opening folder…","");column.addView(new ProgressBar(getContext()),new LayoutParams(dp(36),dp(36)));loading(scrolling(column));}
    private void error(){Uri current=parent();if(StorageAccess.ours(current)&&!StorageAccess.has(getContext())){String kind="internal";try{String id=DocumentsContract.getDocumentId(current);for(String[] entry:StorageAccess.FAVORITES)if(id.equals("primary:"+entry[2]))kind=entry[0];}catch(RuntimeException ignored){}accessPage(kind);return;}LinearLayout column=column();heading(column,"This folder is unavailable","It may not exist, may have moved, or access may have been removed.");if(StorageAccess.ours(current)){try{String id=DocumentsContract.getDocumentId(current);for(String[] entry:StorageAccess.FAVORITES)if(id.equals("primary:"+entry[2]))link(column,"Create "+entry[1]+" folder",()->work("Creating folder…",cancel->{DocumentOps.createFolder(getContext(),StorageAccess.document(""),entry[2],cancel);return "Folder created.";},null));}catch(RuntimeException ignored){}link(column,"Review file access…",files::requestDeviceAccess);link(column,"Internal storage",()->openDevice("internal"));}else link(column,"Choose a folder again…",files::chooseFolder);link(column,"Return to Computer",()->navigate("computer"));status.setText("Folder unavailable");body(scrolling(column));}
    private Uri parent(){try{return DocumentOps.current(page());}catch(Exception ex){return null;}}
    Uri availableFolder(){return listing!=null&&page().equals(loadedPage)?parent():null;}
    private DocumentOps.Ref ref(DocumentFolders.Item item){try{return new DocumentOps.Ref(DocumentFolders.documentUri(page(),item.id),parent(),item);}catch(JSONException ex){throw new IllegalStateException(ex);}}
    private boolean canCreate(){Uri current=parent();return current!=null&&DocumentOps.writable(getContext(),current,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,parentInfo);}
    private boolean visibleItem(DocumentFolders.Item item){return matches(item.name)&&(showHidden||!item.name.startsWith("."))&&(!recentOnly||!item.directory()&&item.modified>=System.currentTimeMillis()-30L*86400000L)&&(picker==null||item.directory()||picker.accepts(item.mime)&&(item.flags&DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)==0);}
    private void fileList(){
        updateCaption();RecentFiles.folder(store,page(),title(page()).contains(" › ")?title(page()).substring(title(page()).lastIndexOf(" › ")+3):title(page()));
        ArrayList<DocumentFolders.Item> visible=new ArrayList<>();for(DocumentFolders.Item item:listing.items)if(visibleItem(item))visible.add(item);Set<String> shown=new HashSet<>();for(DocumentFolders.Item item:visible)shown.add(ref(item).uri.toString());if(selected.keySet().retainAll(shown))selectionChanged();
        java.text.Collator collator=java.text.Collator.getInstance();visible.sort((a,b)->{if(a.directory()!=b.directory())return a.directory()?-1:1;int order=sort.equals("Size")?Long.compare(a.size,b.size):sort.equals("Date modified")?Long.compare(a.modified,b.modified):sort.equals("Type")?collator.compare(fileType(FileLaunch.mime(a.name,a.mime)),fileType(FileLaunch.mime(b.name,b.mime))):collator.compare(a.name,b.name);if(order==0)order=collator.compare(a.name,b.name);return descending?-order:order;});
        updateDetails(visible);List<Object> rows=new ArrayList<>();boolean grouping=!groupBy.equals("None");if(grouping){Map<String,List<DocumentFolders.Item>> groups=new LinkedHashMap<>();for(DocumentFolders.Item item:visible)groups.computeIfAbsent(group(item),key->new ArrayList<>()).add(item);for(Map.Entry<String,List<DocumentFolders.Item>> entry:groups.entrySet()){rows.add(entry.getKey()+" ("+entry.getValue().size()+")");if(!collapsedGroups.contains(entry.getKey()))rows.addAll(entry.getValue());}}else rows.addAll(visible);
        BaseAdapter adapter=new BaseAdapter(){public int getCount(){return rows.size();}public Object getItem(int p){return rows.get(p);}public long getItemId(int p){return p;}public int getViewTypeCount(){return 2;}public int getItemViewType(int p){return rows.get(p) instanceof String?1:0;}public boolean isEnabled(int p){return true;}public View getView(int position,View recycled,ViewGroup parent){Object value=rows.get(position);if(value instanceof String){String label=(String)value;TextView header=Aero.text(getContext(),"▾  "+label,12,0xff28638b);header.setPadding(dp(9),dp(6),dp(5),dp(5));header.setBackground(Aero.panel(getContext(),0xfff7fbfe,0xffe5eef6,0,0xffd7e3ec));header.setContentDescription("Group "+label);return header;}DocumentFolders.Item item=(DocumentFolders.Item)value;DocumentOps.Ref reference=ref(item);String mime=FileLaunch.mime(item.name,item.mime);LinearLayout row=details?detailRow(item):viewMode.equals("List")||viewMode.equals("Small icons")?compactRow(item):viewMode.contains("icons")?iconRow(item):row(icon(mime,item.directory()),null,item.name,metadata(item));if(!item.directory()&&PhotoImage.picture(item.name,item.mime)&&store.prefs.getBoolean("explorer_thumbnails",true))thumbnail(row,reference);boolean checked=selected.containsKey(reference.uri.toString());if(selectionMode&&(picker==null||!item.directory())){CheckBox box=Aero.check(getContext(),"",checked);box.setContentDescription("Select "+item.name);box.setOnCheckedChangeListener((v,check)->select(reference,check));row.addView(box,new LayoutParams(dp(34),dp(36)));}if(checked)row.setBackground(Aero.panel(getContext(),0xffe0f2ff,0xffc4e2f6,2,0xff7aafce));if(ExplorerClipboard.cut&&ExplorerClipboard.files.stream().anyMatch(f->DocumentOps.same(f.uri,reference.uri)))row.setAlpha(.5f);row.setOnClickListener(v->activate(reference));ContextMenus.bind(row,(x,y)->fileMenu(row,reference,x,y));bindFileDrag(row,reference);return row;}};
        AbsListView list;if(viewMode.equals("Large icons")||viewMode.equals("Medium icons")||viewMode.equals("Small icons")){GridView grid=new GridView(getContext());grid.setNumColumns(GridView.AUTO_FIT);grid.setColumnWidth(dp(viewMode.equals("Large icons")?104:viewMode.equals("Small icons")?140:82));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setAdapter(adapter);list=grid;}else{ListView table=new ListView(getContext());table.setDividerHeight(0);table.setFastScrollEnabled(visible.size()>100);table.setAdapter(adapter);list=table;}list.setSelector(Aero.selection(getContext()));list.setContentDescription("Folder contents");
        list.setOnItemClickListener((parent,view,position,id)->{Object value=rows.get(position);if(value instanceof DocumentFolders.Item)activate(ref((DocumentFolders.Item)value));else{String label=(String)value,key=label.substring(0,label.lastIndexOf(" ("));if(!collapsedGroups.add(key))collapsedGroups.remove(key);fileList();}});list.setOnItemLongClickListener((parent,view,position,id)->{Object value=rows.get(position);if(!(value instanceof DocumentFolders.Item))return false;ContextMenus.centered(view,(x,y)->fileMenu(view,ref((DocumentFolders.Item)value),x,y));return true;});ContextMenus.blank(list,(x,y)->background(list,x,y));
        LinearLayout panel=Aero.column(getContext());if(picker==null&&selectionMode){TextView done=Aero.button(getContext(),"Done selecting",()->{selected.clear();selectionMode=false;selectionChanged();fileList();});done.setContentDescription("Done selecting files");panel.addView(done,new LayoutParams(-1,dp(36)));}if(details)panel.addView(columns(),new LayoutParams(-1,dp(28)));
        if(visible.isEmpty()){TextView empty=Aero.text(getContext(),query().isEmpty()?"This folder is empty":"No matching items",14,0xff556a78);empty.setGravity(Gravity.CENTER);empty.setContentDescription("Empty folder");panel.addView(empty,new LayoutParams(-1,dp(52)));}addReadingPane(panel,list);ContextMenus.blank(panel,(x,y)->background(panel,x,y));if(picker==null)bindDrop(panel,parent(),canCreate());body(panel);
    }
    private void bindFileDrag(ViewGroup row,DocumentOps.Ref reference){
        if(picker!=null)return;ViewGroup iconParent=row;if(details&&row.getChildAt(0) instanceof ViewGroup)iconParent=(ViewGroup)row.getChildAt(0);View icon=iconParent.getChildAt(0);if(icon!=null){icon.setContentDescription("Drag "+reference.item.name);icon.setOnClickListener(v->activate(reference));icon.setOnContextClickListener(v->{ContextMenus.centered(icon,(x,y)->fileMenu(row,reference,x,y));return true;});icon.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){fileMenu(row,reference,event.getRawX(),event.getRawY());return true;}return false;});icon.setOnLongClickListener(v->{if(busy)return true;FileDrag drag=new FileDrag(targets(reference),page(),this);return v.startDragAndDrop(ClipData.newPlainText("Nth files",reference.item.name),new View.DragShadowBuilder(v),drag,0);});}if(reference.item.directory())bindDrop(row,reference.uri,DocumentOps.writable(getContext(),reference.uri,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,reference.item));
    }
    private void bindDrop(View target,Uri destination,boolean writable){target.setOnDragListener((view,event)->{if(!(event.getLocalState() instanceof FileDrag)||!writable||busy)return false;FileDrag drag=(FileDrag)event.getLocalState();switch(event.getAction()){case DragEvent.ACTION_DRAG_ENTERED:view.setForeground(Aero.panel(getContext(),0x183f9ed9,0x183f9ed9,2,0xff388ac0));break;case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:view.setForeground(null);break;case DragEvent.ACTION_DROP:view.setForeground(null);confirmDrop(drag,destination);break;}return true;});}
    private void confirmDrop(FileDrag drag,Uri destination){if(destination==null||busy||drag.refs.isEmpty())return;boolean move=drag.refs.stream().allMatch(r->DocumentOps.writable(getContext(),r.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,r.item));android.app.AlertDialog dialog=Aero.dialog(getContext()).setTitle("Drop "+drag.refs.size()+" item"+(drag.refs.size()==1?"":"s")).setMessage("Copy or move into this folder? Existing names are kept separately. Moves remove originals only after verification.").setPositiveButton("Copy here",(d,w)->drop(drag,destination,false)).setNegativeButton("Cancel",null).setNeutralButton("Move here",(d,w)->drop(drag,destination,true)).create();dialog.setOnShowListener(v->{Aero.styleDialog(getContext(),dialog);dialog.getButton(-3).setEnabled(move);});dialog.show();}
    private void drop(FileDrag drag,Uri destination,boolean move){undoCopy=null;work(move?"Moving items…":"Copying items…",cancel->DocumentOps.paste(getContext(),new ArrayList<>(drag.refs),destination,move,cancel,text->main.post(()->{if(!closed)status.setText(text);}),receipt->main.post(()->{if(!closed)undoCopy=receipt;}),this::copyProgress),null);}
    private void folderLink(DocumentOps.Ref ref,String directoryPage,boolean desktop){try{String destination=directoryPage!=null?directoryPage:DocumentFolders.childPage(page(),ref.item);if(desktop)files.desktopFolder(destination,ref.item.name);else files.openFolder(destination);}catch(JSONException ex){toast("Folder no longer available");}}
    private void thumbnail(ViewGroup row,DocumentOps.Ref ref){
        ViewGroup parent=row;int index=0;if(details&&row.getChildAt(0) instanceof ViewGroup)parent=(ViewGroup)row.getChildAt(0);
        View old=parent.getChildAt(index);if(!(old instanceof AeroIcon))return;ViewGroup.LayoutParams size=old.getLayoutParams();parent.removeViewAt(index);ImageView image=new ImageView(getContext());image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImageDrawable(new android.graphics.drawable.ColorDrawable(0xffd6e6ee));image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);parent.addView(image,index,size);thumbnails.bind(image,ref);
    }
    private LinearLayout columns(){LinearLayout header=new LinearLayout(getContext());header.setBackground(Aero.panel(getContext(),0xffffffff,0xffe8edf2,0,0xffccd7df));String[] labels={"Name","Date modified","Type","Size"};float[] weights={.46f,.24f,.18f,.12f};for(int i=0;i<labels.length;i++){String label=labels[i];TextView column=command(label+(sort.equals(label)?descending?" ▾":" ▴":""),()->sortColumn(label));column.setTextSize(10);column.setSingleLine(true);column.setEllipsize(android.text.TextUtils.TruncateAt.END);column.setPadding(dp(3),0,dp(1),0);if(label.equals("Date modified"))column.setText("Modified"+(sort.equals(label)?descending?" ▾":" ▴":""));column.setTooltipText(label);column.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);column.setContentDescription("Sort by "+label);header.addView(column,new LayoutParams(0,-1,weights[i]));}return header;}
    private void sortColumn(String label){if(sort.equals(label))descending=!descending;else{sort=label;descending=false;}saveFolderStyle();fileList();}
    private TextView cell(String value,float weight,LinearLayout row){TextView text=Aero.text(getContext(),value,11,0xff354a59);text.setGravity(Gravity.CENTER_VERTICAL);text.setMaxLines(2);text.setEllipsize(android.text.TextUtils.TruncateAt.END);text.setPadding(dp(5),0,dp(3),0);row.addView(text,new LayoutParams(0,-1,weight));return text;}
    private LinearLayout detailRow(DocumentFolders.Item item){LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(43));LinearLayout name=new LinearLayout(getContext());name.setGravity(Gravity.CENTER_VERTICAL);name.setPadding(dp(6),0,0,0);name.addView(new AeroIcon(getContext(),icon(FileLaunch.mime(item.name,item.mime),item.directory())),new LayoutParams(dp(23),dp(26)));TextView title=Aero.text(getContext(),item.name,12,Aero.INK);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(dp(5),dp(5),dp(2),dp(5));name.addView(title,new LayoutParams(0,-2,1));row.addView(name,new LayoutParams(0,-1,.46f));cell(item.modified>0?android.text.format.DateFormat.getDateFormat(getContext()).format(new Date(item.modified)):"—",.24f,row);cell(item.directory()?"File folder":fileType(FileLaunch.mime(item.name,item.mime)),.18f,row);cell(item.directory()?"":item.size>=0?android.text.format.Formatter.formatShortFileSize(getContext(),item.size):"—",.12f,row);row.setContentDescription(item.name);return row;}
    private LinearLayout compactRow(DocumentFolders.Item item){LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(7),dp(4),dp(7),dp(4));int size=viewMode.equals("Small icons")?16:20;row.addView(new AeroIcon(getContext(),icon(FileLaunch.mime(item.name,item.mime),item.directory())),new LayoutParams(dp(size),dp(size)));TextView text=Aero.text(getContext(),item.name,12,Aero.INK);text.setPadding(dp(7),0,0,0);text.setSingleLine(true);text.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(text,new LayoutParams(0,dp(28),1));row.setContentDescription(item.name);return row;}
    private LinearLayout iconRow(DocumentFolders.Item item){LinearLayout row=Aero.column(getContext());row.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);row.setPadding(dp(5),dp(7),dp(5),dp(5));int size=viewMode.equals("Large icons")?56:38;row.addView(new AeroIcon(getContext(),icon(FileLaunch.mime(item.name,item.mime),item.directory())),new LayoutParams(dp(size),dp(size)));TextView label=Aero.text(getContext(),item.name,12,Aero.INK);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(label,new LayoutParams(-1,dp(36)));row.setContentDescription(item.name);return row;}
    private String group(DocumentFolders.Item item){if(item.directory())return "Folders";switch(groupBy){case "Type":return fileType(FileLaunch.mime(item.name,item.mime));case "Size":return item.size<0?"Unknown size":item.size==0?"Empty":item.size<1048576?"Less than 1 MB":item.size<104857600?"1–100 MB":"Over 100 MB";case "Date modified":long age=System.currentTimeMillis()-item.modified;return item.modified<=0?"Unknown date":age<86400000L?"Today":age<172800000L?"Yesterday":age<604800000L?"This week":"Earlier";default:return item.name.isEmpty()?"Other":item.name.substring(0,1).toUpperCase(Locale.getDefault());}}
    private void showDetailIcon(String kind){if(detailIcon!=null&&!kind.equals(detailIcon.getTag())){detailIcon.setTag(kind);detailIcon.removeAllViews();detailIcon.addView(new AeroIcon(getContext(),kind),new FrameLayout.LayoutParams(-1,-1));}}
    private void updateDetails(List<DocumentFolders.Item> visible){String text=busy?message:!message.isEmpty()?message:(listing.limited?"First 2,000 items • ":"")+visible.size()+" items"+(canCreate()?"":" • Read-only");String kind="files";if(selected.size()==1){DocumentFolders.Item item=selected.values().iterator().next().item;kind=icon(FileLaunch.mime(item.name,item.mime),item.directory());text=item.name+"\n"+metadata(item)+(item.modified>0?" • "+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date(item.modified)):"");}else if(selected.size()>1){long bytes=0;for(DocumentOps.Ref ref:selected.values())if(!ref.item.directory()&&ref.item.size>0)bytes+=ref.item.size;text=selected.size()+" items selected\n"+android.text.format.Formatter.formatFileSize(getContext(),bytes)+" in selected files";}else text+="\n"+(recentOnly?"Changed in the past 30 days • ":"")+title(page());status.setText(text);showDetailIcon(kind);if(sendTo!=null){sendTo.setEnabled(!selected.isEmpty()&&selected.values().stream().noneMatch(r->r.item.directory()));sendTo.setAlpha(sendTo.isEnabled()?1:.5f);}}
    private String metadata(DocumentFolders.Item item){String type=item.directory()?"File folder":fileType(FileLaunch.mime(item.name,item.mime));String result=item.directory()?type:item.size<0?type:android.text.format.Formatter.formatFileSize(getContext(),item.size)+"  •  "+type;if(details&&item.modified>0)result+="  •  "+android.text.format.DateFormat.getDateFormat(getContext()).format(new Date(item.modified));return result;}
    private void openFile(DocumentOps.Ref ref){if(picker==null&&PhotoImage.picture(ref.item.name,ref.item.mime)){JSONObject context=new JSONObject();GadgetLayout.put(context,"page",page());GadgetLayout.put(context,"query",query());GadgetLayout.put(context,"sort",sort);GadgetLayout.put(context,"descending",descending);files.openPicture(ref.uri,ref.item.mime,ref.item.name,context);}else files.openDocument(ref.uri,ref.item.mime,ref.item.name);}
    private void activate(DocumentOps.Ref reference){if(selectionMode&&!reference.item.directory()){select(reference,!selected.containsKey(reference.uri.toString()));return;}if(reference.item.directory()){try{navigate(DocumentFolders.childPage(page(),reference.item));}catch(JSONException ex){error();}}else if(previewEnabled)preview.show(reference,()->openFile(reference));else openFile(reference);}
    private void select(DocumentOps.Ref reference,boolean value){message="";if(value)selected.put(reference.uri.toString(),reference);else selected.remove(reference.uri.toString());selectionChanged();if(page().startsWith("library:"))showLibrary(page().substring(8));else if(listing!=null)fileList();}
    private void selectionChanged(){if(sendTo!=null){boolean enabled=page().startsWith("folder:")&&!selected.isEmpty()&&selected.values().stream().noneMatch(r->r.item.directory());sendTo.setEnabled(enabled);sendTo.setAlpha(enabled?1:.5f);}if(picker!=null){List<Uri> uris=new ArrayList<>();for(DocumentOps.Ref ref:selected.values())if(!ref.item.directory()&&picker.accepts(ref.item.mime))uris.add(ref.uri);picker.changed(uris);}}
    private List<DocumentOps.Ref> targets(DocumentOps.Ref reference){return selected.containsKey(reference.uri.toString())?new ArrayList<>(selected.values()):Collections.singletonList(reference);}
    private DesktopContextMenu menu(String description){if(contextMenu!=null)contextMenu.dismiss();contextMenu=new DesktopContextMenu(getContext(),description);return contextMenu;}
    private void show(DesktopContextMenu menu,View anchor,float x,float y){ContextMenus.show(menu,anchor,x,y);}
    private void fileMenu(View anchor,DocumentOps.Ref reference,float x,float y){
        String folder=null;
        if(reference.item.directory())try{folder=DocumentFolders.childPage(page(),reference.item);}catch(JSONException invalid){}
        fileMenu(anchor,reference,x,y,folder);
    }
    private void fileMenu(View anchor,DocumentOps.Ref reference,float x,float y,String directoryPage){
        List<DocumentOps.Ref> targets=targets(reference);DesktopContextMenu menu=menu("File context menu");
        if(picker==null&&reference.item.directory()&&directoryPage!=null)menu.item("Include in library…","files",()->chooseLibrary(directoryPage));menu.item("Open",icon(reference.item.mime,reference.item.directory()),targets.size()==1,()->{if(directoryPage!=null)navigate(directoryPage);else activateOpen(reference);});if(reference.item.directory()&&picker==null){menu.item("Open in new window","files",()->folderLink(reference,directoryPage,false));menu.item("Add desktop shortcut","shortcut",()->folderLink(reference,directoryPage,true));}if(!reference.item.directory()&&picker==null)menu.item("Open with…","programs",targets.size()==1,()->openWith(reference));menu.separator();
        menu.item("Cut","cut",!busy&&targets.stream().allMatch(r->DocumentOps.writable(getContext(),r.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,r.item)),()->copy(targets,true));menu.item("Copy","copy",!busy,()->copy(targets,false));Uri destination=reference.item.directory()?reference.uri:parent();boolean create=DocumentOps.writable(getContext(),destination,DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE,reference.item.directory()?reference.item:parentInfo);menu.item("Paste","paste",!busy&&create&&!ExplorerClipboard.files.isEmpty(),()->pasteInto(destination));menu.separator();
        menu.item("Delete…","delete",!busy&&targets.stream().allMatch(r->DocumentOps.writable(getContext(),r.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,r.item)),()->confirmDelete(targets));menu.item("Rename…","notes",!busy&&targets.size()==1&&DocumentOps.writable(getContext(),reference.uri,DocumentsContract.Document.FLAG_SUPPORTS_RENAME,reference.item),()->rename(reference));
        menu.item("Select","checklist",picker==null||!reference.item.directory(),()->{selectionMode=true;select(reference,true);});menu.item("Properties","control",()->properties(reference));if(parent()!=null&&!canCreate()&&(!StorageAccess.ours(parent())||!StorageAccess.has(getContext())))menu.item("Enable changes for this folder…","security",()->requestChanges(parent()));show(menu,anchor,x,y);
    }
    private void activateOpen(DocumentOps.Ref reference){if(reference.item.directory())try{navigate(DocumentFolders.childPage(page(),reference.item));}catch(JSONException ignored){}else openFile(reference);}
    private void openWith(DocumentOps.Ref reference){files.openWith(reference.uri,reference.item.mime,reference.item.name);}
    private void copy(List<DocumentOps.Ref> refs,boolean cut){ExplorerClipboard.files(refs,cut);message=refs.size()+" item"+(refs.size()==1?"":"s")+(cut?" cut. Choose a destination and Paste.":" copied. Choose a destination and Paste.");render();files.changed();}
    private void paste(){
        if(page().startsWith("library:")){
            List<String> locations=LibraryLocations.locations(store,page().substring(8));
            if(!locations.isEmpty())pasteInto(libraryUri(locations.get(0)));
        }else pasteInto(parent());
    }
    private void pasteInto(Uri target){if(busy)return;List<DocumentOps.Ref> copied=ExplorerClipboard.files;boolean cut=ExplorerClipboard.cut;if(target==null)return;undoCopy=null;work(cut?"Moving items…":"Copying items…",cancel->{String result=DocumentOps.paste(getContext(),new ArrayList<>(copied),target,cut,cancel,text->main.post(()->{if(!closed){message=text;status.setText(text);}}),receipt->main.post(()->{if(!closed)undoCopy=receipt;}),this::copyProgress);if(cut)ExplorerClipboard.completedCut(copied);return result;},null);}
    private void confirmDelete(List<DocumentOps.Ref> refs){confirmDelete(refs,title(page()),true);}
    void dropInRecycleBin(FileDrag drag){confirmDelete(drag.refs,title(drag.sourcePage),false);}
    private void confirmDelete(List<DocumentOps.Ref> refs,String location,boolean allowPermanent){
        if(busy||closed||refs.isEmpty())return;
        android.app.AlertDialog.Builder dialog=Aero.dialog(getContext())
                .setTitle("Delete "+(refs.size()==1?refs.get(0).item.name:refs.size()+" items")+"?")
                .setMessage("Move to Nth Recycle Bin? Originals are removed after their recovery copies are verified.")
                .setPositiveButton("Move to Recycle Bin",(d,w)->work("Moving to Recycle Bin…",cancel->{
                    int done=0;
                    try{for(DocumentOps.Ref ref:refs){RecycleBin.trash(getContext(),ref,cancel,location);removeBookmark(ref.uri);done++;}}
                    catch(OperationCanceledException ex){throw ex;}
                    catch(Exception ex){throw new java.io.IOException(done+" moved. "+ex.getMessage(),ex);}
                    return done+" moved to Recycle Bin.";
                },null))
                .setNegativeButton("Cancel",null);
        if(allowPermanent)dialog.setNeutralButton("Delete permanently…",(d,w)->permanentDelete(refs));
        dialog.show();
    }

    private void permanentDelete(List<DocumentOps.Ref> refs){Aero.dialog(getContext()).setTitle("Delete permanently?").setMessage("Permanently delete "+refs.size()+" item(s)? They will not be placed in Nth Recycle Bin.").setPositiveButton("Delete",(d,w)->work("Deleting…",cancel->{DocumentOps.delete(getContext(),refs,cancel);for(DocumentOps.Ref ref:refs)removeBookmark(ref.uri);return "Permanently deleted "+refs.size()+" item(s).";},null)).setNegativeButton("Cancel",null).show();}

    private void rename(DocumentOps.Ref source){EditText input=edit("New file name",source.item.name);android.app.AlertDialog dialog=Aero.dialog(getContext()).setTitle("Rename").setView(input).setPositiveButton("Rename",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(v->{Aero.styleDialog(getContext(),dialog);dialog.getButton(-1).setOnClickListener(w->{String name=input.getText().toString();try{DocumentOps.name(name);}catch(IllegalArgumentException ex){input.setError(ex.getMessage());return;}work("Renaming…",cancel->{Uri result=DocumentOps.rename(getContext(),source,name,cancel);replaceBookmark(source.uri,result);return "Renamed to "+name+".";},dialog::dismiss);});});dialog.show();}
    private void newFolder(){EditText input=edit("New folder name","New folder");android.app.AlertDialog dialog=Aero.dialog(getContext()).setTitle("New folder").setView(input).setPositiveButton("Create",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(v->{Aero.styleDialog(getContext(),dialog);dialog.getButton(-1).setOnClickListener(w->{String name=input.getText().toString();try{DocumentOps.name(name);}catch(IllegalArgumentException ex){input.setError(ex.getMessage());return;}Uri parent=parent();work("Creating folder…",cancel->{DocumentOps.createFolder(getContext(),parent,name,cancel);return "Folder created.";},dialog::dismiss);});});dialog.show();}
    private EditText edit(String description,String value){EditText input=new EditText(getContext());input.setTypeface(Aero.face(getContext()));input.setSingleLine(true);input.setText(value);input.setSelectAllOnFocus(true);input.setContentDescription(description);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(description.equals("Folder address")?4096:240)});return input;}
    private void properties(DocumentOps.Ref source){Aero.dialog(getContext()).setTitle(source.item.name).setMessage(metadata(source.item)+(source.item.modified>0?"\nModified: "+java.text.DateFormat.getDateTimeInstance().format(new Date(source.item.modified)):"")+"\nLocation: "+title(page())+"\nAccess: "+(getContext().checkUriPermission(source.uri,android.os.Process.myPid(),android.os.Process.myUid(),Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==android.content.pm.PackageManager.PERMISSION_GRANTED?"Changes allowed where supported":"Read-only")).setPositiveButton("OK",null).show();}
    private void work(String title,Work action,Runnable success){if(busy){toast("Finish or cancel the current operation first.");return;}busy=true;files.operationStarted();operationProgress=-1;message=title;status.setText(title);cancelWork.setVisibility(VISIBLE);DocumentOps.Cancel cancel=new DocumentOps.Cancel();operation=cancel;operations.execute(()->{String result;boolean okay=false;try{result=action.run(cancel);okay=true;}catch(OperationCanceledException ex){result="Operation cancelled. Originals not yet moved were kept; check the destination for partial copies.";}catch(Exception ex){result=ex.getMessage()==null?"The storage provider could not complete this action.":ex.getMessage();}String text=result;boolean completed=okay;main.post(()->{if(!closed)DesktopSounds.play(getContext(),completed?"complete":"error");busy=false;operationProgress=-2;operation=null;if(closed)return;cancelWork.setVisibility(GONE);message=text;if(completed&&success!=null)success.run();if(!completed){files.operationFailed();toast(text);}selected.clear();selectionChanged();treeCache.clear();loadedPage="";libraryLoaded="";render();files.changed();});});}
    private void replaceBookmark(Uri before,Uri after){JSONArray values=LinkedFiles.list(store);for(int i=0;i<values.length();i++){JSONObject item=values.optJSONObject(i);if(item!=null&&sameBookmark(item.optString("uri"),before))try{values.put(i,LinkedFiles.describe(getContext(),after));}catch(Exception ignored){}}store.prefs.edit().putString("linked_files",values.toString()).apply();}
    private void removeBookmark(Uri uri){JSONArray values=LinkedFiles.list(store);for(int i=values.length()-1;i>=0;i--)if(values.optJSONObject(i)!=null&&sameBookmark(values.optJSONObject(i).optString("uri"),uri))values.remove(i);store.prefs.edit().putString("linked_files",values.toString()).apply();}
    private boolean sameBookmark(String value,Uri uri){try{return DocumentOps.same(Uri.parse(value),uri);}catch(RuntimeException ex){return value.equals(uri.toString());}}
    private void background(View anchor,float x,float y){folderMenu(anchor,x,y,false);}
    private void organize(){ContextMenus.centered(commands,(x,y)->folderMenu(commands,x,y,true));}
    private void folderMenu(View anchor,float x,float y,boolean organize){DesktopContextMenu menu=menu("Folder context menu");
        if(organize)menu.submenu("Layout","appearance",this::layoutMenu);
        menu.submenu("View","appearance",this::views);menu.submenu("Sort by","programs",()->sortMenu(anchor));menu.submenu("Group by","programs",()->groupMenu(anchor));menu.item("Refresh","refresh",this::refreshFiles);menu.separator();menu.item("Paste","paste",!busy&&canCreate()&&!ExplorerClipboard.files.isEmpty(),this::paste);menu.item("Undo Copy","empty",!busy&&undoCopy!=null,this::undoCopy);menu.separator();menu.item("New folder","files",!busy&&canCreate(),this::newFolder);menu.submenu("New","files",()->newMenu(anchor));if(page().startsWith("folder:")){menu.item("Select all","checklist",listing!=null&&!listing.items.isEmpty(),this::selectAll);menu.item("Clear selection","checklist",selectionMode||!selected.isEmpty(),this::clearSelection);menu.item("Customize This Folder…","appearance",this::customize);menu.item("Properties","control",this::folderProperties);if(!canCreate()&&(!StorageAccess.ours(parent())||!StorageAccess.has(getContext())))menu.item("Enable changes for this folder…","security",()->requestChanges(parent()));}else menu.item("Add a folder…","files",files::chooseFolder);
        if(organize)menu.item("Folder and search options…","control",this::folderOptions);
        show(menu,anchor,x,y);
    }
    private void layoutMenu(){
        DesktopContextMenu layout=menu("Explorer layout");
        layout.checked("Menu bar",menuBar.getVisibility()==VISIBLE,()->{
            boolean show=menuBar.getVisibility()!=VISIBLE;
            menuBar.setVisibility(show?VISIBLE:GONE);
            store.prefs.edit().putBoolean("explorer_menu_bar",show).apply();
        });
        layout.checked("Navigation pane",navigationPaneVisible(),this::toggleNavigationPane);
        ContextMenus.centered(commands,(x,y)->show(layout,commands,x,y));
    }
    private void views(){DesktopContextMenu menu=menu("View options");for(String label:new String[]{"Large icons","Medium icons","Small icons","List","Details","Tiles"})menu.checked(label,viewMode.equals(label),()->setView(label));menu.separator();menu.checked("Navigation pane",navigationPaneVisible(),this::toggleNavigationPane);ContextMenus.centered(commands,(x,y)->show(menu,commands,x,y));}
    private void setView(String label){viewMode=label;details=label.equals("Details");store.prefs.edit().putString("explorer_view_mode",label).putBoolean("explorer_details",details).apply();saveFolderStyle();render();}
    private void sortMenu(View anchor){DesktopContextMenu menu=menu("Sort options");for(String label:new String[]{"Name","Date modified","Type","Size"})menu.checked(label,sort.equals(label),()->{sort=label;saveFolderStyle();render();});menu.separator();menu.checked("Descending",descending,()->{descending=!descending;saveFolderStyle();render();});ContextMenus.centered(anchor,(x,y)->show(menu,anchor,x,y));}
    private void groupMenu(View anchor){DesktopContextMenu menu=menu("Group options");for(String label:new String[]{"None","Name","Date modified","Type","Size"})menu.checked(label,groupBy.equals(label),()->{groupBy=label;collapsedGroups.clear();saveFolderStyle();render();});ContextMenus.centered(anchor,(x,y)->show(menu,anchor,x,y));}
    private void selectAll(){if(listing==null)return;selectionMode=true;selected.clear();for(DocumentFolders.Item item:listing.items)if(visibleItem(item)&&(picker==null||!item.directory()))selected.put(ref(item).uri.toString(),ref(item));selectionChanged();fileList();}
    private void clearSelection(){selected.clear();selectionMode=picker!=null&&picker.multiple();selectionChanged();if(listing!=null)fileList();}
    private void newMenu(View anchor){DesktopContextMenu menu=menu("New item");menu.item("Folder","files",canCreate()&&!busy,this::newFolder);menu.item("Text Document","documentfile",canCreate()&&!busy,this::newText);ContextMenus.centered(anchor,(x,y)->show(menu,anchor,x,y));}
    private void newText(){newText(parent());}
    private void newText(Uri target){LinearLayout column=column();EditText name=edit("New text document name","New Text Document.txt"),text=new EditText(getContext());text.setTypeface(Aero.face(getContext()));text.setHint("Text");text.setGravity(Gravity.TOP);text.setMinLines(3);text.setContentDescription("New text document contents");text.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200000)});column.addView(name);column.addView(text);android.app.AlertDialog dialog=Aero.dialog(getContext()).setTitle("New Text Document").setView(column).setPositiveButton("Create",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(v->{Aero.styleDialog(getContext(),dialog);dialog.getButton(-1).setOnClickListener(w->{String value=name.getText().toString();try{DocumentOps.name(value);}catch(IllegalArgumentException ex){name.setError(ex.getMessage());return;}String file=value.toLowerCase(Locale.ROOT).endsWith(".txt")?value:value+".txt",contents=text.getText().toString();work("Creating text document…",cancel->{DocumentOps.createText(getContext(),target,file,contents,cancel);return "Text document created.";},dialog::dismiss);});});dialog.show();}
    private void undoCopy(){DocumentOps.CopyUndo receipt=undoCopy;if(receipt==null)return;Aero.dialog(getContext()).setTitle("Undo Copy?").setMessage("Remove the files created by the last copy? Originals are kept. Undo refuses to remove a copy that has since changed.").setNegativeButton("Cancel",null).setPositiveButton("Undo",(d,w)->work("Undoing copy…",cancel->{receipt.undo(getContext(),cancel);return "Copy undone. Originals were kept.";},()->undoCopy=null)).show();}
    private void folderProperties(){if(parentInfo==null||parent()==null)return;properties(new DocumentOps.Ref(parent(),null,parentInfo));}
    private void customize(){String[] options={"Details","Tiles","Large icons","List"};Aero.dialog(getContext()).setTitle("Customize This Folder").setItems(options,(d,which)->setView(options[which])).setNegativeButton("Cancel",null).show();}
    private void sendSelected(){if(!page().startsWith("folder:"))return;List<DocumentOps.Ref> refs=new ArrayList<>(selected.values());if(refs.isEmpty()||refs.stream().anyMatch(r->r.item.directory()))return;ArrayList<Uri> uris=new ArrayList<>();String mime=null;for(DocumentOps.Ref ref:refs){uris.add(ref.uri);String type=FileLaunch.mime(ref.item.name,ref.item.mime);mime=mime==null?type:mime.equals(type)?mime:"*/*";}Intent share=new Intent(uris.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE).setType(mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);if(uris.size()==1)share.putExtra(Intent.EXTRA_STREAM,uris.get(0));else share.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);ClipData clip=ClipData.newRawUri("Selected files",uris.get(0));for(int i=1;i<uris.size();i++)clip.addItem(new ClipData.Item(uris.get(i)));share.setClipData(clip);try{getContext().startActivity(Intent.createChooser(share,"Send to"));}catch(ActivityNotFoundException|SecurityException ex){toast("Android could not share these files. Check folder access and installed apps.");}}
    private void closeExplorerWindow(){View current=this;while(current.getParent() instanceof View){current=(View)current.getParent();if(current instanceof DesktopWindow){View button=closeButton(current);if(button!=null)button.performClick();return;}}if(getContext() instanceof android.app.Activity)((android.app.Activity)getContext()).finish();}
    private View closeButton(View view){if("Close window".contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View result=closeButton(((ViewGroup)view).getChildAt(i));if(result!=null)return result;}return null;}
    private void classicMenu(String label){DesktopContextMenu menu=menu(label+" menu");List<DocumentOps.Ref> refs=new ArrayList<>(selected.values());boolean has=!refs.isEmpty();switch(label){case "File":if(picker==null){menu.item("Open folder in new window","files",()->files.openFolder(page()));menu.item("Add folder to desktop","shortcut",page().startsWith("folder:"),()->files.desktopFolder(page(),parentInfo==null?"Folder":parentInfo.name));}menu.item("Open","files",refs.size()==1,()->activateOpen(refs.get(0)));menu.submenu("New","files",()->newMenu(commands));menu.item("Rename…","notes",refs.size()==1&&!busy&&DocumentOps.writable(getContext(),refs.get(0).uri,DocumentsContract.Document.FLAG_SUPPORTS_RENAME,refs.get(0).item),()->rename(refs.get(0)));menu.item("Delete…","delete",has&&!busy&&refs.stream().allMatch(r->DocumentOps.writable(getContext(),r.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,r.item)),()->confirmDelete(refs));menu.item("Properties","control",parentInfo!=null,()->{if(refs.size()==1)properties(refs.get(0));else folderProperties();});menu.separator();menu.item("Close","close",this::closeExplorerWindow);break;case "Edit":menu.item("Undo Copy","empty",undoCopy!=null&&!busy,this::undoCopy);menu.separator();menu.item("Cut","cut",has&&!busy&&refs.stream().allMatch(r->DocumentOps.writable(getContext(),r.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,r.item)),()->copy(refs,true));menu.item("Copy","copy",has&&!busy,()->copy(refs,false));menu.item("Paste","paste",canCreate()&&!busy&&!ExplorerClipboard.files.isEmpty(),this::paste);menu.separator();menu.item("Select all","checklist",listing!=null,this::selectAll);menu.item("Clear selection","checklist",has,this::clearSelection);break;case "View":menu.submenu("Views","appearance",this::views);menu.submenu("Sort by","programs",()->sortMenu(commands));menu.submenu("Group by","programs",()->groupMenu(commands));menu.checked("Navigation pane",navigationPaneVisible(),this::toggleNavigationPane);menu.item("Refresh","refresh",this::refreshFiles);break;case "Tools":menu.item("Save Search…","files",!query().isEmpty()&&page().startsWith("folder:"),this::saveSearch);menu.item("Folder Options…","control",this::folderOptions);menu.item("File access…","security",files::requestDeviceAccess);break;case "Help":menu.item("Nth Explorer Help","control",()->Aero.dialog(getContext()).setTitle("Nth Explorer").setMessage("Tap a file to open it. Hold its icon and drag to another folder or Explorer window for Copy here / Move here. Drop a folder on the desktop to create a shortcut. Hold its name for Open, Cut, Copy, Rename, Delete and Properties. Select items for Send to or Edit commands. Click column headings to sort; use Views and Group by to organize. Breadcrumbs, Back, Forward and Up navigate real folders. On a phone, Folders toggles the navigation pane. Recently Changed filters the current folder to files changed within 30 days. Searches remembers only searches you save. Device file access and linked-folder grants remain under your control.").setPositiveButton("OK",null).show());menu.item("About Nth Explorer","orb",()->Aero.dialog(getContext()).setTitle("Nth Explorer").setMessage("Nth Desktop "+AppInfo.version(getContext())+"\nVista-style native file management for Android.").setPositiveButton("OK",null).show());break;}ContextMenus.centered(commands,(x,y)->show(menu,commands,x,y));}
    private void folderOptions(){LinearLayout column=column();CheckBox previews=Aero.check(getContext(),"Show picture thumbnails",store.prefs.getBoolean("explorer_thumbnails",true));previews.setOnCheckedChangeListener((button,value)->{store.prefs.edit().putBoolean("explorer_thumbnails",value).apply();thumbnails.clear();render();});column.addView(previews);CheckBox hidden=Aero.check(getContext(),"Show hidden files and folders",showHidden);hidden.setOnCheckedChangeListener((button,value)->{showHidden=value;store.prefs.edit().putBoolean("explorer_hidden",value).apply();render();});column.addView(hidden);Aero.dialog(getContext()).setTitle("Folder Options").setView(column).setPositiveButton("OK",null).show();}
    private JSONObject folderStyles(){try{return new JSONObject(store.prefs.getString("explorer_folder_views","{}"));}catch(JSONException ex){return new JSONObject();}}
    private void saveFolderStyle(){Uri folder=parent();if(folder==null)return;JSONObject styles=folderStyles();if(!styles.has(folder.toString())&&styles.length()>=64)styles.remove(styles.keys().next());JSONObject style=new JSONObject();GadgetLayout.put(style,"view",viewMode);GadgetLayout.put(style,"sort",sort);GadgetLayout.put(style,"group",groupBy);GadgetLayout.put(style,"descending",descending);GadgetLayout.put(styles,folder.toString(),style);store.prefs.edit().putString("explorer_folder_views",styles.toString()).apply();}
    private void loadFolderStyle(String page){try{JSONObject style=folderStyles().optJSONObject(DocumentOps.current(page).toString());if(style!=null){viewMode=style.optString("view","Details");details=viewMode.equals("Details");sort=style.optString("sort","Name");groupBy=style.optString("group","None");descending=style.optBoolean("descending");collapsedGroups.clear();}}catch(JSONException|RuntimeException ignored){}}
    private JSONArray savedSearches(){try{return new JSONArray(store.prefs.getString("explorer_searches","[]"));}catch(JSONException ex){return new JSONArray();}}
    private void saveSearch(){EditText name=edit("Saved search name",search.getText().toString());Aero.dialog(getContext()).setTitle("Save Search").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String label=name.getText().toString().trim();if(label.isEmpty())return;JSONArray all=savedSearches(),next=new JSONArray();JSONObject item=new JSONObject();GadgetLayout.put(item,"name",label);GadgetLayout.put(item,"page",page());GadgetLayout.put(item,"query",search.getText().toString());GadgetLayout.put(item,"recent",recentOnly);next.put(item);for(int i=0;i<all.length()&&next.length()<16;i++){JSONObject old=all.optJSONObject(i);if(old!=null&&!old.optString("name").equals(label))next.put(old);}store.prefs.edit().putString("explorer_searches",next.toString()).apply();toast("Search saved.");}).show();}
    private void globalSearchPage(){LinearLayout column=column();heading(column,"Search Results",query().isEmpty()?"Search programs, Control Panel, notes and linked files.":"Results for “"+search.getText()+"”");List<StartSearch.Result> results=query().isEmpty()?Collections.emptyList():StartSearch.find(store,apps.get(),query());String group="";for(StartSearch.Result result:results){if(!result.group.equals(group)){group=result.group;sideHeadingResult(column,group);}task(column,result.kind,result.title,result.group,()->actions.open(result.id));}if(results.isEmpty()&&!query().isEmpty())column.addView(Aero.text(getContext(),"No matching items",13,0xff586976));status.setText(results.size()+" results • Apps, settings, notes and linked files");body(scrolling(column));}
    private void sideHeadingResult(LinearLayout column,String group){TextView header=Aero.text(getContext(),group,13,0xff326421);header.setPadding(0,dp(9),0,dp(5));column.addView(header);}
    private void savedSearchPage(){LinearLayout column=column();heading(column,"Searches","Saved searches keep their original folder and search text.");JSONArray saved=savedSearches();if(saved.length()==0)column.addView(Aero.text(getContext(),"Search a folder, then choose Tools → Save Search.",13,0xff586976));for(int i=0;i<saved.length();i++){JSONObject item=saved.optJSONObject(i);if(item==null)continue;int index=i;int before=column.getChildCount();task(column,"search",item.optString("name"),item.optString("query"),()->{recentOnly=item.optBoolean("recent");navigate(item.optString("page","home"));search.setText(item.optString("query"));});View row=column.getChildAt(before);ContextMenus.bind(row,(x,y)->{DesktopContextMenu menu=menu("Saved search menu");menu.item("Delete saved search","delete",()->{JSONArray all=savedSearches();all.remove(index);store.prefs.edit().putString("explorer_searches",all.toString()).apply();render();});show(menu,row,x,y);});}status.setText(saved.length()+" saved searches");body(scrolling(column));}
    private void bookmarkMenu(View anchor,JSONObject file,float x,float y){DesktopContextMenu menu=menu("File link context menu");menu.item("Open","documentfile",()->files.openDocument(Uri.parse(file.optString("uri")),file.optString("mime"),file.optString("name")));menu.item("Copy","copy",()->{Uri uri=Uri.parse(file.optString("uri"));work("Reading file…",cancel->{DocumentFolders.Item item=DocumentOps.stat(getContext(),uri,cancel);ExplorerClipboard.files(Collections.singletonList(new DocumentOps.Ref(uri,null,item)),false);return "File copied. Choose a destination and Paste.";},null);});menu.item("Remove from Start search","delete",()->{LinkedFiles.remove(store,file.optString("uri"));render();files.changed();});show(menu,anchor,x,y);}
    private void requestChanges(Uri location){if(StorageAccess.ours(location))files.requestDeviceAccess();else files.requestFolder(location);}
    private void linkMenu(View anchor,String uri,String name,float x,float y){DesktopContextMenu menu=menu("Folder link context menu");menu.item("Open","files",()->openLinked(uri,name));menu.item(StorageAccess.ours(Uri.parse(uri))?"File access…":"Enable changes…","security",()->requestChanges(Uri.parse(uri)));if(!StorageAccess.ours(Uri.parse(uri))){menu.separator();menu.item("Remove folder link","delete",()->{DocumentFolders.remove(getContext(),store,uri);render();});}menu.item("Properties","control",()->Aero.dialog(getContext()).setTitle(name).setMessage(StorageAccess.ours(Uri.parse(uri))?"Primary shared device storage. Android protects operating-system files and other apps’ private data.":"A linked storage folder. Removing this link keeps its files.").setPositiveButton("OK",null).show());show(menu,anchor,x,y);}
    private void appList(boolean adding){List<AppCatalog.Entry> visible=new ArrayList<>();List<String> selectedApps=store.fileManagers();for(AppCatalog.Entry app:apps.get())if((adding||selectedApps.contains(app.id))&&matches(app.label))visible.add(app);LinearLayout column=column();heading(column,adding?"Add file manager apps":"File Managers",adding?"Choose apps to keep in this folder.":"Open an app or hold its shortcut for a menu.");if(!adding){task(column,"files","System file picker","Browse files with Android",()->files.pickFile("documents"));link(column,"Add file manager apps…",()->navigate("add-apps")).setContentDescription("Add file manager apps");}ListView list=new ListView(getContext());list.setDividerHeight(0);list.addHeaderView(column,null,false);if(adding){LinearLayout footer=column();link(footer,"Done",()->navigate("managers"));list.addFooterView(footer,null,false);}list.setAdapter(new BaseAdapter(){public int getCount(){return visible.size();}public Object getItem(int i){return visible.get(i);}public long getItemId(int i){return i;}public View getView(int i,View recycled,ViewGroup parent){AppCatalog.Entry app=visible.get(i);LinearLayout row=row("programs",app.icon,app.label,adding?(store.fileManagers().contains(app.id)?"✓ In File Managers":"Tap to add"):"Tap to open • Hold for a menu");row.setContentDescription((adding?"Choose file manager app: ":"File manager app: ")+app.label);row.setOnClickListener(v->appClick(app,adding));ContextMenus.bind(row,(x,y)->appMenu(row,app,x,y));return row;}});list.setOnItemClickListener((p,v,position,id)->{int index=position-list.getHeaderViewsCount();if(index>=0&&index<visible.size())appClick(visible.get(index),adding);});list.setOnItemLongClickListener((p,v,position,id)->{int index=position-list.getHeaderViewsCount();if(index<0||index>=visible.size())return false;ContextMenus.centered(v,(x,y)->appMenu(v,visible.get(index),x,y));return true;});ContextMenus.blank(list,(x,y)->{DesktopContextMenu menu=menu("App folder context menu");menu.item("Paste","paste",!ExplorerClipboard.shortcuts.isEmpty(),()->{try{ExplorerClipboard.paste(store,"managers");render();files.changed();}catch(IllegalArgumentException ex){toast(ex.getMessage());}});menu.item("Add file manager apps…","programs",()->navigate("add-apps"));menu.item("Refresh","refresh",this::render);show(menu,list,x,y);});status.setText(adding?selectedApps.size()+" selected apps":visible.size()+" file manager shortcuts");body(list);}
    private void appClick(AppCatalog.Entry app,boolean adding){if(!adding){files.app(app.id);return;}List<String> ids=store.fileManagers();if(ids.contains(app.id))ids.remove(app.id);else ids.add(app.id);store.fileManagers(ids);render();}
    private void appMenu(View anchor,AppCatalog.Entry app,float x,float y){ShortcutMenus.show(getContext(),anchor,x,y,store,app.id,"managers",app.label,new ShortcutMenus.Host(){public void open(String id){files.app(id);}public void rename(String id){actions.action("rename-shortcut:"+id);}public void changed(){render();files.changed();}});}
    private LinearLayout row(String kind,android.graphics.drawable.Drawable drawable,String name,String description){LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(11),dp(7),dp(9),dp(7));View icon;if(drawable==null)icon=new AeroIcon(getContext(),kind);else{ImageView image=new ImageView(getContext());image.setImageDrawable(drawable);image.setScaleType(ImageView.ScaleType.FIT_CENTER);icon=image;}row.addView(icon,new LayoutParams(dp(details?28:40),dp(details?28:40)));LinearLayout text=Aero.column(getContext());text.setPadding(dp(10),0,0,0);TextView title=Aero.text(getContext(),name,13,Aero.INK);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);text.addView(title);TextView sub=Aero.text(getContext(),description,11,0xff6c7e8a);sub.setSingleLine(true);sub.setEllipsize(android.text.TextUtils.TruncateAt.END);text.addView(sub);row.addView(text,new LayoutParams(0,-2,1));row.setContentDescription(name);return row;}
    static String icon(String mime,boolean directory){return directory?"files":mime.equals(FileLaunch.APK)?"programs":mime.startsWith("image/")?"picturefile":mime.startsWith("audio/")?"musicfile":mime.startsWith("video/")?"videofile":mime.equals("application/pdf")?"pdffile":mime.contains("zip")?"archivefile":"documentfile";}
    private static String fileType(String mime){return mime.equals(FileLaunch.APK)?"Android app package":mime.startsWith("image/")?"Picture":mime.startsWith("audio/")?"Audio file":mime.startsWith("video/")?"Video":mime.startsWith("text/")?"Text document":mime.equals("application/pdf")?"PDF document":mime.equals("application/json")?"JSON document":mime.contains("zip")?"ZIP archive":mime.contains("xml")?"XML document":"File";}
    private void renderTree(){side.removeAllViews();sideHeading("Favorites");favoriteLink("Documents","documents",()->{recentOnly=false;favorite("documents","Documents");});favoriteLink("Pictures","pictures",()->{recentOnly=false;favorite("pictures","Pictures");});favoriteLink("Music","music",()->{recentOnly=false;favorite("music","Music");});favoriteLink("Recently Changed","clock",()->{recentOnly=true;sort="Date modified";descending=true;if(!page().startsWith("folder:"))openDevice("documents");else render();});favoriteLink("Searches","search",()->navigate("searches"));favoriteLink("More »","files",()->favoriteLinksMenu());if(picker==null){sideHeading("Libraries");favoriteLink("Libraries","folderapps",()->navigate("libraries"));}sideHeading("Computer");favoriteLink("Desktop","computer",()->actions.action("show-desktop"));favoriteLink("Computer","computer",()->{recentOnly=false;navigate("computer");});JSONArray roots=navigationRoots();for(int i=0;i<roots.length();i++){JSONObject root=roots.optJSONObject(i);if(root!=null)treeRow(DocumentFolders.rootPage(root.optString("uri"),root.optString("name")),0);} }
    private void sideHeading(String title){TextView label=Aero.text(getContext(),title,11,0xff6d7478);label.setPadding(0,dp(8),0,dp(5));side.addView(label);}
    private void favoriteLink(String title,String kind,Runnable action){LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setBackground(Aero.selection(getContext()));row.addView(new AeroIcon(getContext(),kind),new LayoutParams(dp(18),dp(18)));TextView label=Aero.text(getContext(),title,11,0xff14649b);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setPadding(dp(5),0,0,0);row.addView(label,new LayoutParams(0,dp(29),1));row.setContentDescription("Navigation: "+title);row.setOnClickListener(v->action.run());side.addView(row);}
    private void favoriteLinksMenu(){DesktopContextMenu menu=menu("Favorite Links");for(String[] target:StorageAccess.FAVORITES)if(!Arrays.asList("documents","pictures","music").contains(target[0]))menu.item(target[1],target[0],()->{recentOnly=false;openFavorite(target[0]);});if(picker==null)menu.item("File Managers","folderapps",()->navigate("managers"));menu.item("Add a folder…","files",files::chooseFolder);ContextMenus.centered(side,(x,y)->show(menu,side,x,y));}
    private void treeRow(String page,int depth){treeRow(page,depth,null);}
    private void treeRow(String page,int depth,DocumentFolders.Item known){if(depth>12)return;String name=title(page);name=name.substring(name.lastIndexOf(" › ")+3);try{JSONArray c=DocumentFolders.path(page).getJSONArray("crumbs");name=c.getJSONObject(c.length()-1).optString("name");}catch(JSONException ignored){}LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(depth*10),0,0,0);String label=name;TextView expand=command(expanded.contains(page)?"−":"+",()->{if(expanded.contains(page))expanded.remove(page);else if(expanded.size()<100)expanded.add(page);renderTree();});expand.setContentDescription((expanded.contains(page)?"Collapse ":"Expand ")+label);row.addView(expand,new LayoutParams(dp(18),dp(27)));row.addView(new AeroIcon(getContext(),"files"),new LayoutParams(dp(17),dp(19)));TextView folder=command(name,()->navigate(page));folder.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);folder.setSingleLine(true);folder.setEllipsize(android.text.TextUtils.TruncateAt.END);folder.setTextSize(11);if(page.equals(page()))folder.setBackground(Aero.selection(getContext()));folder.setContentDescription("Browse tree folder: "+name);row.addView(folder,new LayoutParams(0,dp(30),1));side.addView(row);ContextMenus.bind(folder,(x,y)->{try{JSONObject path=DocumentFolders.path(page);if(known==null)linkMenu(folder,path.getString("tree"),label,x,y);else{JSONArray values=path.getJSONArray("crumbs");Uri parent=DocumentOps.current(prefix(path,values.length()-2));fileMenu(folder,new DocumentOps.Ref(DocumentOps.current(page),parent,known),x,y,page);}}catch(JSONException ignored){}});if(!expanded.contains(page))return;if(treeCache.containsKey(page)){int count=0;for(DocumentFolders.Item child:treeCache.get(page)){if(!child.directory())continue;if(count++>=80)break;try{treeRow(DocumentFolders.childPage(page,child),depth+1,child);}catch(JSONException ignored){}}}else if(treeLoading.add(page)){worker.execute(()->{List<DocumentFolders.Item> children=new ArrayList<>();try{children=DocumentFolders.read(getContext(),page,new CancellationSignal()).items;}catch(Exception ignored){}List<DocumentFolders.Item> result=children;main.post(()->{treeLoading.remove(page);if(!closed){treeCache.put(page,result);renderTree();}});});}}
    @Override void showFolders(){DesktopContextMenu menu=menu("Navigation folders");for(String[] target:new String[][]{{"Files","home"},{"Libraries","libraries"},{"Computer","computer"},{"File Managers","managers"}})if(picker==null||!target[1].equals("managers")&&(!target[1].equals("libraries")||!picker.foldersOnly()))menu.item(target[0],"files",()->navigate(target[1]));JSONArray roots=navigationRoots();for(int i=0;i<roots.length();i++){JSONObject root=roots.optJSONObject(i);if(root!=null)menu.item(root.optString("name"),"files",()->openLinked(root.optString("uri"),root.optString("name")));}ContextMenus.centered(folders,(x,y)->show(menu,folders,x,y));}
    private String parentPage(){
        if(page().startsWith("folder:"))try{JSONObject path=DocumentFolders.path(page());JSONArray values=path.getJSONArray("crumbs");if(values.length()>1)return prefix(path,values.length()-2);}catch(JSONException ignored){}
        if(page().startsWith("library:"))return "libraries";
        if(page().equals("home")||page().equals("computer"))return null;
        return page().equals("add-apps")?"managers":"computer";
    }
    @Override boolean canGoUp(){return parentPage()!=null;}
    @Override void goUp(){String parent=parentPage();if(parent!=null)navigate(parent);}
    void refreshFiles(){if(closed||busy)return;thumbnails.clear();treeCache.clear();loadedPage="";libraryLoaded="";render();}
    @Override void refreshApps(){if(page().equals("managers")||page().equals("add-apps"))render();}
    @Override JSONObject extraState(){JSONObject state=new JSONObject();GadgetLayout.put(state,"details",details);GadgetLayout.put(state,"view",viewMode);GadgetLayout.put(state,"group",groupBy);GadgetLayout.put(state,"recent",recentOnly);GadgetLayout.put(state,"sort",sort);GadgetLayout.put(state,"descending",descending);GadgetLayout.put(state,"selectionMode",selectionMode);GadgetLayout.put(state,"favorite",pendingFavorite);GadgetLayout.put(state,"expanded",new JSONArray(expanded));JSONArray refs=new JSONArray();for(DocumentOps.Ref ref:selected.values()){try{refs.put(new JSONObject().put("uri",ref.uri.toString()).put("parent",ref.parent==null?"":ref.parent.toString()).put("id",ref.item.id).put("name",ref.item.name).put("mime",ref.item.mime).put("size",ref.item.size).put("modified",ref.item.modified).put("flags",ref.item.flags));}catch(JSONException ignored){}}if(refs.length()<=64&&refs.toString().length()<24000)GadgetLayout.put(state,"selected",refs);return state;}
    @Override void restoreExtra(JSONObject state){if(state==null)return;details=state.optBoolean("details",true);viewMode=state.optString("view",details?"Details":"Tiles");groupBy=state.optString("group","None");recentOnly=state.optBoolean("recent");sort=state.optString("sort","Name");descending=state.optBoolean("descending");selectionMode=state.optBoolean("selectionMode");pendingFavorite=state.optString("favorite");JSONArray refs=state.optJSONArray("selected");if(refs!=null){for(int i=0;i<Math.min(64,refs.length());i++)try{JSONObject ref=refs.getJSONObject(i);Uri uri=Uri.parse(ref.getString("uri"));if(!"content".equals(uri.getScheme()))continue;DocumentFolders.Item item=new DocumentFolders.Item(ref.getString("id"),ref.getString("name"),ref.getString("mime"),ref.optLong("size",-1),ref.optLong("modified"),ref.optInt("flags"));String parent=ref.optString("parent");selected.put(uri.toString(),new DocumentOps.Ref(uri,parent.isEmpty()?null:Uri.parse(parent),item));}catch(JSONException ignored){}restoredSelection=true;}JSONArray nodes=state.optJSONArray("expanded");if(nodes!=null)for(int i=0;i<Math.min(100,nodes.length());i++)expanded.add(nodes.optString(i));}
    @Override public boolean dispatchKeyEvent(KeyEvent event){if(event.getAction()==KeyEvent.ACTION_DOWN){int key=event.getKeyCode();if(picker==null&&event.isAltPressed()&&key==KeyEvent.KEYCODE_P){togglePreview();return true;}if(key==KeyEvent.KEYCODE_F5){refreshFiles();return true;}if(event.isCtrlPressed()&&key==KeyEvent.KEYCODE_F){search.requestFocus();return true;}if(!(findFocus() instanceof EditText)){if(event.isAltPressed()&&key==KeyEvent.KEYCODE_DPAD_LEFT){goBack();return true;}if(event.isAltPressed()&&key==KeyEvent.KEYCODE_DPAD_RIGHT){forward.performClick();return true;}if(event.isCtrlPressed()){List<DocumentOps.Ref> refs=new ArrayList<>(selected.values());if(key==KeyEvent.KEYCODE_A&&listing!=null){selectAll();return true;}if(key==KeyEvent.KEYCODE_C&&!refs.isEmpty()){copy(refs,false);return true;}if(key==KeyEvent.KEYCODE_X&&!refs.isEmpty()&&refs.stream().allMatch(r->DocumentOps.writable(getContext(),r.uri,DocumentsContract.Document.FLAG_SUPPORTS_DELETE,r.item))){copy(refs,true);return true;}if(key==KeyEvent.KEYCODE_V&&canCreate()&&!ExplorerClipboard.files.isEmpty()){paste();return true;}if(key==KeyEvent.KEYCODE_Z&&undoCopy!=null){undoCopy();return true;}}}}return super.dispatchKeyEvent(event);}
    void pickerMode(){selectionMode=picker!=null&&picker.multiple();}
    void dismissMenus(){if(contextMenu!=null)contextMenu.dismiss();}
    @Override void close(){closed=true;if(preview!=null)preview.close();thumbnails.close();dismissMenus();if(cancellation!=null)cancellation.cancel();if(operation!=null)operation.cancel();main.removeCallbacksAndMessages(null);worker.shutdownNow();operations.shutdown();}
    private void toast(String text){Toast.makeText(getContext(),text,Toast.LENGTH_LONG).show();}
}
