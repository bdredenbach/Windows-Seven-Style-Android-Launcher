package com.nthexperience.desktop;
import android.content.Context;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class GadgetRotationTest {
    private DesktopStore store;
    @Before public void setup(){store=new DesktopStore(RuntimeEnvironment.getApplication());store.prefs.edit().clear().commit();}
    @Test public void staleLandscapeLayoutCannotEnableUnselectedGadgets()throws Exception{
        List<String> selected=Arrays.asList("clock","calendar");store.gadgets(selected);store.prefs.edit().putBoolean("sidebar_visible",true).putString("gadget_layout_portrait","{\"visible\":false}").putString("gadget_layout_landscape","{\"visible\":true,\"order\":[\"weather\",\"clock\",\"notes\",\"calendar\",\"cpu\"]}").commit();
        GadgetLayout landscape=new GadgetLayout(store,"landscape"),portrait=new GadgetLayout(store,"portrait");assertFalse(portrait.visible());assertFalse(landscape.visible());assertEquals(new HashSet<>(selected),new HashSet<>(landscape.ordered()));assertEquals(new HashSet<>(selected),new HashSet<>(portrait.ordered()));landscape.visible(true);assertTrue(portrait.visible());assertTrue(new GadgetLayout(store,"portrait").visible());
        store.gadgets(Collections.singletonList("calendar"));assertEquals(Collections.singletonList("calendar"),new GadgetLayout(store,"landscape").ordered());assertEquals(Collections.singletonList("calendar"),new GadgetLayout(store,"portrait").ordered());
    }
    @Test public void positionsStaySeparateWhileVisibilityAndSelectionAreShared(){
        store.gadgets(Arrays.asList("clock","weather"));GadgetLayout portrait=new GadgetLayout(store,"portrait");portrait.position("clock",.1f,.2f,1);GadgetLayout landscape=new GadgetLayout(store,"landscape");landscape.position("clock",.7f,.8f,1);landscape.visible(false);assertFalse(portrait.visible());assertEquals(.1f,new GadgetLayout(store,"portrait").x("clock"),.001);assertEquals(.7f,new GadgetLayout(store,"landscape").x("clock"),.001);assertEquals(new HashSet<>(portrait.ordered()),new HashSet<>(landscape.ordered()));
    }
}
