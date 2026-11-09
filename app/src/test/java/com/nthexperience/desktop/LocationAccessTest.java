package com.nthexperience.desktop;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class LocationAccessTest {
    private ActivityController<Activity> controller;private Activity activity;
    @Before public void setup(){controller=Robolectric.buildActivity(Activity.class).setup();activity=controller.get();shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);}
    @After public void close(){controller.pause().stop().destroy();}
    @Test public void approximateGrantWorksWithoutPreciseAccess(){shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);assertTrue(LocationAccess.permitted(activity));assertFalse(LocationAccess.precise(activity));}
    @Test public void preciseGrantIsRecognized(){shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);assertTrue(LocationAccess.permitted(activity));assertTrue(LocationAccess.precise(activity));}
    @Test public void approximateChoiceRequestsOnlyCoarsePermission(){LocationAccess.request(activity,InitializationFlow.LOCATION,false);org.robolectric.shadows.ShadowActivity.PermissionsRequest request=shadowOf(activity).getLastRequestedPermission();assertArrayEquals(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},request.requestedPermissions);}
    @Test public void manifestDeclaresForegroundAccuracyChoicesOnly()throws Exception{List<String> permissions=Arrays.asList(activity.getPackageManager().getPackageInfo(activity.getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions);assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION));assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION));assertFalse(permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION));}
}
