package com.nthexperience.desktop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

/** Foreground location grants; coarse access remains fully usable when precise access is declined. */
final class LocationAccess {
    static boolean precise(Context context){return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    static boolean permitted(Context context){return precise(context)||context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    static void request(Activity activity,int request,boolean precise){activity.requestPermissions(precise?new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}:new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},request);}
    private LocationAccess(){}
}
