package com.nthexperience.desktop;

import android.content.Context;
import android.os.SystemClock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import android.view.*;
import android.widget.*;
import org.json.*;

final class GadgetGallery extends VistaShell {
    interface Host extends Actions {void changed();void reveal();void align(String id);}
    private final WeatherClient weather;private final Host host;private String category="All gadgets",selected="clock";private int columns=2;
    private static final String[] CATEGORIES={"All gadgets","Time","Information","Productivity","Device","Photos","Games"};
    GadgetGallery(Context c,DesktopStore store,WeatherClient weather,Host host){super(c,store,host,"Search Gadget Gallery");this.weather=weather;this.host=host;TextView show=command("Show gadgets",host::reveal);show.setContentDescription("Show desktop gadgets");commands.addView(show,new LayoutParams(-2,dp(36)));commands.addView(command("Category ▾",this::categories),new LayoutParams(-2,dp(36)));start("home");addOnLayoutChangeListener((v,l,t,right,b,ol,ot,or,ob)->{int next=right-l>=dp(720)?3:2;if(next!=columns){columns=next;post(this::render);}});}
    private String lastTile="";
    private long lastTap;
    @Override void render(){
        lastTile="";side.removeAllViews();
        for(String name:CATEGORIES)sideLink(name,()->{category=name;render();});
        sideLink("Personalization",()->host.open("@appearance"));
        address.setText("Desktop Gadget Gallery  ›  "+category);
        LinearLayout column=column();
        heading(column,"Desktop Gadget Gallery","Double-click a gadget to place it at the right edge.");
        GridLayout grid=new GridLayout(getContext());grid.setColumnCount(columns);
        grid.setBackground(Aero.panel(getContext(),0xfff8fcff,0xffe3eef7,3,0xffa5bbcb));
        column.addView(grid,new LayoutParams(-1,-2));
        TextView information=Aero.text(getContext(),"",12,Aero.INK);
        information.setContentDescription("Selected gadget details");
        Aero.pad(information,10);information.setBackground(Aero.panel(getContext(),0xffe9f2f8,0xffd2e6f4,2,0xffa4bdcf));
        Map<String,LinearLayout> tiles=new LinkedHashMap<>();
        for(String[] item:GadgetCatalog.ITEMS){
            String id=item[0];
            if(!category.equals("All gadgets")&&!category.equals(item[2]))continue;
            if(!query().isEmpty()&&!(item[1]+" "+item[2]+" "+item[3]).toLowerCase(java.util.Locale.ROOT).contains(query()))continue;
            LinearLayout cell=Aero.column(getContext());
            cell.setGravity(Gravity.CENTER);cell.setPadding(dp(7),dp(8),dp(7),dp(8));
            tiles.put(id,cell);highlight(cell,id.equals(selected));
            FrameLayout preview=new FrameLayout(getContext());
            preview.setBackground(Aero.panel(getContext(),0xffe2edf3,0xffbacfdd,4,0xffa4b9c7));
            GadgetView face=new GadgetView(getContext(),id,store,weather);
            face.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            preview.addView(face,new FrameLayout.LayoutParams(dp(102),dp(105),Gravity.CENTER));
            preview.setContentDescription("Select "+item[1]+" gadget");preview.setFocusable(true);
            cell.addView(preview,new LayoutParams(-1,dp(109)));
            TextView title=Aero.text(getContext(),item[1],13,Aero.INK);title.setGravity(Gravity.CENTER);
            cell.addView(title,new LayoutParams(-1,dp(27)));
            TextView toggle=Aero.button(getContext(),"",()->{});
            updateToggle(toggle,item,store.gadgets().contains(id));
            toggle.setOnClickListener(v->setEnabled(item,toggle,!store.gadgets().contains(id)));
            cell.addView(toggle,new LayoutParams(-1,dp(40)));
            preview.setOnClickListener(v->{
                long now=SystemClock.uptimeMillis();
                boolean doubleClick=id.equals(lastTile)&&now-lastTap<=ViewConfiguration.getDoubleTapTimeout();
                lastTile=doubleClick?"":id;lastTap=now;
                if(tiles.containsKey(selected))highlight(tiles.get(selected),false);
                selected=id;highlight(cell,true);showDetails(information,item);
                if(doubleClick)setEnabled(item,toggle,true);
            });
            TextView settings=link(cell,"Settings",()->open(id));settings.setGravity(Gravity.CENTER);
            settings.setContentDescription("Settings for "+item[1]+" gadget");
            TextView details=link(cell,"Show details",()->Aero.dialog(getContext()).setTitle(item[1])
                    .setMessage(item[3]+"\n\nCategory: "+item[2]).setPositiveButton("OK",null).show());
            details.setGravity(Gravity.CENTER);details.setContentDescription("Details for "+item[1]+" gadget");
            GridLayout.LayoutParams cellSize=new GridLayout.LayoutParams();cellSize.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            cellSize.width=0;cellSize.bottomMargin=dp(8);grid.addView(cell,cellSize);
        }
        for(String[] item:GadgetCatalog.ITEMS)if(item[0].equals(selected))showDetails(information,item);
        column.addView(information);
        if(tiles.isEmpty())heading(column,"No matching gadgets","Try another category or clear your search.");
        CheckBox lock=Aero.check(getContext(),"Lock desktop layout",store.prefs.getBoolean("layout_locked",false));
        lock.setOnCheckedChangeListener((v,value)->store.prefs.edit().putBoolean("layout_locked",value).apply());column.addView(lock);
        CheckBox snap=Aero.check(getContext(),"Snap gadgets to grid",store.prefs.getBoolean("snap_gadgets",true));
        snap.setOnCheckedChangeListener((v,value)->store.prefs.edit().putBoolean("snap_gadgets",value).apply());column.addView(snap);
        showCount(tiles.size());body(scrolling(column));
    }
    private void highlight(LinearLayout tile,boolean selected){
        tile.setBackground(selected?Aero.panel(getContext(),0xffe2f4ff,0xffc1e1f5,3,0xff79a9c8):Aero.selection(getContext()));
    }
    private void showDetails(TextView details,String[] item){details.setText(item[1]+"\n"+item[3]);}
    private void updateToggle(TextView toggle,String[] item,boolean added){
        String verb=added?"Remove":"Add";toggle.setText(verb);toggle.setContentDescription(verb+" "+item[1]+" gadget");
    }
    private void setEnabled(String[] item,TextView toggle,boolean added){
        String id=item[0];List<String> enabled=store.gadgets();
        boolean alreadyAdded=enabled.contains(id);
        if(added&&!alreadyAdded){enabled.add(id);store.gadgets(enabled);}
        if(!added&&alreadyAdded){enabled.remove(id);store.gadgets(enabled);}
        if(added)host.align(id);else host.changed();
        updateToggle(toggle,item,added);
        showCount(GadgetCatalog.IDS.length);
    }
    private void showCount(int count){status.setText(count+" available gadgets  •  "+store.gadgets().size()+" on your desktop");}
    private void open(String kind){host.action("gadget:"+kind);}
    private void categories(){DesktopContextMenu menu=new DesktopContextMenu(getContext(),"Gadget categories");for(String name:CATEGORIES)menu.checked(name,name.equals(category),()->{category=name;render();});ContextMenus.centered(commands,(x,y)->ContextMenus.show(menu,commands,x,y));}
    @Override void showFolders(){categories();}
    @Override void goUp(){category="All gadgets";render();}
    @Override JSONObject extraState(){JSONObject state=new JSONObject();GadgetLayout.put(state,"category",category);GadgetLayout.put(state,"selected",selected);return state;}
    @Override void restoreExtra(JSONObject state){if(state!=null){if(java.util.Arrays.asList(CATEGORIES).contains(state.optString("category")))category=state.optString("category");if(java.util.Arrays.asList(GadgetCatalog.IDS).contains(state.optString("selected")))selected=state.optString("selected");}}
    @Override void refreshApps(){render();}
}
