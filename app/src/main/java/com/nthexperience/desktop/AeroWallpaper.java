package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Original, resolution-independent aurora artwork. No Microsoft assets. */
final class AeroWallpaper extends View {
    static final int SEVEN = 3;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final SevenScenery scenery=new SevenScenery();
    private List<String> slides=Collections.emptyList();
    private List<String> order=Collections.emptyList();
    private int cursor,interval;
    private boolean shuffle,resumed;
    private String rendered="";
    private final Runnable rotation=this::next;
    private int theme;private boolean following,closed;private Bitmap photo;private int photoWidth,photoHeight,background=0xff0a263b;private String position="Fill";private android.os.CancellationSignal request;
    private static final java.util.concurrent.ExecutorService images=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static Bitmap artwork,wideArtwork;
    private final java.util.List<java.lang.ref.WeakReference<AeroGlass>> surfaces=new java.util.ArrayList<>();
    private Bitmap softened;
    private int softWidth,softHeight,softTheme=-1;
    private final int[] ownLocation=new int[2],targetLocation=new int[2];
    AeroWallpaper(Context context,int theme){this(context,theme,true);}
    AeroWallpaper(Context context, int theme,boolean following) { super(context); this.theme = theme;this.following=following;
        if(artwork==null){try{artwork=BitmapFactory.decodeStream(context.getAssets().open("wallpapers/aero-aurora.webp"));}catch(java.io.IOException ignored){}} if(wideArtwork==null){try{wideArtwork=BitmapFactory.decodeStream(context.getAssets().open("wallpapers/aero-aurora-landscape.webp"));}catch(java.io.IOException ignored){}} setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);if(following)reload(); }
    void theme(int value) { if(following)reload();else theme=value;softened=null;appearanceChanged(); }
    void appearanceChanged(){invalidate();for(java.util.Iterator<java.lang.ref.WeakReference<AeroGlass>> i=surfaces.iterator();i.hasNext();){AeroGlass glass=i.next().get();if(glass==null)i.remove();else glass.invalidateSelf();}}
    void register(AeroGlass glass){surfaces.add(new java.lang.ref.WeakReference<>(glass));}
    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if(photo!=null&&!photo.isRecycled()){drawPhoto(c,w,h);return;}
        if(theme==SEVEN){drawSeven(c,w,h);return;}
        if(theme>SEVEN&&theme<DesktopBackgrounds.NAMES.length){scenery.draw(c,w,h,theme);return;}
        if(artwork!=null){
            drawArtwork(c,w,h);return;
        }
        int[][] colors = {{0xff08263f,0xff156d82,0xff79bcb1}, {0xff141c41,0xff49427c,0xffbc8cae}, {0xff102c30,0xff347e6c,0xffc4d8aa}};
        int[] palette = colors[Math.floorMod(theme, colors.length)];
        paint.setShader(new LinearGradient(0, 0, w*.8f, h, palette, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(new RadialGradient(w*.88f, h*.47f, h*.66f, new int[]{0x887ae8de,0x0024b6cc}, null, Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,paint);
        path.reset(); path.moveTo(-w*.3f,h*.82f);
        path.cubicTo(w*.1f,h*.16f,w*.9f,h*.84f,w*1.3f,h*.14f);
        path.cubicTo(w*.75f,h*.98f,w*.27f,h*.45f,-w*.3f,h*1.1f); path.close();
        paint.setShader(new LinearGradient(0,h*.25f,w,h, new int[]{0x002ae3ca,0x6684f6bf,0x08c4f9d9},null,Shader.TileMode.CLAMP));
        c.drawPath(path,paint);
        path.reset(); path.moveTo(-w*.1f,h*.91f);
        path.cubicTo(w*.26f,h*.5f,w*.8f,h*.86f,w*1.2f,h*.36f);
        path.cubicTo(w*.78f,h*1.02f,w*.27f,h*.72f,-w*.1f,h*1.07f); path.close();
        paint.setShader(new LinearGradient(0,h*.5f,w,h,new int[]{0x001c8faf,0x668fdcdf,0x00c4f9d9},null,Shader.TileMode.CLAMP));
        c.drawPath(path,paint);
        paint.setShader(null); paint.setColor(0x11ffffff); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f);
        path.reset(); path.moveTo(-w*.2f,h*.9f); path.cubicTo(w*.22f,h*.38f,w*.88f,h*.9f,w*1.2f,h*.27f);
        c.drawPath(path,paint); paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x08ffffff);
        for(int i=0;i<24;i++) { float x=((i*167+41)%997)/997f*w, y=((i*107+73)%997)/997f*h; c.drawCircle(x,y,1+(i%3),paint); }
    }
    void draft(int theme,Bitmap image,int width,int height,String position,int background){this.theme=theme;photo=image;photoWidth=width;photoHeight=height;this.position=position;this.background=background;softened=null;appearanceChanged();}
    void resumeSlides(){resumed=true;reload();}
    void pauseSlides(){resumed=false;removeCallbacks(rotation);}
    boolean hasSlideshow(){return slides.size()>1;}
    void reload(){
        if(closed||!following)return;
        android.content.SharedPreferences prefs=getContext().getSharedPreferences("desktop",0);
        String placement=prefs.getString("wallpaper_position","Fill");
        int color=BackgroundPicker.COLORS[Math.max(0,Math.min(BackgroundPicker.COLORS.length-1,prefs.getInt("wallpaper_bg",0)))];
        boolean geometryChanged=!position.equals(placement)||color!=background;
        position=placement;background=color;
        List<String> selected=DesktopBackgrounds.current(prefs);
        boolean random=prefs.getBoolean(DesktopBackgrounds.SHUFFLE,false);
        interval=prefs.getInt(DesktopBackgrounds.INTERVAL,DesktopBackgrounds.DEFAULT_INTERVAL);
        if(!DesktopBackgrounds.intervalValid(interval))interval=DesktopBackgrounds.DEFAULT_INTERVAL;
        if(!selected.equals(slides)||random!=shuffle){
            slides=selected;shuffle=random;cursor=0;
            order=new ArrayList<>(slides);
            if(shuffle&&order.size()>2)Collections.shuffle(order.subList(1,order.size()));
        }
        display(order.get(cursor));
        if(geometryChanged){softened=null;appearanceChanged();}
        schedule();
    }
    void next(){
        if(closed||slides.size()<2)return;
        String previous=order.get(cursor);
        cursor=(cursor+1)%order.size();
        if(cursor==0&&shuffle){
            Collections.shuffle(order);
            if(order.get(0).equals(previous))Collections.swap(order,0,1);
        }
        display(order.get(cursor));schedule();
    }
    private void schedule(){
        removeCallbacks(rotation);
        if(resumed&&slides.size()>1)postDelayed(rotation,interval*1000L);
    }
    private void display(String key){
        if(key.equals(rendered))return;
        rendered=key;
        if(request!=null)request.cancel();
        int id=DesktopBackgrounds.stockId(key);
        theme=id>=0?id:getContext().getSharedPreferences("desktop",0).getInt("wallpaper",SEVEN);
        photo=null;softened=null;appearanceChanged();
        if(id>=0)return;
        android.os.CancellationSignal signal=new android.os.CancellationSignal();request=signal;
        android.content.Context context=getContext().getApplicationContext();
        images.execute(()->{
            PhotoImage result=null;
            try{result=PhotoImage.read(context,android.net.Uri.parse(key),signal,2048,4000000);}
            catch(Exception|OutOfMemoryError ignored){}
            PhotoImage decoded=result;
            post(()->{
                if(closed||signal.isCanceled()){if(decoded!=null)decoded.bitmap.recycle();return;}
                if(decoded==null)rendered="";
                photo=decoded==null?null:decoded.bitmap;
                photoWidth=decoded==null?0:decoded.width;photoHeight=decoded==null?0:decoded.height;
                softened=null;appearanceChanged();
            });
        });
    }
    private void drawPhoto(Canvas canvas,float width,float height){canvas.drawColor(background);paint.setShader(null);paint.setColorFilter(null);paint.setAlpha(255);paint.setFilterBitmap(true);float scale=position.equals("Fit")?Math.min(width/photo.getWidth(),height/photo.getHeight()):Math.max(width/photo.getWidth(),height/photo.getHeight());float w=photo.getWidth()*scale,h=photo.getHeight()*scale;
        if(position.equals("Stretch")){w=width;h=height;}
        if(position.equals("Center")||position.equals("Tile")){float displayScale=following?1:width/Math.max(1,getResources().getDisplayMetrics().widthPixels);w=Math.max(1,photoWidth)*displayScale;h=Math.max(1,photoHeight)*displayScale;}
        if(position.equals("Tile")){BitmapShader shader=new BitmapShader(photo,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);Matrix matrix=new Matrix();matrix.setScale(w/photo.getWidth(),h/photo.getHeight());shader.setLocalMatrix(matrix);paint.setShader(shader);canvas.drawRect(0,0,width,height,paint);paint.setShader(null);}else canvas.drawBitmap(photo,null,new RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),paint);
    }
    void close(){closed=true;pauseSlides();if(request!=null)request.cancel();photo=null;softened=null;}
    private void drawSeven(Canvas canvas, float width, float height) {
        if (width <= 0 || height <= 0) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[]{0xff071a42, 0xff104890, 0xff0879bd, 0xff052b5a},
                new float[]{0, .35f, .68f, 1}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(new RadialGradient(width * .79f, height * .53f, Math.max(width, height) * .63f,
                new int[]{0xb65ce5ff, 0x3658b7ff, 0x004486cd}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(new LinearGradient(0, 0, width, 0,
                new int[]{0x00a1ebff, 0xa6b5f7ff, 0x0085cfff}, null, Shader.TileMode.CLAMP));
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 3; i++) {
            float lift = i * .035f;
            path.rewind();
            path.moveTo(-width * .11f, height * (.94f + lift));
            path.cubicTo(width * .23f, height * (.48f + lift), width * .64f, height * (.96f + lift),
                    width * 1.1f, height * (.38f + lift));
            paint.setStrokeWidth(Math.max(1, Math.min(width, height) * (.0016f + i * .0009f)));
            canvas.drawPath(path, paint);
        }
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawArtwork(Canvas c,float w,float h){
        Bitmap image=w>h&&wideArtwork!=null?wideArtwork:artwork;
        float scale=Math.max(w/image.getWidth(),h/image.getHeight());
        float rw=image.getWidth()*scale,rh=image.getHeight()*scale;
        paint.setShader(null);paint.setFilterBitmap(true);
        if(theme==1){float[] m={.65f,0,.45f,0,10, 0,.5f,0,0,0, .35f,0,1,0,15, 0,0,0,1,0};paint.setColorFilter(new ColorMatrixColorFilter(m));}
        else if(theme==2){float[] m={.55f,.25f,0,0,0, .05f,.9f,0,0,0, 0,.25f,.5f,0,0, 0,0,0,1,0};paint.setColorFilter(new ColorMatrixColorFilter(m));}
        else paint.setColorFilter(null);
        c.drawBitmap(image,null,new RectF((w-rw)/2,(h-rh)/2,(w+rw)/2,(h+rh)/2),paint);paint.setColorFilter(null);
    }
    void drawGlass(Canvas canvas,View target){
        if(getWidth()<1||getHeight()<1)return;
        if(softened==null||softWidth!=getWidth()||softHeight!=getHeight()||softTheme!=theme){
            softWidth=getWidth();softHeight=getHeight();softTheme=theme;
            int w=Math.max(1,Math.round(getWidth()*96f/getHeight()));softened=Bitmap.createBitmap(w,96,Bitmap.Config.ARGB_8888);
            Canvas small=new Canvas(softened);small.scale(w/(float)getWidth(),96f/getHeight());onDraw(small);
        }
        getLocationOnScreen(ownLocation);target.getLocationOnScreen(targetLocation);
        float x=ownLocation[0]-targetLocation[0],y=ownLocation[1]-targetLocation[1];
        paint.setShader(null);paint.setAlpha(255);paint.setFilterBitmap(true);canvas.drawBitmap(softened,null,new RectF(x,y,x+getWidth(),y+getHeight()),paint);
    }

}
