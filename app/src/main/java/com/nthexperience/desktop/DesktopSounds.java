package com.nthexperience.desktop;

import android.app.NotificationManager;
import android.content.Context;
import android.media.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import java.util.HashMap;
import java.util.Map;

/** Original opt-in sound schemes, respecting Android sound settings and existing media. */
final class DesktopSounds {
    static final String SCHEME="sound_scheme";
    private static long last;private static AudioTrack track;
    private static final Map<String,short[]> samples=new HashMap<>();
    static String scheme(DesktopStore store){String value=store.prefs.getString(SCHEME,SoundSchemes.DEFAULT);return SoundSchemes.valid(value)?value:SoundSchemes.DEFAULT;}
    static void play(Context context,String event){play(context,event,false);}
    static boolean preview(Context context,String event){return play(context,event,true);}
    private static synchronized boolean play(Context context,String event,boolean preview){
        DesktopStore store=new DesktopStore(context);AudioManager audio=context.getSystemService(AudioManager.class);
        NotificationManager notices=context.getSystemService(NotificationManager.class);
        if(!preview&&(!store.prefs.getBoolean("desktop_sounds",false)||!store.prefs.getBoolean("sound_"+event,true)))return false;
        if(audio==null||audio.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL||audio.getStreamVolume(AudioManager.STREAM_SYSTEM)==0
                ||audio.isMusicActive()||notices!=null&&notices.getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_ALL
                ||store.prefs.getInt("desktop_sound_volume",30)==0)return false;
        if(preview)stop();else if(SystemClock.uptimeMillis()-last<180||track!=null)return false;
        last=SystemClock.uptimeMillis();String key=scheme(store)+":"+event;
        short[] pcm=samples.get(key);if(pcm==null){pcm=SoundSchemes.samples(scheme(store),event);samples.put(key,pcm);}
        try{
            AudioTrack sound=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(SoundSchemes.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length*2).build();
            track=sound;sound.write(pcm,0,pcm.length);sound.setVolume(store.prefs.getInt("desktop_sound_volume",30)/100f);sound.play();
            new Handler(Looper.getMainLooper()).postDelayed(()->{synchronized(DesktopSounds.class){if(track==sound)track=null;}try{sound.release();}catch(RuntimeException ignored){}},pcm.length*1000L/SoundSchemes.RATE+90);
            return true;
        }catch(RuntimeException unavailable){stop();return false;}
    }
    static synchronized void stop(){if(track!=null){try{track.release();}catch(RuntimeException ignored){}track=null;}}
    static void settings(Context context,LinearLayout body){
        DesktopStore store=new DesktopStore(context);
        TextView title=Aero.text(context,"Sound scheme",13,Aero.INK);body.addView(title);
        Spinner schemes=new Spinner(context);schemes.setContentDescription("Desktop sound scheme");
        ArrayAdapter<String> adapter=new ArrayAdapter<>(context,android.R.layout.simple_spinner_dropdown_item,SoundSchemes.NAMES);
        schemes.setAdapter(adapter);schemes.setSelection(SoundSchemes.index(scheme(store)));
        schemes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){store.prefs.edit().putString(SCHEME,SoundSchemes.IDS[position]).apply();}
            public void onNothingSelected(AdapterView<?> parent){}
        });
        body.addView(schemes,new LinearLayout.LayoutParams(-1,Aero.dp(context,48)));
        for(String[] option:new String[][]{{"desktop_sounds","Enable desktop sound effects"},{"sound_complete","File operation completed"},{"sound_error","Errors"},{"sound_notice","Desktop notifications"}}){
            CheckBox check=Aero.check(context,option[1],store.prefs.getBoolean(option[0],!option[0].equals("desktop_sounds")));
            check.setOnCheckedChangeListener((button,value)->store.prefs.edit().putBoolean(option[0],value).apply());body.addView(check);
        }
        SeekBar volume=new SeekBar(context);volume.setContentDescription("Desktop sound volume");volume.setMax(100);volume.setProgress(store.prefs.getInt("desktop_sound_volume",30));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){play(context,"complete");}
            public void onProgressChanged(SeekBar b,int value,boolean user){if(user)store.prefs.edit().putInt("desktop_sound_volume",value).apply();}
        });body.addView(volume);
        LinearLayout tests=new LinearLayout(context);
        String[] labels={"Test complete","Test error","Test notification"};
        for(int i=0;i<labels.length;i++){
            String event=SoundSchemes.EVENTS[i];
            TextView test=Aero.button(context,labels[i],()->{if(!preview(context,event))Toast.makeText(context,"Sound previews follow Android sound settings, Nth volume, and active media.",Toast.LENGTH_SHORT).show();});
            test.setContentDescription(labels[i]+" sound");test.setMaxLines(2);
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1);size.rightMargin=Aero.dp(context,3);tests.addView(test,size);
        }
        body.addView(tests,new LinearLayout.LayoutParams(-1,-2));
        body.addView(Aero.text(context,"Sound schemes are included in saved themes. Test plays a preview without enabling automatic sounds. Silent mode, Do Not Disturb and system volume are respected; startup keeps its own setting.",12,0xff496171));
    }
}
