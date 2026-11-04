package com.nthexperience.desktop;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Samples the softened wallpaper beneath the chrome, keeping foreground text sharp. */
final class AeroGlass extends Drawable {
    static final int WINDOW=0,START=1,TASKBAR=2,SIDEBAR=3,TASKBUTTON=4;
    private final AeroWallpaper wallpaper;
    private final int style;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF rect=new RectF();
    private final Path clip=new Path();
    private boolean active=true;
    private boolean stacked;
    private int insetTop;
    private int drawableAlpha=255;
    static final int DEFAULT_OPACITY=85;
    private final android.content.SharedPreferences prefs;
    static final String[] COLOR_NAMES={"Frost","Graphite","Seafoam","Sage","Lavender","Gold","Ruby","Clear"};
    static final int[] TINTS={0xff599cbd,0xff58636e,0xff3e9ca0,0xff6d986c,0xff8b78b0,0xffb69a5c,0xffa56872,0xffb4cad7};
    AeroGlass(AeroWallpaper wallpaper,int style){this.wallpaper=wallpaper;this.style=style;prefs=wallpaper.getContext().getSharedPreferences("desktop",0);wallpaper.register(this);}
    void active(boolean value){if(active!=value){active=value;invalidateSelf();}}
    void stacked(boolean value){stacked=value;invalidateSelf();}
    void insetTop(int pixels){insetTop=pixels;invalidateSelf();}
    @Override public void draw(Canvas c){
        rect.set(getBounds());rect.top+=insetTop;if(rect.isEmpty())return;float d=getCallback() instanceof View?((View)getCallback()).getResources().getDisplayMetrics().density:1;
        float radius=(style==TASKBAR||style==SIDEBAR)?0:6*d;
        boolean transparent=prefs.getBoolean("aero_transparency",true);
        int opacity=transparent?Math.max(0,Math.min(100,prefs.getInt("aero_opacity",DEFAULT_OPACITY))):100;
        int alpha=Math.round(drawableAlpha*opacity/100f);if(alpha==0)return;
        // Composite the whole surface once: sampling, tint, reflection and edging share one opacity.
        int layer=c.saveLayerAlpha(rect,alpha);clip.reset();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);int save=c.save();c.clipPath(clip);
        p.setColor(0xff0d1e2b);p.setAlpha(255);c.drawRect(rect,p);
        if(transparent&&getCallback() instanceof View)wallpaper.drawGlass(c,(View)getCallback());
        int[] colors;
        if(style==WINDOW)colors=new int[]{0xb2bedbe8,0x88b2d3e4,0x9b7f9db1,0xccc0d9e7};
        else if(style==START)colors=new int[]{0xdb426584,0xd32a4967,0xe01d3550,0xe42e4c68};
        else if(style==TASKBUTTON)colors=active?new int[]{0xeb557e98,0xe62c526b,0xee234056,0xe947718c}:new int[]{0x667c94a4,0x55536c7e,0x55384f60,0x66647e91};
        else if(style==TASKBAR)colors=new int[]{0xe75b809f,0xdd365574,0xee19354f,0xf011253b};
        else colors=new int[]{0x33212b33,0x4423313e,0x55202a34,0x55202a34};
        int tint=TINTS[Math.max(0,Math.min(TINTS.length-1,prefs.getInt("aero_color",0)))];float intensity=Math.max(0,Math.min(100,prefs.getInt("aero_intensity",45)))/100f;
        for(int i=0;i<colors.length;i++){int tintAlpha=transparent?Color.alpha(colors[i]):255;int target=style==WINDOW?mix(0xffe2edf3,tint,.6f):style==SIDEBAR?mix(0xff0b2435,tint,.35f):mix(0xff0d1e2b,tint,.45f);colors[i]=(mix(colors[i],target,intensity*.82f)&0x00ffffff)|(tintAlpha<<24);if(style==WINDOW&&!active)colors[i]=mix(colors[i],0xffc6d0d5,.38f)|(tintAlpha<<24);}
        p.setShader(new LinearGradient(0,rect.top,0,rect.bottom,colors,null,Shader.TileMode.CLAMP));p.setAlpha(255);c.drawRect(rect,p);p.setShader(null);
        if(style!=SIDEBAR){
            p.setShader(new LinearGradient(0,0,rect.width(),rect.height(),new int[]{0x00ffffff,0x20ffffff,0x00ffffff,0x10ffffff},new float[]{0,.23f,.45f,1},Shader.TileMode.CLAMP));
            Path beam=new Path();beam.moveTo(rect.width()*.05f,0);beam.lineTo(rect.width()*.35f,0);beam.lineTo(rect.width()*.78f,rect.height());beam.lineTo(rect.width()*.48f,rect.height());beam.close();c.drawPath(beam,p);p.setShader(null);
        }
        c.restoreToCount(save);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(style==WINDOW?0xc5f3fbff:0x90b8ced9);c.drawRoundRect(rect.left+d*.5f,rect.top+d*.5f,rect.right-d*.5f,rect.bottom-d*.5f,radius,radius,p);
        p.setColor(0x55000000);c.drawLine(rect.left,rect.bottom-d,rect.right,rect.bottom-d,p);
        if(stacked&&rect.width()>12*d){
            p.setColor(0xb5c6e4f5);
            for(int i=1;i<=2;i++)c.drawLine(rect.right-(2+3*i)*d,rect.top+(4-i)*d,rect.right-(2+3*i)*d,rect.bottom-(4-i)*d,p);
        }
        p.setStyle(Paint.Style.FILL);c.restoreToCount(layer);
    }
    private static int mix(int a,int b,float t){return Color.argb(Color.alpha(a),Math.round(Color.red(a)*(1-t)+Color.red(b)*t),Math.round(Color.green(a)*(1-t)+Color.green(b)*t),Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));}
    @Override public void setAlpha(int alpha){drawableAlpha=Math.max(0,Math.min(255,alpha));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
