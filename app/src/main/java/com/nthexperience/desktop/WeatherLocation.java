package com.nthexperience.desktop;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Location is requested only while Home is visible. No background service or tracking permission. */
final class WeatherLocation {
    private final Context context;private final DesktopStore store;private final WeatherClient weather;private final Runnable changed;private final BooleanSupplier needed;
    private final LocationManager manager;private final Handler main=new Handler(Looper.getMainLooper());
    private final List<CancellationSignal> signals=new ArrayList<>();private final List<LocationListener> listeners=new ArrayList<>();
    private boolean visible,closed,received,pending;private int generation;private String status="";
    WeatherLocation(Context c,DesktopStore s,WeatherClient w,BooleanSupplier needed,Runnable changed){context=c;store=s;weather=w;this.needed=needed;this.changed=changed;manager=c.getSystemService(LocationManager.class);}
    boolean permitted(){return LocationAccess.permitted(context);}
    String status(){return status;}
    boolean watching(){return !listeners.isEmpty()||!signals.isEmpty();}
    void resume(){visible=true;if(pending||(store.prefs.getBoolean("weather_follow",false)&&needed.getAsBoolean()))locate();}
    void pause(){visible=false;cancel();}
    void cancel(){generation++;for(CancellationSignal signal:signals)signal.cancel();signals.clear();for(LocationListener listener:listeners)try{manager.removeUpdates(listener);}catch(RuntimeException ignored){}listeners.clear();main.removeCallbacksAndMessages(null);}
    void fixed(){pending=false;store.prefs.edit().putBoolean("weather_follow",false).apply();cancel();status="Fixed city selected.";changed.run();}
    void stopFollowing(){pending=false;cancel();status="Automatic location is off. Your selected area is kept.";changed.run();}
    void denied(){pending=false;store.prefs.edit().putBoolean("weather_follow",false).apply();cancel();status="Location access is off. You can still choose a city.";changed.run();}
    void locate(){
        cancel();if(closed)return;if(!visible){pending=true;return;}pending=false;if(!permitted()){status="Allow approximate location to update your area, or choose a city.";changed.run();return;}
        if(manager==null){status="Location is unavailable. Choose a city instead.";changed.run();return;}
        int request=generation;received=false;List<String> providers=new ArrayList<>();for(String provider:LocationAccess.precise(context)?new String[]{"fused",LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER}:new String[]{"fused",LocationManager.NETWORK_PROVIDER})try{if(manager.getAllProviders().contains(provider)&&manager.isProviderEnabled(provider))providers.add(provider);}catch(RuntimeException ignored){}
        if(providers.isEmpty()){status="Enable device location, or choose a city.";changed.run();return;}
        status=LocationAccess.precise(context)?"Finding your location…":"Finding your approximate area…";changed.run();Location cached=null;
        for(String provider:LocationAccess.precise(context)?Arrays.asList("fused",LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER,LocationManager.PASSIVE_PROVIDER):Arrays.asList("fused",LocationManager.NETWORK_PROVIDER,LocationManager.PASSIVE_PROVIDER))try{Location candidate=manager.getLastKnownLocation(provider);if(usable(candidate,SystemClock.elapsedRealtimeNanos(),System.currentTimeMillis(),30*60*1000L)&&(cached==null||candidate.getElapsedRealtimeNanos()>cached.getElapsedRealtimeNanos()))cached=candidate;}catch(RuntimeException ignored){}
        if(cached!=null){accept(cached);status="Using a recent location while checking for an update…";changed.run();}
        boolean follow=store.prefs.getBoolean("weather_follow",false);
        for(String provider:providers)try{
            if(follow||Build.VERSION.SDK_INT<30){LocationListener listener=new LocationListener(){public void onLocationChanged(Location location){if(!visible||request!=generation)return;accept(location);if(!follow)cancel();}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int s,Bundle b){}};manager.requestLocationUpdates(provider,follow?300000:0,follow?3000:0,listener,Looper.getMainLooper());listeners.add(listener);}
            else{CancellationSignal signal=new CancellationSignal();signals.add(signal);manager.getCurrentLocation(provider,signal,context.getMainExecutor(),location->{if(visible&&request==generation&&location!=null){accept(location);cancel();}});}
        }catch(SecurityException|IllegalArgumentException ignored){}
        main.postDelayed(()->{if(request!=generation||!visible)return;boolean got=received;if(!follow)cancel();status=got?"Using your recent area. Updates resume when a newer location is available.":"No location fix yet. Check device location, or choose a city. Your saved forecast is kept.";changed.run();},30000);
    }
    private void accept(Location location){if(!usable(location,SystemClock.elapsedRealtimeNanos(),System.currentTimeMillis(),30*60*1000L))return;received=true;
        double lat=location.getLatitude(),lon=location.getLongitude();boolean move=true;try{float[] distance=new float[1];Location.distanceBetween(Double.parseDouble(store.prefs.getString("weather_lat","999")),Double.parseDouble(store.prefs.getString("weather_lon","999")),lat,lon,distance);move=distance[0]>=3000;}catch(RuntimeException ignored){}
        if(!weather.configured()||move)weather.location("My area",lat,lon);else store.prefs.edit().putString("weather_city","My area").apply();
        status=store.prefs.getBoolean("weather_follow",false)?"Following your "+(LocationAccess.precise(context)?"location":"approximate area")+" while Nth Desktop is visible.":(LocationAccess.precise(context)?"Location updated.":"Approximate location updated.");changed.run();
    }
    static boolean usable(Location location,long elapsedNanos,long wall,int maxAge){return usable(location,elapsedNanos,wall,(long)maxAge);}
    static boolean usable(Location location,long elapsedNanos,long wall,long maxAge){if(location==null||!Double.isFinite(location.getLatitude())||!Double.isFinite(location.getLongitude())||Math.abs(location.getLatitude())>90||Math.abs(location.getLongitude())>180||location.getElapsedRealtimeNanos()<0||(location.hasAccuracy()&&(!Float.isFinite(location.getAccuracy())||location.getAccuracy()<0||location.getAccuracy()>30000)))return false;long age=location.getElapsedRealtimeNanos()>0?(elapsedNanos-location.getElapsedRealtimeNanos())/1000000:wall-location.getTime();return age>=0&&age<=maxAge;}
    void close(){closed=true;visible=false;cancel();}
}
