package com.nthexperience.desktop;

import android.content.*;
import android.graphics.*;
import android.media.AudioManager;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Foreground-only status subscriptions and native flyouts, within the desktop's safe bounds. */
final class SystemTray extends LinearLayout {
    interface Host {void action(String id);void open(String id);void opening();default void layout(){}}
    private final FrameLayout workspace;
    private final AeroWallpaper wallpaper;
    private final Host host;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ConnectivityManager connections;
    private final AudioManager audio;
    private final PowerManager power;
    private final ArrayList<TrayIcon> icons=new ArrayList<>();
    private FrameLayout cover;
    private LinearLayout frame;
    private TextView summary;
    private SeekBar volume;
    private Intent battery;
    private String page="";
    private int leftInset,rightInset,restoreVolume,workspaceBudget;
    private final AeroIcon taskManager;
    private final TrayFlag actionCenter;private int alertCount=-1;
    private final SharedPreferences notices;
    private final SharedPreferences.OnSharedPreferenceChangeListener alertChanges=(saved,key)->{if(key==null||key.equals("files")||key.equals("backup")||key.equals("linked_folders"))refresh();};
    private final BrightnessControl brightness;private boolean expanded;private final android.content.SharedPreferences prefs;private TextView compactStatus;
    private boolean running,batteryRegistered,networkRegistered,tracking;
    private final BroadcastReceiver batteryChanged=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(Intent.ACTION_BATTERY_CHANGED.equals(i.getAction()))battery=i;update();}};
    private final ConnectivityManager.NetworkCallback networkChanged=new ConnectivityManager.NetworkCallback(){public void onAvailable(Network n){refresh();}public void onLost(Network n){refresh();}public void onCapabilitiesChanged(Network n,NetworkCapabilities caps){refresh();}};
    private final Runnable tick=new Runnable(){public void run(){if(!running||cover==null)return;update();main.postDelayed(this,1000);}};
    SystemTray(Context c,FrameLayout workspace,AeroWallpaper wallpaper,Host host){
        super(c);notices=c.getSharedPreferences("seven_action_center",0);prefs=c.getSharedPreferences("desktop",0);expanded=prefs.getBoolean("tray_expanded",false);brightness=new BrightnessControl((android.app.Activity)c);this.workspace=workspace;this.wallpaper=wallpaper;this.host=host;connections=c.getSystemService(ConnectivityManager.class);audio=c.getSystemService(AudioManager.class);power=c.getSystemService(PowerManager.class);setGravity(Gravity.CENTER_VERTICAL);
        String[] names={"Battery status","Network status","Volume control","Brightness control"};String[] pages={"battery","network","volume","brightness"};
        for(int i=0;i<names.length;i++){TrayIcon icon=new TrayIcon(c,i);icon.setContentDescription(names[i]);icon.setFocusable(true);icon.setBackground(Aero.selection(c));String target=pages[i];icon.setOnClickListener(v->show(target));addView(icon,new LayoutParams(dp(44),dp(48)));icons.add(icon);}
        taskManager = new AeroIcon(c, "taskmanager");
        taskManager.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        taskManager.setFocusable(true);
        taskManager.setContentDescription("Task Manager");
        taskManager.setTooltipText("Task Manager");
        taskManager.setBackground(Aero.selection(c));
        taskManager.setOnClickListener(v -> host.open("@taskmanager"));
        addView(taskManager, new LayoutParams(dp(44), dp(48)));
        actionCenter=new TrayFlag(c);
        actionCenter.setFocusable(true);
        actionCenter.setContentDescription("Action Center");
        actionCenter.setTooltipText("Action Center");
        actionCenter.setBackground(Aero.selection(c));
        actionCenter.setOnClickListener(v->show("actioncenter"));
        addView(actionCenter,new LayoutParams(dp(44),dp(48)));
        compactStatus=Aero.text(c,"⌃",21,Color.WHITE);compactStatus.setGravity(Gravity.CENTER);compactStatus.setContentDescription("Device controls");compactStatus.setFocusable(true);compactStatus.setBackground(Aero.selection(c));compactStatus.setOnClickListener(v->{expanded=!expanded;prefs.edit().putBoolean("tray_expanded",expanded).apply();updateIconVisibility();host.layout();});compactStatus.setOnLongClickListener(v->{show("controls");return true;});addView(compactStatus,0,new LayoutParams(dp(44),dp(48)));updateIconVisibility();
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        sizeIcons();
        super.onMeasure(widthSpec,heightSpec);
    }
    void fitWidth(int workspaceWidth){workspaceBudget=workspaceWidth;sizeIcons();}
    private void sizeIcons(){
        int configured=dp(getResources().getConfiguration().screenWidthDp);
        // Use one budget in measure and layout, including narrower inset/IME transitions.
        int workspaceWidth=workspaceBudget>0?Math.min(workspaceBudget,configured):configured;
        // Leave room for Start, the clock, and part of the scrolling task strip.
        int width = Math.min(dp(44), Math.max(dp(24), (workspaceWidth - dp(200)) / getChildCount()));
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int childWidth = !expanded && child == compactStatus ? dp(44) : width;
            ViewGroup.LayoutParams params = child.getLayoutParams();
            if (params.width != childWidth) {
                params.width = childWidth;
                child.setLayoutParams(params);
            }
        }
        int horizontalPadding = Math.max(0, (width - dp(28)) / 2);
        if (taskManager.getPaddingLeft() != horizontalPadding) {
            taskManager.setPadding(horizontalPadding, dp(10), horizontalPadding, dp(10));
        }
    }
    void clockNow(){if(compactStatus!=null){compactStatus.setTextSize(20);compactStatus.setText(expanded?"›":"‹");compactStatus.setTooltipText(expanded?"Hide notification icons":"Show hidden notification icons");}}
    private void updateIconVisibility() {
        taskManager.setVisibility(expanded ? VISIBLE : GONE);
        actionCenter.setVisibility(expanded ? VISIBLE : GONE);
        for (TrayIcon icon : icons) {
            icon.setVisibility(expanded ? VISIBLE : GONE);
        }
        clockNow();
    }

    void start(){if(running)return;running=true;notices.registerOnSharedPreferenceChangeListener(alertChanges);prefs.registerOnSharedPreferenceChangeListener(alertChanges);try{IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);battery=Build.VERSION.SDK_INT>=33?getContext().registerReceiver(batteryChanged,filter,Context.RECEIVER_EXPORTED):getContext().registerReceiver(batteryChanged,filter);batteryRegistered=true;}catch(RuntimeException ignored){}try{if(connections!=null){connections.registerDefaultNetworkCallback(networkChanged);networkRegistered=true;}}catch(RuntimeException ignored){}update();}
    void stop(){running=false;notices.unregisterOnSharedPreferenceChangeListener(alertChanges);prefs.unregisterOnSharedPreferenceChangeListener(alertChanges);dismiss();if(batteryRegistered){getContext().unregisterReceiver(batteryChanged);batteryRegistered=false;}if(networkRegistered){connections.unregisterNetworkCallback(networkChanged);networkRegistered=false;}main.removeCallbacksAndMessages(null);}
    private void refresh(){main.post(()->{if(running)update();});}
    private int topInset;
    void bounds(int left,int right,int top){topInset=top;bounds(left,right);}
    void bounds(int left,int right){leftInset=left;rightInset=right;if(cover==null)return;FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)frame.getLayoutParams();p.width=Math.min(dp(324),Math.max(1,workspace.getWidth()-left-right-dp(12)));p.height=Math.min(dp(page.equals("calendar")?395:page.equals("actioncenter")?310:285),Math.max(1,workspace.getHeight()-topInset-dp(10)));p.rightMargin=right+dp(6);frame.setLayoutParams(p);}
    boolean dismiss(){if(cover==null)return false;workspace.removeView(cover);cover=null;summary=null;volume=null;page="";tracking=false;main.removeCallbacks(tick);return true;}
    void show(String value){
        if(value.equals(page)&&cover!=null){dismiss();return;}dismiss();host.opening();page=value;
        cover=new FrameLayout(getContext());cover.setElevation(dp(48));cover.setFocusableInTouchMode(true);cover.setContentDescription("System tray flyout");cover.setOnClickListener(v->dismiss());workspace.addView(cover,new FrameLayout.LayoutParams(-1,-1));
        frame=Aero.column(getContext());frame.setBackground(new AeroGlass(wallpaper,AeroGlass.START));Aero.pad(frame,6);frame.setClickable(true);FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(dp(324),dp(285),Gravity.BOTTOM|Gravity.END);position.bottomMargin=dp(5);cover.addView(frame,position);
        LinearLayout title=new LinearLayout(getContext());TextView name=Aero.text(getContext(),value.equals("actioncenter")?"Action Center":value.equals("battery")?"Power and battery":value.equals("network")?"Network connections":value.equals("volume")?"Volume":value.equals("brightness")?"Brightness":value.equals("controls")?"Device controls":"Clock and calendar",14,Color.WHITE);name.setPadding(dp(6),0,0,0);title.addView(name,new LayoutParams(0,dp(36),1));TextView close=Aero.button(getContext(),"×",this::dismiss);close.setContentDescription("Close tray flyout");title.addView(close,new LayoutParams(dp(38),dp(34)));frame.addView(title);
        LinearLayout body=Aero.column(getContext());Aero.pad(body,12);body.setBackground(Aero.panel(getContext(),0xfffdfefe,0xfff0f6fa,1,0xffa8bccb));ScrollView scroll=new ScrollView(getContext());scroll.setFillViewport(true);scroll.addView(body);frame.addView(scroll,new LayoutParams(-1,0,1));
        summary=Aero.text(getContext(),"",13,Aero.INK);summary.setPadding(0,0,0,dp(12));summary.setContentDescription("Tray status details");body.addView(summary);
        switch(value){
            case "actioncenter":
                frame.setContentDescription("Action Center flyout");
                java.util.List<ActionCenter.Alert> alerts=ActionCenter.alerts(getContext(),new DesktopStore(getContext()));
                for(ActionCenter.Alert alert:alerts){
                    TextView heading=Aero.text(getContext(),alert.title,14,0xff234f75);Aero.bold(heading);body.addView(heading);
                    TextView message=Aero.text(getContext(),alert.message,12,Aero.INK);message.setMaxLines(3);message.setEllipsize(android.text.TextUtils.TruncateAt.END);body.addView(message);
                    button(body,"Review "+alert.title,()->{dismiss();host.open(alert.target);});
                }
                button(body,"Open Action Center",()->{dismiss();host.open("@actioncenter");});break;
            case "controls":
                for(String[] choice:new String[][]{{"Battery status","battery"},{"Network status","network"},{"Volume control","volume"},{"Brightness control","brightness"},{"Clock and calendar","calendar"}}){TextView link=Aero.button(getContext(),choice[0],()->show(choice[1]));link.setContentDescription(choice[0]);body.addView(link,new LayoutParams(-1,dp(48)));}break;
            case "brightness":brightness.content(body);break;
            case "battery":button(body,"Power settings",()->host.action("android:battery"));break;
            case "network":button(body,"Wi-Fi settings",()->host.action("android:wifi"));button(body,"Network settings",()->host.action("android:network"));break;
            case "volume":
                volume=new SeekBar(getContext());volume.setContentDescription("Media volume");int max=audio==null?0:audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);volume.setMax(max);volume.setEnabled(audio!=null&&!audio.isVolumeFixed()&&max>0);volume.setProgress(audio==null?0:audio.getStreamVolume(AudioManager.STREAM_MUSIC));restoreVolume=Math.max(1,volume.getProgress());
                volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){tracking=true;}public void onStopTrackingTouch(SeekBar b){tracking=false;update();}public void onProgressChanged(SeekBar b,int n,boolean user){if(user)setVolume(n);}});body.addView(volume,new LayoutParams(-1,dp(44)));
                button(body,"Mute / restore media",()->{if(audio==null||audio.isVolumeFixed())return;int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);if(current>0)restoreVolume=current;setVolume(current>0?0:restoreVolume);});button(body,"Sound settings",()->host.action("android:sound"));break;
            case "calendar":
                TextView month=Aero.text(getContext(),"",15,0xff234f75);month.setGravity(Gravity.CENTER);MonthCalendarView[] ref=new MonthCalendarView[1];MonthCalendarView calendar=new MonthCalendarView(getContext(),()->{if(ref[0]!=null)month.setText(ref[0].title());});ref[0]=calendar;
                LinearLayout nav=new LinearLayout(getContext());nav.addView(Aero.button(getContext(),"‹",()->calendar.offset(-1)),new LayoutParams(dp(40),dp(36)));nav.addView(month,new LayoutParams(0,dp(36),1));nav.addView(Aero.button(getContext(),"›",()->calendar.offset(1)),new LayoutParams(dp(40),dp(36)));body.addView(nav);month.setText(calendar.title());body.addView(calendar,new LayoutParams(-1,dp(210)));button(body,"Today",calendar::today);button(body,"Open Calendar",()->host.open("@calendar"));button(body,"Date and time settings",()->host.action("android:date"));break;
        }
        bounds(leftInset,rightInset);cover.bringToFront();update();if(running)main.postDelayed(tick,1000);
    }
    private void button(LinearLayout body,String title,Runnable action){TextView button=Aero.button(getContext(),title,action);button.setContentDescription(title);LayoutParams p=new LayoutParams(-1,dp(48));p.topMargin=dp(6);body.addView(button,p);}
    private void setVolume(int level){if(audio==null||audio.isVolumeFixed())return;try{audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),level)),0);update();}catch(SecurityException ex){Toast.makeText(getContext(),"Use Sound settings to change this volume",Toast.LENGTH_SHORT).show();}}
    private int percent(){int level=battery==null?-1:battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=battery==null?0:battery.getIntExtra(BatteryManager.EXTRA_SCALE,100);return level<0||scale<1?-1:Math.min(100,Math.max(0,Math.round(level*100f/scale)));}
    private boolean charging(){int status=battery==null?0:battery.getIntExtra(BatteryManager.EXTRA_STATUS,0);return status==BatteryManager.BATTERY_STATUS_CHARGING||status==BatteryManager.BATTERY_STATUS_FULL;}
    private String network(){
        try{NetworkCapabilities caps=connections==null?null:connections.getNetworkCapabilities(connections.getActiveNetwork());if(caps==null)return "Not connected";String transport=caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)?"VPN":caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile network":caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet":"Network";return transport+" • "+(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Internet available":caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)?"Internet not verified":"Local connection");}catch(SecurityException ex){return "Open Network settings for connection details";}
    }
    private void update(){
        int count=ActionCenter.alerts(getContext(),new DesktopStore(getContext())).size();
        if(count!=alertCount){alertCount=count;actionCenter.invalidate();actionCenter.setTooltipText(count==0?"Action Center: no recent Nth alerts":"Action Center: "+count+" Nth alert"+(count==1?"":"s"));if(Build.VERSION.SDK_INT>=30)actionCenter.setStateDescription(count==0?"No recent Nth alerts":count+" Nth alert"+(count==1?"":"s"));}
        clockNow();for(TrayIcon icon:icons)icon.invalidate();int percentage=percent();icons.get(0).setContentDescription("Battery status"+(percentage<0?"":", "+percentage+"%"));icons.get(1).setContentDescription("Network status");
        if(summary==null)return;
        switch(page){case "actioncenter":summary.setText(alertCount==0?"No recent Nth alerts.":alertCount+" Nth alert"+(alertCount==1?"":"s"));break;case "battery":summary.setText((percentage<0?"Battery level unavailable":percentage+"% remaining")+"\n"+(charging()?"Plugged in":"On battery")+"\nBattery Saver: "+(power!=null&&power.isPowerSaveMode()?"On":"Off"));break;case "network":summary.setText(network());break;case "volume":int current=audio==null?0:audio.getStreamVolume(AudioManager.STREAM_MUSIC),max=audio==null?0:audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);summary.setText("Media volume: "+current+" / "+max+(audio!=null&&audio.isVolumeFixed()?"\nVolume is controlled by your output device":"\nRinger and Do Not Disturb follow Android settings."));if(volume!=null&&!tracking)volume.setProgress(current);break;case "brightness":summary.setText("Screen brightness");break;case "controls":summary.setText(java.text.DateFormat.getDateTimeInstance().format(new Date())+"\n"+(percentage<0?"":percentage+"% battery • ")+network());break;case "calendar":summary.setText(java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(new Date())+"\n"+TimeZone.getDefault().getDisplayName());break;}
    }
    private int dp(int n){return Aero.dp(getContext(),n);}
    private final class TrayFlag extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TrayFlag(Context context){super(context);}
        @Override protected void onDraw(Canvas canvas){
            float d=dp(22)/24f;int save=canvas.save();canvas.translate((getWidth()-dp(22))/2f,(getHeight()-dp(22))/2f);canvas.scale(d,d);
            paint.setColor(0xdd203348);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);canvas.drawLine(5,3,5,22,paint);
            paint.setStrokeWidth(1.5f);paint.setColor(Color.WHITE);canvas.drawLine(5,3,5,22,paint);
            Path flag=new Path();flag.moveTo(6,4);flag.cubicTo(11,1,15,7,21,4);flag.lineTo(21,14);flag.cubicTo(15,17,11,11,6,14);flag.close();paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(7,3,21,15,0xfff9fcff,0xffb2c3d1,Shader.TileMode.CLAMP));canvas.drawPath(flag,paint);paint.setShader(null);
            if(alertCount>0){paint.setColor(0xffecb23d);canvas.drawCircle(20,20,4,paint);paint.setColor(0xff614113);paint.setStrokeWidth(1.5f);canvas.drawLine(20,17.5f,20,20,paint);canvas.drawPoint(20,22,paint);}
            canvas.restoreToCount(save);
        }
    }
    private final class TrayIcon extends View {
        final int kind;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TrayIcon(Context c,int kind){super(c);this.kind=kind;}
        @Override protected void onDraw(Canvas original){super.onDraw(original);int save=original.save();original.translate((getWidth()-dp(22))/2f,(getHeight()-dp(22))/2f);original.scale(dp(22)/24f,dp(22)/24f);Canvas c=original;paint.setColor(Color.WHITE);paint.setStrokeWidth(1.5f);paint.setStyle(Paint.Style.STROKE);
            if(kind==0){paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(3,4,20,18,new int[]{0xfff7fbff,0xffa6bac7,0xff536c7c},null,Shader.TileMode.CLAMP));c.drawRoundRect(3,5,20,20,2,2,paint);paint.setShader(null);paint.setColor(0xff394d5a);c.drawRect(9,3,14,5,paint);c.drawRect(5,7,18,18,paint);int level=percent();paint.setShader(new LinearGradient(0,8,0,18,level>=0&&level<15?0xffff9470:0xffb9ed7f,level>=0&&level<15?0xffac3020:0xff3b872c,Shader.TileMode.CLAMP));if(level>=0)c.drawRect(6,17-9*level/100f,17,17,paint);paint.setShader(null);if(charging()){paint.setColor(0xffffdf69);Path bolt=new Path();bolt.moveTo(13,6);bolt.lineTo(8,13);bolt.lineTo(12,13);bolt.lineTo(10,20);bolt.lineTo(17,11);bolt.lineTo(13,11);bolt.close();c.drawPath(bolt,paint);}}
            else if(kind==1){paint.setStyle(Paint.Style.FILL);paint.setColor(0xffc6d9e6);c.drawRoundRect(1,3,14,13,1,1,paint);paint.setColor(0xff377fc0);c.drawRect(3,5,12,10,paint);paint.setColor(0xff6f8fa5);c.drawRect(6,13,9,16,paint);paint.setColor(0xffeff5fa);c.drawRoundRect(10,10,23,20,1,1,paint);paint.setColor(0xff347bb3);c.drawRect(12,12,21,17,paint);paint.setColor(0xff92ae86);c.drawRect(2,19,13,21,paint);paint.setColor(network().equals("Not connected")?0xffce4a33:0xff78bc55);c.drawCircle(19,20,3,paint);}
            else if(kind==3){paint.setStyle(Paint.Style.FILL);paint.setShader(new RadialGradient(11,10,7,new int[]{0xfffff6b3,0xffffd355,0xffcb8d20},null,Shader.TileMode.CLAMP));c.drawCircle(12,12,5,paint);paint.setShader(null);paint.setColor(0xffffd75c);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);for(int i=0;i<8;i++){double angle=i*Math.PI/4;c.drawLine(12+7*(float)Math.cos(angle),12+7*(float)Math.sin(angle),12+10*(float)Math.cos(angle),12+10*(float)Math.sin(angle),paint);}}
            else{Path speaker=new Path();speaker.moveTo(2,9);speaker.lineTo(7,9);speaker.lineTo(12,5);speaker.lineTo(12,19);speaker.lineTo(7,15);speaker.lineTo(2,15);speaker.close();paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(2,5,12,19,0xfff5f9fc,0xff8aa0b1,Shader.TileMode.CLAMP));c.drawPath(speaker,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffbce6ff);if(audio!=null&&audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0){paint.setColor(0xffef775a);c.drawLine(16,8,22,16,paint);c.drawLine(22,8,16,16,paint);}else{c.drawArc(10,6,19,18,-65,130,false,paint);c.drawArc(9,3,23,21,-65,130,false,paint);}}
            c.restoreToCount(save);
        }
    }
}
