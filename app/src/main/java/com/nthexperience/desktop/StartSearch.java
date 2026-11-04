package com.nthexperience.desktop;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import org.json.*;
import java.util.*;

final class StartSearch {
    static final class Result {
        final String id,title,group,kind;final Drawable icon;final boolean shortcut;
        Result(String id,String title,String group,String kind,Drawable icon,boolean shortcut){this.id=id;this.title=title;this.group=group;this.kind=kind;this.icon=icon;this.shortcut=shortcut;}
    }
    static List<Result> find(DesktopStore store,List<AppCatalog.Entry> apps,String query){
        String q=query.trim().toLowerCase(Locale.ROOT);List<Result> result=new ArrayList<>();
        for(AppCatalog.Entry app:apps){String title=store.name(app.id,app.label);if((title+" "+app.label).toLowerCase(Locale.ROOT).contains(q))result.add(new Result(app.id,title,"Programs","programs",app.icon,true));}
        if(q.isEmpty())return result;
        if((BrowserPanel.NAME+" browser web internet").toLowerCase(Locale.ROOT).contains(q))result.add(new Result("@browser",store.name("@browser",BrowserPanel.NAME),"Programs","browser",null,true));
        if("photo gallery pictures images".contains(q))result.add(new Result("@photos","Photo Gallery","Programs","pictures",null,true));
        if("nth media player music audio mp3 video".contains(q))result.add(new Result(MediaLibrary.PLAYER,MediaLibrary.PLAYER_TITLE,"Programs","musicfile",null,true));
        if("nth media center music video".contains(q))result.add(new Result(MediaLibrary.CENTER,MediaLibrary.CENTER_TITLE,"Programs","videofile",null,true));
        if("flip 3d aero window switcher".contains(q))result.add(new Result("@flip3d","Flip 3D","Programs","appearance",null,false));
        VistaControlPanel.searchItems(q,result);
        for(String[] gadget:GadgetCatalog.ITEMS)if(GadgetCatalog.extra(gadget[0])&&(gadget[1]+" "+gadget[3]).toLowerCase(Locale.ROOT).contains(q))result.add(new Result("@gadget:"+gadget[0],gadget[1],"Gadgets",GadgetCatalog.icon(gadget[0]),null,false));
        for(String[] note:new String[][]{{"note","Notepad","@notes"},{"gadget_note","Sticky note","@sticky"}}){String text=store.prefs.getString(note[0],"");if((note[1]+" "+text).toLowerCase(Locale.ROOT).contains(q))result.add(new Result(note[2],note[1],"Notes","notes",null,false));}
        JSONArray files=LinkedFiles.list(store);for(int i=0;i<files.length();i++){JSONObject file=files.optJSONObject(i);if(file!=null&&file.optString("name").toLowerCase(Locale.ROOT).contains(q))result.add(new Result("search-file:"+file.optString("uri"),file.optString("name"),"Linked files","documentfile",null,false));}
        Set<String> seen=new HashSet<>();for(Result item:result)seen.add(item.id.startsWith("search-file:")?item.id.substring(12):item.id);
        JSONObject locations=FolderLinks.list(store);Iterator<String> linkKeys=locations.keys();while(linkKeys.hasNext()){String key=linkKeys.next();JSONObject link=locations.optJSONObject(key);if(link!=null&&link.optString("title").toLowerCase(Locale.ROOT).contains(q))result.add(new Result(key,store.name(key,link.optString("title")),"Folders","files",null,true));}
        JSONArray recent=RecentFiles.list(store);for(int i=0;i<recent.length();i++){JSONObject item=recent.optJSONObject(i);if(item!=null&&item.optString("name").toLowerCase(Locale.ROOT).contains(q)&&seen.add(item.optString("uri")))result.add(new Result("recent-file:"+item.optString("uri"),item.optString("name"),"Recent Items",item.optString("kind").equals("folder")?"files":"documentfile",null,false));}
        JSONArray favorites=new BrowserData(store.context).list("favorites");for(int i=0;i<favorites.length();i++){JSONObject item=favorites.optJSONObject(i);if(item!=null&&(item.optString("title")+" "+item.optString("url")).toLowerCase(Locale.ROOT).contains(q)&&BrowserUrls.web(item.optString("url")))result.add(new Result("favorite-web:"+item.optString("url"),item.optString("title",item.optString("url")),"Favorites","browser",null,false));}
        return result;
    }
}
