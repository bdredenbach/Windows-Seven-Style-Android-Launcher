package com.nthexperience.desktop;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="mdpi")
public class LauncherRenderingTest {
    private Context context;private DesktopStore store;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();store=new DesktopStore(context);store.prefs.edit().clear().commit();BuiltinIconCache.clear();}
    private DesktopIconGrid grid(int count,int rows){
        store.prefs.edit().putInt("desktop_rows_portrait",rows).putInt("desktop_columns_portrait",3).commit();
        DesktopIconGrid grid=new DesktopIconGrid(context,store,"portrait");grid.configure(120,104,Collections.emptyList());
        for(int i=0;i<count;i++){LinearLayout tile=new LinearLayout(context);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);tile.setPadding(2,6,2,3);tile.setTag("item"+i);tile.addView(new AeroIcon(context,"files"),new LinearLayout.LayoutParams(48,48));TextView label=new TextView(context);label.setText("Folder "+i+"\nSecond label line");label.setTextSize(12);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(label,new LinearLayout.LayoutParams(-1,-2));grid.addView(tile);}
        grid.viewport(264,312);return grid;
    }
    private void measure(DesktopIconGrid grid){grid.forceLayout();grid.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));grid.layout(0,0,360,grid.getMeasuredHeight());}
    private void labelsFit(DesktopIconGrid grid){
        int visible=0;for(int i=0;i<grid.getChildCount();i++){LinearLayout tile=(LinearLayout)grid.getChildAt(i);if(tile.getTop()>=264)continue;visible++;assertTrue("Whole icon row stays above taskbar: "+i+" bottom="+tile.getBottom(),tile.getBottom()<=264);TextView label=(TextView)tile.getChildAt(1);assertTrue(label.getMeasuredHeight()>0);assertTrue("Label fits its cell",label.getBottom()+tile.getPaddingBottom()<=tile.getHeight());assertEquals("Both label lines remain available",2,label.getLayout().getLineCount());assertTrue("Second line is not clipped: bottom="+label.getLayout().getLineBottom(1)+" height="+label.getHeight(),label.getLayout().getLineBottom(1)<=label.getHeight()-label.getPaddingTop()-label.getPaddingBottom());}
        assertEquals(9,visible);
    }
    @Test public void extendedTaskbarFitsAutomaticRowsAndScrollsWholeOverflowRows(){DesktopIconGrid grid=grid(12,0);measure(grid);labelsFit(grid);assertTrue(grid.getChildAt(9).getTop()>=264);}
    @Test public void extendedTaskbarFitsRequestedRows(){DesktopIconGrid grid=grid(9,3);measure(grid);labelsFit(grid);}
    @Test public void largerSystemTextKeepsTwoLinesAboveExtendedTaskbar(){android.content.res.Configuration configuration=new android.content.res.Configuration(context.getResources().getConfiguration());configuration.fontScale=1.3f;context=context.createConfigurationContext(configuration);DesktopIconGrid grid=grid(9,3);measure(grid);labelsFit(grid);}
    @Test public void fittingSavedPositionDoesNotRewriteIt()throws Exception{
        DesktopIconGrid grid=grid(1,0);JSONObject points=new JSONObject().put("item0",new JSONObject().put("x",0).put("y",208));store.prefs.edit().putString("desktop_positions_portrait",points.toString()).commit();
        Map<String,?> original=new HashMap<>(store.prefs.getAll());measure(grid);assertTrue(grid.getChildAt(0).getBottom()<=264);assertEquals(original,store.prefs.getAll());grid.viewport(312,312);measure(grid);assertEquals(208,grid.getChildAt(0).getTop());assertEquals(original,store.prefs.getAll());
    }
    @Test public void unchangedGeometryReusesPlanButViewportAndPreferencesInvalidate()throws Exception{
        DesktopIconGrid grid=grid(9,0);measure(grid);int builds=grid.planBuildCount();measure(grid);assertEquals(builds,grid.planBuildCount());grid.viewport(312,312);measure(grid);assertTrue(grid.planBuildCount()>builds);builds=grid.planBuildCount();JSONObject points=new JSONObject().put("item0",new JSONObject().put("x",1).put("y",0));store.prefs.edit().putString("desktop_positions_portrait",points.toString()).commit();
        measure(grid);assertTrue(grid.planBuildCount()>builds);assertEquals(240,grid.getChildAt(0).getLeft());
    }
    @Test public void modelCacheObservesEditsAndReturnsIndependentMutableValues()throws Exception{
        store.prefs.edit().putString("app_folders","{\"folder:a\":{\"name\":\"Original\",\"items\":[\"@files\"]}}").putString("shortcut_names","{\"@files\":\"Renamed files\"}").putString("gadgets","[\"clock\",\"calendar\"]").commit();
        assertEquals("Renamed files",store.name("@files","Files"));assertEquals("Original",store.name("folder:a","Folder"));store.folder("folder:a").put("name","Unsaved");assertEquals("Original",store.name("folder:a","Folder"));List<String> gadgets=store.gadgets();gadgets.clear();assertEquals(2,store.gadgets().size());
        store.prefs.edit().putString("shortcut_names","{\"@files\":\"Changed again\"}").putString("gadgets","[\"weather\"]").commit();assertEquals("Changed again",store.name("@files","Files"));assertEquals(Collections.singletonList("weather"),store.gadgets());store.rename("folder:a","Changed folder");assertEquals("Changed folder",store.name("folder:a","Folder"));
    }
    private Bitmap icon(String kind,int size)throws Exception{AeroIcon view=new AeroIcon(context,kind);view.layout(0,0,size,size);view.draw(new Canvas(Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888)));Field cached=AeroIcon.class.getDeclaredField("cachedIcon");cached.setAccessible(true);return (Bitmap)cached.get(view);}
    @Test public void staticArtworkReusesBitmapAndSeparatesSizeAndKind()throws Exception{Bitmap first=icon("files",48);assertSame(first,icon("files",48));assertNotSame(first,icon("files",64));assertNotSame(first,icon("clock",48));}
    @Test public void staticArtworkCacheIsBounded(){for(int i=0;i<12;i++)BuiltinIconCache.put("large"+i,Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888));assertTrue(BuiltinIconCache.bytes()<=4*1024*1024);assertNull(BuiltinIconCache.get("large0"));}
    @Test public void catalogReuseRequiresSameEntries(){AppCatalog.Entry entry=new AppCatalog.Entry("a/.Main","A",new ColorDrawable());Map<String,AppCatalog.Entry> current=new HashMap<>();current.put(entry.id,entry);assertTrue(DesktopCache.matches(current,Collections.singletonList(entry)));assertFalse(DesktopCache.matches(current,Collections.singletonList(new AppCatalog.Entry(entry.id,"New label",new ColorDrawable()))));assertFalse(DesktopCache.matches(current,Collections.emptyList()));}
}
