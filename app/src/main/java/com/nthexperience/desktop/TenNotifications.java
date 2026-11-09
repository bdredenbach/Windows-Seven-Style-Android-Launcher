package com.nthexperience.desktop;
import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.service.notification.*;
import java.util.*;
import java.util.concurrent.*;

/** Optional Android notification access. Contents remain in memory and are never sent to AI. */
public final class TenNotifications extends NotificationListenerService {
 private static final Map<String,StatusBarNotification> active=new ConcurrentHashMap<>();private static final Set<Runnable> observers=new CopyOnWriteArraySet<>();private static final Handler main=new Handler(Looper.getMainLooper());private static volatile TenNotifications service;
 static final class Row {final String key,app,title,text;final PendingIntent intent;final boolean dismissible;Row(String key,String app,String title,String text,PendingIntent intent,boolean dismissible){this.key=key;this.app=app;this.title=title;this.text=text;this.intent=intent;this.dismissible=dismissible;}}
 static boolean allowed(Context c){ComponentName name=new ComponentName(c,TenNotifications.class);if(Build.VERSION.SDK_INT>=27)return c.getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(name);String enabled=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");return enabled!=null&&Arrays.asList(enabled.split(":")).contains(name.flattenToString());}
 static void request(Context c){try{c.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(ActivityNotFoundException e){c.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}}
 static void observe(Runnable r){observers.add(r);}static void unobserve(Runnable r){observers.remove(r);}private static void changed(){for(Runnable r:observers)main.post(r);}
 @Override public void onListenerConnected(){service=this;active.clear();try{StatusBarNotification[] rows=getActiveNotifications();if(rows!=null)for(StatusBarNotification s:rows)active.put(s.getKey(),s);}catch(SecurityException ignored){}changed();}
 @Override public void onListenerDisconnected(){service=null;active.clear();changed();}
 @Override public void onNotificationPosted(StatusBarNotification item){if(item!=null){active.put(item.getKey(),item);changed();}}
 @Override public void onNotificationRemoved(StatusBarNotification item){if(item!=null){active.remove(item.getKey());changed();}}
 @Override public void onDestroy(){if(service==this)service=null;active.clear();changed();super.onDestroy();}
 static List<Row> rows(Context c){List<Row> result=new ArrayList<>();if(!allowed(c)||!c.getSharedPreferences("desktop",0).getBoolean("ten_notifications",true)||c.getSharedPreferences("desktop",0).getBoolean("ten_focus",false))return result;KeyguardManager lock=c.getSystemService(KeyguardManager.class);boolean locked=lock!=null&&lock.isKeyguardLocked();List<StatusBarNotification> all=new ArrayList<>(active.values());all.sort(Comparator.comparingLong(StatusBarNotification::getPostTime).reversed());for(StatusBarNotification s:all){if(result.size()>=40)break;Notification n=s.getNotification();if((n.flags&Notification.FLAG_GROUP_SUMMARY)!=0||n.visibility==Notification.VISIBILITY_SECRET)continue;String app=s.getPackageName();try{app=c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(app,0)).toString();}catch(Exception ignored){}String title=locked?app:clip(n.extras.getCharSequence(Notification.EXTRA_TITLE)),text=locked?"Unlock to view this notification":clip(n.extras.getCharSequence(Notification.EXTRA_TEXT));result.add(new Row(s.getKey(),app,title,text,n.contentIntent,s.isClearable()));}return result;}
 private static String clip(CharSequence value){if(value==null)return "";String s=value.toString();return s.substring(0,Math.min(300,s.length()));}
 static void dismiss(String key){TenNotifications s=service;if(s!=null)try{s.cancelNotification(key);}catch(SecurityException ignored){}}
 static void clear(){TenNotifications s=service;if(s!=null)try{s.cancelAllNotifications();}catch(SecurityException ignored){}}
 static boolean open(Row row){if(row.intent==null)return false;try{row.intent.send();return true;}catch(PendingIntent.CanceledException e){return false;}}
}
