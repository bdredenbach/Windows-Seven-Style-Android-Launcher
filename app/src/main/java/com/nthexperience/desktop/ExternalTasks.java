package com.nthexperience.desktop;

import android.app.AppOpsManager;
import android.app.usage.*;
import android.content.*;
import java.util.*;

/** Session taskbar buttons; Android does not expose other apps' actual running-task lists. */
final class ExternalTasks {
    private static final LinkedHashSet<String> tasks=new LinkedHashSet<>(),launched=new LinkedHashSet<>();
    private static final Map<String,Long> dismissed=new LinkedHashMap<>();
    private static long since=System.currentTimeMillis();
    static synchronized void opened(String id){dismissed.remove(id);tasks.add(id);launched.add(id);trim();}
    static synchronized List<String> list(){return new ArrayList<>(tasks);}
    static synchronized void remove(String id){tasks.remove(id);launched.remove(id);dismissed.put(id,System.currentTimeMillis());while(dismissed.size()>128)dismissed.remove(dismissed.keySet().iterator().next());}
    static synchronized void resetObservation(){since=System.currentTimeMillis();tasks.retainAll(launched);}
    static boolean permitted(Context context){try{AppOpsManager ops=context.getSystemService(AppOpsManager.class);return ops!=null&&ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),context.getPackageName())==AppOpsManager.MODE_ALLOWED;}catch(RuntimeException ex){return false;}}
    static boolean includesExternalApps(DesktopStore store,boolean permitted){
        return permitted&&store.usagePrefs.getBoolean(DesktopStore.TASKBAR_USAGE,true);
    }
    static synchronized void refresh(Context context,DesktopStore store,List<AppCatalog.Entry> apps){
        Set<String> available=new HashSet<>();Map<String,String> packages=new HashMap<>();for(AppCatalog.Entry app:apps){available.add(app.id);ComponentName name=ComponentName.unflattenFromString(app.id);if(name!=null)packages.putIfAbsent(name.getPackageName(),app.id);}tasks.retainAll(available);launched.retainAll(available);dismissed.keySet().retainAll(available);
        long now=System.currentTimeMillis();if(now<since){since=now-60000;dismissed.clear();}if(!includesExternalApps(store,permitted(context))){since=now;tasks.retainAll(launched);return;}
        try{UsageStatsManager manager=context.getSystemService(UsageStatsManager.class);UsageEvents events=manager==null?null:manager.queryEvents(Math.max(since,now-86400000L),now);UsageEvents.Event event=new UsageEvents.Event();int count=0;while(events!=null&&events.hasNextEvent()&&count++<20000){events.getNextEvent(event);if(event.getEventType()!=UsageEvents.Event.MOVE_TO_FOREGROUND)continue;String id=packages.get(event.getPackageName());if(id!=null&&event.getTimeStamp()>dismissed.getOrDefault(id,0L))tasks.add(id);}since=now;trim();}catch(RuntimeException ex){since=now;}
    }
    private static void trim(){while(tasks.size()>32){String first=tasks.iterator().next();tasks.remove(first);launched.remove(first);}}
}
