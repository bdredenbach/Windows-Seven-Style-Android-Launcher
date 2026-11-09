package com.nthexperience.desktop;

import android.content.*;
import java.util.*;

/** Device-local progress; desktop backups never grant permissions or carry setup completion. */
final class InitializationState {
    static final String PREFS="desktop_initialization_v1";
    enum Step { HOME, FILES, USAGE, NOTIFICATIONS, BRIGHTNESS, LOCATION, FOLDERS, DONE }
    final SharedPreferences prefs;
    InitializationState(Context context){prefs=context.getSharedPreferences(PREFS,0);
        // v1 used 5 for folder repair and 6 for Done. Offer the newly added location step once.
        if(prefs.getInt("flow_version",1)<2){int old=prefs.getInt("step",0);prefs.edit().putInt("step",old>=5?Step.LOCATION.ordinal():Math.max(0,old)).putInt("flow_version",2).commit();}
    }
    Step step(){int index=prefs.getInt("step",0);return Step.values()[Math.max(0,Math.min(index,Step.DONE.ordinal()))];}
    void advance(){prefs.edit().putInt("step",Math.min(step().ordinal()+1,Step.DONE.ordinal())).remove("waiting").remove("left").remove("returned").remove("repair_uri").remove("processing").commit();}
    int waiting(){return prefs.getInt("waiting",0);}
    void waitFor(int code,String uri){prefs.edit().putInt("waiting",code).putBoolean("left",false).putBoolean("returned",false).putString("repair_uri",uri).remove("processing").commit();}
    void clearWaiting(){prefs.edit().remove("waiting").remove("left").remove("returned").remove("repair_uri").remove("processing").commit();}
    Set<String> deferred(){return new HashSet<>(prefs.getStringSet("deferred",Collections.emptySet()));}
    void defer(String uri){Set<String> values=deferred();values.add(uri);prefs.edit().putStringSet("deferred",values).commit();}
    static void restored(Context context){InitializationState state=new InitializationState(context);state.prefs.edit().putInt("step",Math.min(state.step().ordinal(),Step.FOLDERS.ordinal())).remove("waiting").remove("left").remove("returned").remove("repair_uri").remove("deferred").remove("processing").commit();}
}
