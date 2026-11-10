package com.nthexperience.desktop;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="mdpi")
public class TaskbarRowsTest {
    Context context;DesktopStore store;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();store=new DesktopStore(context);store.prefs.edit().clear().commit();}
    @Test public void remembersChoicesIndependentlyOfOrderAndRunningState()throws Exception{
        TaskbarRows.save(store.prefs,"@notes",1);TaskbarRows.save(store.prefs,"@photos",0);
        assertEquals(Integer.valueOf(1),TaskbarRows.read(store.prefs).get("@notes"));
        assertEquals(Integer.valueOf(0),TaskbarRows.read(store.prefs).get("@photos"));
        assertFalse(store.prefs.contains(TaskbarOrder.KEY));
        TaskbarItems items=new TaskbarItems(context,true);View notes=slot(items,"@notes",true),photos=slot(items,"@photos",false),other=slot(items,"@calendar",false);
        items.assignRows(TaskbarRows.read(store.prefs));assertEquals(1,TaskbarItems.row(notes));assertEquals(0,TaskbarItems.row(photos));assertEquals(1,TaskbarItems.row(other));
    }
    private View slot(TaskbarItems items,String id,boolean running){View v=new View(context);v.setTag(R.id.taskbar_order_key,id);v.setLayoutParams(new LinearLayout.LayoutParams(150,48));TaskbarItems.slot(v,running);items.addView(v);return v;}
    @Test public void scrollingMovesOnlyChosenRowAndStaticResetsBoth(){
        TaskbarItems items=new TaskbarItems(context,true);View top=slot(items,"@notes",true),bottom=slot(items,"@photos",false);slot(items,"@system",true);slot(items,"@calendar",false);
        items.bottomBounds(30,180,200);items.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(96,View.MeasureSpec.EXACTLY));items.layout(0,0,200,96);
        int bottomLeft=bottom.getLeft();assertTrue(items.scrollRowBy(0,70));assertEquals(-70,top.getLeft());assertEquals(bottomLeft,bottom.getLeft());
        assertTrue(items.scrollRowBy(1,45));assertEquals(-70,top.getLeft());assertEquals(bottomLeft-45,bottom.getLeft());
        items.scrollable(false);items.layout(0,0,200,96);assertEquals(0,items.rowScroll(0));assertEquals(0,items.rowScroll(1));
    }
    @Test public void rowChoicesExportAndActuallyRestore()throws Exception{
        TaskbarRows.save(store.prefs,"@notes",1);DesktopBackup.Plan plan=DesktopBackup.parse(DesktopBackup.snapshot(store).toString());store.prefs.edit().remove(TaskbarRows.KEY).commit();DesktopBackup.apply(context,store,plan,false);assertEquals(Integer.valueOf(1),TaskbarRows.read(store.prefs).get("@notes"));
    }
    @Test public void olderAndOtherVariantBackupsRemainAccepted()throws Exception{
        JSONObject backup=DesktopBackup.snapshot(store);backup.getJSONObject("settings").remove(TaskbarRows.KEY);DesktopBackup.parse(backup.toString());backup.put("variant","eight");DesktopBackup.parse(backup.toString());
    }
    @Test public void rejectsBadTypesRangesIdsAndJsonBeforeMutation()throws Exception{
        store.prefs.edit().putString("note","keep").commit();
        for(String raw:new String[]{"[]","{","{\"@notes\":true}","{\"@notes\":\"0\"}","{\"@notes\":0.5}","{\"@notes\":2}","{\"@notes\":-1}","{\"\":0}","{\"bad\\nkey\":0}"}){
            JSONObject backup=DesktopBackup.snapshot(store);backup.getJSONObject("settings").put("note","changed").put(TaskbarRows.KEY,raw);try{DesktopBackup.parse(backup.toString());fail(raw);}catch(JSONException expected){}assertEquals("keep",store.prefs.getString("note",""));
        }
    }
    @Test public void rejectsOversizedMapAndBoundsSavedHistory()throws Exception{
        JSONObject rows=new JSONObject();for(int i=0;i<=TaskbarOrder.LIMIT;i++)rows.put("app:"+i,i%2);try{TaskbarRows.parse(rows.toString());fail();}catch(JSONException expected){}
        for(int i=0;i<80;i++)TaskbarRows.save(store.prefs,"app:"+i,i%2);assertEquals(TaskbarOrder.LIMIT,TaskbarRows.read(store.prefs).size());assertFalse(TaskbarRows.read(store.prefs).containsKey("app:0"));
    }
}
