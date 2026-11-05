package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Base64;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** An opt-in record of launcher actions and manually captured desktop images. */
final class StepRecorder {
    private static final int MAX_STEPS=100,MAX_PICTURES=6;
    private final List<Step> steps=new ArrayList<>();
    private long started;
    private boolean recording;
    private static final class Step {
        final long seconds;
        final String description;
        final byte[] image;
        Step(long seconds,String description,byte[] image){this.seconds=seconds;this.description=description;this.image=image;}
    }

    boolean active(){return recording;}
    boolean empty(){return steps.isEmpty();}
    int size(){return steps.size();}
    static File exportFile(Context context){return new File(context.getCacheDir(),"nth-seven-steps.html");}

    void start(){
        steps.clear();started=SystemClock.elapsedRealtime();recording=true;
        record("Started recording Nth Desktop Seven");
    }
    void stop(){
        record("Stopped recording");
        recording=false;
    }
    void record(String action){if(recording&&steps.size()<MAX_STEPS)append(action,null);}
    private void append(String action,byte[] image){
        steps.add(new Step((SystemClock.elapsedRealtime()-started)/1000,action,image));
    }
    boolean capture(View view){
        if(!recording||steps.size()>=MAX_STEPS||view.getWidth()<1||view.getHeight()<1)return false;
        int pictures=0;
        for(Step step:steps)if(step.image!=null)pictures++;
        if(pictures>=MAX_PICTURES)return false;
        float scale=Math.min(1f,840f/Math.max(view.getWidth(),view.getHeight()));
        Bitmap bitmap=Bitmap.createBitmap(Math.max(1,Math.round(view.getWidth()*scale)),
                Math.max(1,Math.round(view.getHeight()*scale)),Bitmap.Config.ARGB_8888);
        try{
            Canvas canvas=new Canvas(bitmap);canvas.scale(scale,scale);view.draw(canvas);
            ByteArrayOutputStream image=new ByteArrayOutputStream();
            if(!bitmap.compress(Bitmap.CompressFormat.JPEG,77,image)||image.size()>512*1024)return false;
            append("Captured the desktop",image.toByteArray());
            return true;
        }catch(RuntimeException unavailable){return false;}
        finally{bitmap.recycle();}
    }
    boolean removePicture(){
        for(int i=steps.size()-1;i>=0;i--)if(steps.get(i).image!=null){steps.remove(i);return true;}
        return false;
    }
    android.widget.LinearLayout review(Context context){
        android.widget.LinearLayout content=Aero.column(context);
        Aero.pad(content,12);
        for(Step step:steps){
            android.widget.TextView label=Aero.text(context,step.seconds+"s  "+step.description,13,Aero.INK);
            Aero.pad(label,6);content.addView(label);
            if(step.image!=null){
                android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();
                options.inSampleSize=2;
                Bitmap picture=android.graphics.BitmapFactory.decodeByteArray(step.image,0,step.image.length,options);
                if(picture==null)continue;
                android.widget.ImageView image=new android.widget.ImageView(context);
                image.setAdjustViewBounds(true);image.setImageBitmap(picture);
                image.setContentDescription("Recorded desktop screenshot");
                content.addView(image,new android.widget.LinearLayout.LayoutParams(-1,-2));
            }
        }
        return content;
    }
    byte[] export(){
        StringBuilder html=new StringBuilder("<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><title>Nth Desktop Seven Steps</title>")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style>body{font:16px system-ui;margin:2em;max-width:60em}li{margin:1em 0}img{display:block;max-width:100%;height:auto;margin:.5em 0}</style>")
                .append("<h1>Nth Desktop Seven Steps</h1><ol>");
        for(Step step:steps){
            html.append("<li><b>").append(step.seconds).append("s</b> ").append(escape(step.description));
            if(step.image!=null)html.append("<img alt=\"Desktop screenshot\" src=\"data:image/jpeg;base64,")
                    .append(Base64.encodeToString(step.image,Base64.NO_WRAP)).append("\">");
            html.append("</li>");
        }
        return html.append("</ol></html>").toString().getBytes(StandardCharsets.UTF_8);
    }
    private String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
