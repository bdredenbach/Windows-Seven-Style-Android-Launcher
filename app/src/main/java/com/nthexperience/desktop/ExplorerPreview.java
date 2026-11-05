package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded previews of readable files selected in Nth Explorer. */
final class ExplorerPreview extends LinearLayout {
    private final TextView title,description,open;
    private final ImageView image;
    private final ScrollView text;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private CancellationSignal request;
    private Bitmap bitmap;

    ExplorerPreview(Context context,Runnable hide){
        super(context);
        setOrientation(VERTICAL);
        setContentDescription("Explorer Preview Pane");
        setBackground(Aero.panel(context,0xfff7fbff,0xffe8f0f8,1,0xffa6bbce));
        LinearLayout bar=new LinearLayout(context);
        title=Aero.text(context,"Preview Pane",13,0xff214567);title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bar.addView(title,new LayoutParams(0,-1,1));
        TextView close=Aero.button(context,"×",hide);close.setContentDescription("Hide Preview Pane");
        close.setMinHeight(0);close.setPadding(0,0,0,0);
        bar.addView(close,new LayoutParams(Aero.dp(context,38),-1));
        addView(bar,new LayoutParams(-1,Aero.dp(context,32)));
        android.widget.FrameLayout content=new android.widget.FrameLayout(context);
        description=Aero.text(context,"Select a picture or text file to preview it.",12,Aero.INK);
        description.setTextIsSelectable(true);
        text=new ScrollView(context);text.setFillViewport(true);text.addView(description);
        content.addView(text,new android.widget.FrameLayout.LayoutParams(-1,-1));
        image=new ImageView(context);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setVisibility(GONE);
        content.addView(image,new android.widget.FrameLayout.LayoutParams(-1,-1));
        addView(content,new LayoutParams(-1,0,1));
        open=Aero.button(context,"Open file",()->{});open.setVisibility(GONE);open.setMinHeight(0);
        addView(open,new LayoutParams(-1,Aero.dp(context,32)));
    }

    void show(DocumentOps.Ref file,Runnable action){
        clear();
        title.setText(file.item.name);
        open.setVisibility(VISIBLE);open.setOnClickListener(v->action.run());
        String mime=FileLaunch.mime(file.item.name,file.item.mime);
        boolean picture=mime.startsWith("image/");
        boolean readableText=mime.startsWith("text/")||mime.equals("application/json");
        if(!picture&&!readableText){description.setText("Preview unavailable for this file type.");return;}
        description.setText("Loading preview…");
        CancellationSignal signal=new CancellationSignal();
        request=signal;
        worker.execute(()->{
            Bitmap decoded=null;
            String sample=null;
            try{
                if(picture)decoded=PhotoImage.read(getContext(),file.uri,signal,640,640L*640).bitmap;
                else sample=read(file,signal);
            }catch(Exception unavailable){}
            Bitmap result=decoded;
            String body=sample;
            main.post(()->{
                if(request!=signal||signal.isCanceled()){
                    if(result!=null)result.recycle();
                    return;
                }
                if(result!=null){
                    bitmap=result;image.setImageBitmap(bitmap);image.setVisibility(VISIBLE);
                    image.setContentDescription("Preview of "+file.item.name);
                    text.setVisibility(GONE);
                }else description.setText(body==null?"This file could not be previewed.":body);
            });
        });
    }

    private String read(DocumentOps.Ref file,CancellationSignal signal)throws Exception{
        ParcelFileDescriptor descriptor=getContext().getContentResolver().openFileDescriptor(file.uri,"r",signal);
        if(descriptor==null)return null;
        try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
            ByteArrayOutputStream data=new ByteArrayOutputStream()){
            byte[] buffer=new byte[2048];
            while(data.size()<8193){
                signal.throwIfCanceled();
                int count=input.read(buffer,0,Math.min(buffer.length,8193-data.size()));
                if(count<=0)break;
                data.write(buffer,0,count);
            }
            byte[] bytes=data.toByteArray();
            return new String(bytes,0,Math.min(8192,bytes.length),StandardCharsets.UTF_8)+(bytes.length>8192?"\n…":"");
        }
    }

    void clear(){
        if(request!=null){request.cancel();request=null;}
        image.setImageDrawable(null);image.setVisibility(GONE);
        if(bitmap!=null){bitmap.recycle();bitmap=null;}
        title.setText("Preview Pane");description.setText("Select a picture or text file to preview it.");
        text.setVisibility(VISIBLE);text.scrollTo(0,0);open.setVisibility(GONE);open.setOnClickListener(null);
    }

    void close(){clear();worker.shutdownNow();}
}
