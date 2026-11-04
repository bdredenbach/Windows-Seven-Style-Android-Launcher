package com.nthexperience.desktop;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** An optional in-launcher introduction. No boot receiver, background activity or system-boot replacement. */
final class StartupExperience {
    static final String SESSION="startup_session", HOME_OFFERED="home_prompt_offered";
    private final MainActivity activity;
    private final FrameLayout host;
    private final DesktopStore store;
    private final SharedPreferences session;
    private final Runnable welcome;
    private final boolean welcomeAtStart;
    private Intro intro;
    private boolean resumed,checked,welcomeChecked,playedHere;
    private int checkedBoot=Integer.MIN_VALUE;
    // Start the observation window with this Home session, even after a slow boot.
    private final long bootWatchDeadline=SystemClock.uptimeMillis()+90000;
    private final Runnable reconsider=this::maybeShow;
    StartupExperience(MainActivity a,FrameLayout host,DesktopStore store,boolean restored,Runnable welcome){activity=a;this.host=host;this.store=store;this.welcome=welcome;welcomeAtStart=store.prefs.getBoolean("welcome_on_start",false);session=a.getSharedPreferences(SESSION,0);welcomeChecked=restored&&session.getBoolean("welcome_seen",false);}
    static int boot(Context c){try{return Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);}catch(SecurityException ex){return -1;}}
    void resume(){resumed=true;host.removeCallbacks(reconsider);host.post(reconsider);}
    void pause(){resumed=false;host.removeCallbacks(reconsider);if(intro!=null)intro.finish(false);}
    void close(){resumed=false;host.removeCallbacks(reconsider);if(intro!=null)intro.finish(false);}
    boolean skip(){if(intro==null)return false;intro.finish(true);return true;}
    void preview(){if(intro==null&&resumed&&activity.hasWindowFocus())show(true);}
    void maybeShow(){
        host.removeCallbacks(reconsider);
        if(!resumed||!activity.hasWindowFocus())return;
        int currentBoot=boot(activity);
        // Some devices publish BOOT_COUNT after the first visible Home activity.
        // Reconcile that late identity while this early-boot session stays visible.
        if(playedHere&&currentBoot>=0&&currentBoot!=checkedBoot){checkedBoot=currentBoot;session.edit().putInt("boot",currentBoot).apply();}
        if(intro!=null){watchEarlyBoot();return;}
        if(host.getWidth()==0){host.postDelayed(reconsider,100);return;}
        // Unlock can finish after the focused Home activity resumes. Wait only while visible;
        // pause/close cancel this single pending check, and playback still requires an unlocked phone.
        KeyguardManager keyguard=activity.getSystemService(KeyguardManager.class);if(keyguard!=null&&keyguard.isKeyguardLocked()){host.postDelayed(reconsider,250);return;}
        if(!checked||currentBoot!=checkedBoot){checked=true;checkedBoot=currentBoot;int boot=currentBoot;if(!playedHere&&StartupPolicy.shouldPlay(store.prefs.getInt("startup_mode",0),session.getBoolean("seen",false),session.getInt("boot",-1),boot)){
            // Claim playback before showing it, so rotation, Home and process recreation cannot replay it.
            playedHere=true;welcomeChecked=false;session.edit().putBoolean("seen",true).putInt("boot",boot).putInt("runs",session.getInt("runs",0)+1).commit();show(false);watchEarlyBoot();return;
        }}
        watchEarlyBoot();finishSetup();
    }
    private void watchEarlyBoot(){if(resumed&&store.prefs.getInt("startup_mode",0)==1&&SystemClock.uptimeMillis()<bootWatchDeadline)host.postDelayed(reconsider,1000);}
    private void finishSetup(){
        if(!resumed||!activity.hasWindowFocus()||activity.offerInitialHome(session))return;
        activity.storageReady();if(welcomeChecked)return;welcomeChecked=true;if(!session.getBoolean("welcome_seen",false)||welcomeAtStart){
        session.edit().putBoolean("welcome_seen",true).apply();welcome.run();
    }}
    private void show(boolean preview){
        ((android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(host.getWindowToken(),0);
        intro=new Intro(preview);host.addView(intro,new FrameLayout.LayoutParams(-1,-1));intro.start();
    }
    private final class Intro extends FrameLayout {
        final boolean preview,animated;
        final Scene scene;
        final TextView name,welcomeText;
        final long duration;
        long started;
        boolean done,chimed;
        AudioTrack track;
        final Handler main=new Handler(Looper.getMainLooper());
        final Runnable frame=new Runnable(){public void run(){if(done)return;long elapsed=SystemClock.uptimeMillis()-started;float progress=Math.min(1,elapsed/(float)duration);scene.progress=animated?progress:1f;scene.invalidate();float text=animated?Math.max(0,Math.min(1,(progress-.58f)*5)):1;name.setAlpha(text);welcomeText.setAlpha(text);if(animated&&!chimed&&progress>=.25f){chimed=true;chime();}if(elapsed>=duration){finish(true);return;}postOnAnimation(this);}};
        Intro(boolean preview){
            super(activity);this.preview=preview;animated=Aero.motion(activity);duration=animated?3000:450;setContentDescription("Nth startup sequence");setClickable(true);setFocusableInTouchMode(true);setElevation(Aero.dp(activity,80));
            scene=new Scene(activity);addView(scene,new LayoutParams(-1,-1));
            LinearLayout identity=Aero.column(activity);identity.setGravity(Gravity.CENTER);name=Aero.text(activity,store.prefs.getString("desktop_name",activity.getString(R.string.app_name)),23,Color.WHITE);name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setShadowLayer(Aero.dp(activity,4),0,0,0xff7ac9f2);identity.addView(name,new LinearLayout.LayoutParams(-1,-2));welcomeText=Aero.text(activity,"Welcome",16,0xffccdeec);welcomeText.setGravity(Gravity.CENTER);identity.addView(welcomeText,new LinearLayout.LayoutParams(-1,Aero.dp(activity,35)));LayoutParams text=new LayoutParams(-1,-2,Gravity.CENTER);text.topMargin=Aero.dp(activity,84);text.leftMargin=text.rightMargin=Aero.dp(activity,24);addView(identity,text);
            TextView skip=Aero.text(activity,"Skip",14,0xffd7e9f6);skip.setGravity(Gravity.CENTER);skip.setBackground(Aero.panel(activity,0x332c536b,0x33122d42,3,0x667ca0b8));skip.setContentDescription("Skip startup");skip.setFocusable(true);skip.setOnClickListener(v->finish(true));LayoutParams lp=new LayoutParams(Aero.dp(activity,112),Aero.dp(activity,44),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);lp.bottomMargin=Aero.dp(activity,26);addView(skip,lp);
            setOnApplyWindowInsetsListener((v,insets)->{int left=0,top=0,right=0,bottom=0;if(Build.VERSION.SDK_INT>=30){Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=safe.left;top=safe.top;right=safe.right;bottom=safe.bottom;}else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}setPadding(left,top,right,bottom);return insets;});
        }
        void start(){started=SystemClock.uptimeMillis();requestApplyInsets();requestFocus();post(frame);}
        void finish(boolean completed){if(done)return;done=true;removeCallbacks(frame);if(track!=null){try{track.stop();}catch(IllegalStateException ignored){}track.release();track=null;}host.removeView(this);intro=null;if(completed&&!preview)finishSetup();}
        private boolean soundAllowed(){AudioManager audio=activity.getSystemService(AudioManager.class);NotificationManager notification=activity.getSystemService(NotificationManager.class);return store.prefs.getBoolean("startup_sound",false)&&audio!=null&&audio.getRingerMode()==AudioManager.RINGER_MODE_NORMAL&&audio.getStreamVolume(AudioManager.STREAM_SYSTEM)>0&&!audio.isMusicActive()&&(notification==null||notification.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_ALL);}
        private void chime(){if(!soundAllowed())return;new Thread(()->{AudioTrack created=null;try{int rate=22050;short[] pcm=new short[rate*13/10];double[] frequency={523.251,659.255,783.991,1046.502};for(int i=0;i<pcm.length;i++){double t=i/(double)rate,sample=0;for(int n=0;n<frequency.length;n++){double age=t-n*.12;if(age<0)continue;double envelope=(1-Math.exp(-age*45))*Math.exp(-age*4.5);sample+=Math.sin(2*Math.PI*frequency[n]*age)*envelope;}pcm[i]=(short)(sample*4300);}created=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.length*2).build();created.write(pcm,0,pcm.length);AudioTrack sound=created;main.post(()->{if(done||!resumed||!soundAllowed()){sound.release();return;}track=sound;try{sound.setVolume(.45f);sound.play();}catch(IllegalStateException ignored){sound.release();track=null;}});}catch(RuntimeException ex){if(created!=null)created.release();}},"Nth-original-chime").start();}
    }
    private static final class Scene extends View {
        private static final int[] BLUES = {0xff54dcff, 0xff307df3, 0xff47aafa, 0xff91e9ff};
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path flag = new Path();
        private final Shader[] silk = new Shader[BLUES.length];
        private final android.graphics.drawable.Drawable pearl;
        private Shader glow;
        private float centerX, centerY, radius;
        float progress;

        Scene(Context context) {
            super(context);
            pearl = context.getDrawable(R.drawable.ic_launcher).mutate();
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            flag.moveTo(-1, -.72f);
            flag.cubicTo(-.28f, -1.42f, .35f, -.30f, 1, -.86f);
            flag.lineTo(.88f, .69f);
            flag.cubicTo(.18f, 1.39f, -.38f, .35f, -1.04f, .91f);
            flag.close();
            for (int i = 0; i < silk.length; i++)
                silk[i] = new LinearGradient(-1, -1, 1, 1, 0xffe0fcff, BLUES[i], Shader.TileMode.CLAMP);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            centerX = width / 2f;
            centerY = height / 2f - Aero.dp(getContext(), 30);
            radius = Math.min(Aero.dp(getContext(), 70), Math.min(width, height) * .19f);
            if (radius <= 0) return;
            glow = new RadialGradient(centerX, centerY, radius * 3.1f,
                    new int[]{0xff276fbf, 0x001b448f}, null, Shader.TileMode.CLAMP);
            pearl.setBounds(Math.round(centerX - radius), Math.round(centerY - radius),
                    Math.round(centerX + radius), Math.round(centerY + radius));
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xff02050c);
            if (radius <= 0) return;
            float merge = ease((progress - .66f) / .19f);
            float light = ease(progress / .4f);
            paint.setShader(glow);
            paint.setAlpha(Math.round(light * (68 + 74 * merge)));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
            if (merge < 1) for (int i = 0; i < BLUES.length; i++) {
                float t = Math.max(0, progress - i * .026f);
                float gather = ease(t / .78f);
                float orbit = radius * (2.17f - 1.98f * gather);
                float angle = i * (float) Math.PI / 2 + t * 9.5f;
                float alpha = ease(t / .12f) * (1 - merge);
                for (int tail = 5; tail >= 1; tail--) {
                    float behind = angle - .12f * tail;
                    paint.setColor(BLUES[i]);
                    paint.setAlpha(Math.round(alpha * (100 - tail * 15)));
                    canvas.drawCircle(centerX + (float)Math.cos(behind) * orbit,
                            centerY + (float)Math.sin(behind) * orbit * .68f,
                            radius * (.084f - .008f * tail), paint);
                }
                canvas.save();
                canvas.translate(centerX + (float)Math.cos(angle) * orbit,
                        centerY + (float)Math.sin(angle) * orbit * .68f);
                canvas.rotate((float)Math.toDegrees(angle) + 54);
                float scale = radius * (.17f + .17f * gather);
                canvas.scale(scale, scale);
                paint.setShader(silk[i]);
                paint.setAlpha(Math.round(255 * alpha));
                canvas.drawPath(flag, paint);
                paint.setShader(null);
                canvas.restore();
            }
            if (merge > 0) {
                pearl.setAlpha(Math.round(255 * merge));
                pearl.draw(canvas);
            }
        }

        private static float ease(float value) {
            float t = Math.max(0, Math.min(1, value));
            return t * t * (3 - 2 * t);
        }
    }
}
