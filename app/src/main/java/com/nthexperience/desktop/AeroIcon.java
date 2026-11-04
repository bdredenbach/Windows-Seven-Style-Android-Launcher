package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** A small original icon family drawn with Android primitives. */
final class AeroIcon extends View {
    private final String kind;
    private final android.graphics.drawable.Drawable orb;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    AeroIcon(Context c, String kind) { super(c); this.kind = kind;
        orb = kind.equals("orb") ? c.getDrawable(R.drawable.ic_launcher) : null;
        if (orb != null) orb.setBounds(0, 0, 64, 64);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth()-getPaddingLeft()-getPaddingRight(),getHeight()-getPaddingTop()-getPaddingBottom())/64f;
        canvas.save(); canvas.translate((getWidth()-64*s)/2,(getHeight()-64*s)/2); canvas.scale(s,s);
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setAlpha(255);
        if (orb != null) {
            orb.draw(canvas);
            canvas.restore();
            return;
        }
        if (kind.equals("mediaplayer") || kind.equals("mediacenter")) {
            mediaBadge(canvas, kind.equals("mediacenter"));
            canvas.restore();
            return;
        }
        if(VistaArtwork.draw(canvas,kind)){canvas.restore();return;}
        if (kind.equals("computer")) {
            rounded(canvas,7,9,57,45,4,0xffc5d9e2,0xff7297ab);
            rounded(canvas,11,13,53,39,1,0xff4bb7ce,0xff133d6b);
            p.setColor(0xffbfd1dc); canvas.drawRect(27,45,37,53,p); rounded(canvas,18,52,46,56,2,0xffe3eaf0,0xff88a5b7);
            p.setColor(0x559ef5f0); canvas.drawOval(13,15,48,28,p);
        } else if(kind.endsWith("file")) {
            Path paper=new Path();paper.moveTo(13,5);paper.lineTo(39,5);paper.lineTo(52,18);paper.lineTo(52,59);paper.lineTo(13,59);paper.close();p.setShader(new LinearGradient(0,5,0,59,0xfffefefe,0xffd7e6ef,Shader.TileMode.CLAMP));canvas.drawPath(paper,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0xff7794a8);canvas.drawPath(paper,p);p.setStyle(Paint.Style.FILL);Path fold=new Path();fold.moveTo(39,5);fold.lineTo(39,18);fold.lineTo(52,18);fold.close();p.setColor(0xffb9cedc);canvas.drawPath(fold,p);
            if(kind.equals("picturefile")){rounded(canvas,18,24,47,49,1,0xff8bd8ec,0xffd5eff4);p.setColor(0xfff3cb66);canvas.drawCircle(39,31,4,p);Path hill=new Path();hill.moveTo(18,49);hill.lineTo(29,34);hill.lineTo(35,42);hill.lineTo(42,36);hill.lineTo(47,49);hill.close();p.setColor(0xff5c9464);canvas.drawPath(hill,p);}
            else if(kind.equals("musicfile")){p.setColor(0xff3e83bb);canvas.drawRect(32,25,36,44,p);canvas.drawRect(34,25,44,29,p);canvas.drawOval(23,40,36,49,p);}
            else if(kind.equals("videofile")){rounded(canvas,18,25,47,49,1,0xff5e7486,0xff263d50);p.setColor(0xffe9f5ff);Path play=new Path();play.moveTo(29,30);play.lineTo(39,37);play.lineTo(29,44);play.close();canvas.drawPath(play,p);}
            else{p.setColor(0xff6a91ad);p.setStrokeWidth(2);for(int y=26;y<51;y+=6)canvas.drawLine(20,y,44,y,p);}
        } else if(kind.equals("files")||kind.equals("folderapps")||kind.equals("pictures")||kind.equals("documents")||kind.equals("music")||kind.equals("videos")||kind.equals("downloads")) {
            rounded(canvas,5,14,33,26,3,0xffffe3a0,0xffd59432);
            rounded(canvas,5,21,58,53,4,0xffffde79,0xffd9a13a);
            if(!kind.equals("files")){rounded(canvas,14,10,49,47,2,0xfffdfefe,0xffb5c9d8);if(kind.equals("pictures")){rounded(canvas,18,14,45,38,1,0xff77d4f0,0xffb4e8f1);p.setColor(0xffffdf71);canvas.drawCircle(38,21,4,p);Path mountain=new Path();mountain.moveTo(18,37);mountain.lineTo(28,24);mountain.lineTo(35,33);mountain.lineTo(40,28);mountain.lineTo(45,38);mountain.close();p.setColor(0xff438261);canvas.drawPath(mountain,p);}else if(kind.equals("documents")){p.setStrokeWidth(2);p.setColor(0xff719bb5);for(int y=18;y<39;y+=5)canvas.drawLine(20,y,43,y,p);}else if(kind.equals("music")){p.setColor(0xff3b83ba);canvas.drawRect(31,18,35,36,p);canvas.drawRect(33,18,43,22,p);canvas.drawOval(23,32,35,40,p);}else if(kind.equals("videos")){rounded(canvas,18,15,45,39,1,0xff546574,0xff1c2c3a);p.setColor(0xfff6fbff);Path play=new Path();play.moveTo(28,21);play.lineTo(38,27);play.lineTo(28,33);play.close();canvas.drawPath(play,p);}else if(kind.equals("downloads")){p.setColor(0xff3f9a53);Path arrow=new Path();arrow.moveTo(27,16);arrow.lineTo(36,16);arrow.lineTo(36,28);arrow.lineTo(43,28);arrow.lineTo(32,40);arrow.lineTo(21,28);arrow.lineTo(27,28);arrow.close();canvas.drawPath(arrow,p);}else{for(int i=0;i<4;i++)rounded(canvas,19+(i%2)*13,17+(i/2)*13,29+(i%2)*13,27+(i/2)*13,2,i==0?0xff9edbf6:0xffa4d9a4,i==0?0xff2270a5:0xff43814c);}}

            Path front=new Path();front.moveTo(9,29);front.lineTo(59,25);front.lineTo(55,53);front.lineTo(6,53);front.close();p.setShader(new LinearGradient(0,27,0,54,0xffffedb1,0xffe4ac42,Shader.TileMode.CLAMP));canvas.drawPath(front,p);p.setShader(null);p.setColor(0xaafff9df);p.setStrokeWidth(1);canvas.drawLine(10,30,57,27,p);
        } else if(kind.equals("browser")) {
            p.setShader(new LinearGradient(0,7,0,57,0xff85e0ea,0xff1671b0,Shader.TileMode.CLAMP)); canvas.drawCircle(32,31,24,p); p.setShader(null);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0xbbe4ffff); canvas.drawOval(20,7,44,55,p); canvas.drawOval(8,22,56,40,p); canvas.drawLine(9,31,55,31,p); p.setStyle(Paint.Style.FILL);
            p.setColor(0xffe8bc54); canvas.save(); canvas.rotate(-26,32,31); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4); canvas.drawOval(1,22,63,43,p); p.setStyle(Paint.Style.FILL); canvas.restore();
        } else if(kind.equals("notes")) {
            canvas.save(); canvas.rotate(-7,32,32); rounded(canvas,13,7,53,57,3,0xffffffff,0xffd4e5ec);
            p.setColor(0xff4c8eaa); p.setStrokeWidth(2);
            for(int y=21;y<50;y+=7) canvas.drawLine(20,y,45,y,p);
            p.setColor(0xff86bfcf); canvas.drawRect(13,8,53,16,p); canvas.restore();
        } else if(kind.equals("appearance")) {
            rounded(canvas,8,9,55,48,4,0xffd4e3ec,0xff7f9daf);
            rounded(canvas,12,13,51,42,1,0xff71cfbf,0xff2b668c);
            p.setColor(0xfff0c96e); canvas.drawCircle(45,43,13,p);
            p.setColor(0xffffe1a2); canvas.drawCircle(42,39,7,p);
        } else if(kind.equals("control")) {
            rounded(canvas,5,10,59,51,4,0xffc8e2ef,0xff527c99);rounded(canvas,9,15,55,45,1,0xfff8fdff,0xffdceaf3);
            for(int i=0;i<6;i++)rounded(canvas,14+(i%3)*13,20+(i/3)*12,23+(i%3)*13,28+(i/3)*12,2,i%2==0?0xff85cae3:0xffa2cd80,i%2==0?0xff2789b5:0xff5b9a46);
            p.setColor(0xffc7dce8);canvas.drawRect(27,51,37,56,p);rounded(canvas,19,55,45,59,2,0xffe7f1f7,0xff7599b1);
        } else if(kind.equals("drive")) {
            Path top=new Path();top.moveTo(12,13);top.lineTo(51,13);top.lineTo(59,38);top.lineTo(5,38);top.close();p.setShader(new LinearGradient(0,12,0,40,0xfff2f6f9,0xff9eafbc,Shader.TileMode.CLAMP));canvas.drawPath(top,p);p.setShader(null);rounded(canvas,5,36,59,52,3,0xff9babb7,0xff455866);rounded(canvas,10,40,54,48,2,0xff253843,0xff607784);p.setColor(0xff6fe187);canvas.drawCircle(49,44,2,p);p.setColor(0xffdfe7ec);canvas.drawLine(12,43,37,43,p);
        } else if(kind.equals("security")) {
            Path shield=new Path();shield.moveTo(32,6);shield.cubicTo(41,11,48,13,55,13);shield.lineTo(52,36);shield.cubicTo(50,46,40,54,32,59);shield.cubicTo(21,53,13,45,11,35);shield.lineTo(8,13);shield.cubicTo(17,13,25,10,32,6);shield.close();int save=canvas.save();canvas.clipPath(shield);rounded(canvas,8,5,32,33,0,0xffb3e4ff,0xff3784c6);rounded(canvas,32,5,56,33,0,0xffffdf89,0xffdf8e2e);rounded(canvas,8,33,32,60,0,0xffedbf56,0xffb5731d);rounded(canvas,32,33,56,60,0,0xff5594d3,0xff195a93);canvas.restoreToCount(save);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xffd6dfe6);canvas.drawPath(shield,p);p.setStyle(Paint.Style.FILL);
        } else if(kind.equals("network")) {
            p.setShader(new RadialGradient(24,18,27,new int[]{0xffbceff2,0xff2689be,0xff164d83},null,Shader.TileMode.CLAMP));canvas.drawCircle(28,28,23,p);p.setShader(null);p.setColor(0xff76bd7a);Path land=new Path();land.moveTo(13,14);land.lineTo(30,8);land.lineTo(39,20);land.lineTo(30,27);land.lineTo(28,42);land.lineTo(19,35);land.lineTo(21,25);land.lineTo(12,23);land.close();canvas.drawPath(land,p);rounded(canvas,30,33,59,54,2,0xffd9e6ec,0xff7998ad);rounded(canvas,33,36,56,49,0,0xff6dc8e2,0xff27638e);p.setColor(0xffb5cad8);canvas.drawRect(42,54,47,59,p);canvas.drawRect(35,58,54,61,p);
        } else if(kind.equals("accounts")) {
            p.setColor(0xffcb9e66);canvas.drawCircle(43,21,9,p);rounded(canvas,31,31,59,54,8,0xff9acbe7,0xff3d719d);p.setShader(new RadialGradient(23,18,12,new int[]{0xffffe7b8,0xffbb8959},null,Shader.TileMode.CLAMP));canvas.drawCircle(23,22,12,p);p.setShader(null);rounded(canvas,5,36,40,60,10,0xffb6df8c,0xff4a873e);p.setColor(0x55ffffff);canvas.drawOval(10,37,34,44,p);
        } else if(kind.equals("programs")) {
            rounded(canvas,8,9,44,54,3,0xffecf6fc,0xff6597ba);rounded(canvas,12,14,40,35,1,0xff63bae2,0xff216995);p.setColor(0xffc9d7df);canvas.drawCircle(44,43,17,p);p.setShader(new LinearGradient(30,30,59,57,new int[]{0xfff7f7e8,0xffadcad6,0xffd8c0e4,0xfff3f5f5},null,Shader.TileMode.CLAMP));canvas.drawCircle(44,43,15,p);p.setShader(null);p.setColor(0xff648597);canvas.drawCircle(44,43,4,p);p.setColor(0xffe8f3fa);canvas.drawCircle(44,43,2,p);
        } else if(kind.equals("copy")) {
            rounded(canvas,8,6,43,49,2,0xfff5fbfe,0xff80a4bd);rounded(canvas,22,18,57,60,2,0xfffefefe,0xffbfd5e4);p.setColor(0xff648da7);p.setStrokeWidth(2);for(int y=30;y<52;y+=7)canvas.drawLine(29,y,49,y,p);
        } else if(kind.equals("paste")) {
            rounded(canvas,10,9,53,59,3,0xffd3b67e,0xff9c753b);rounded(canvas,20,5,43,16,3,0xffdfe8ed,0xff8599a7);rounded(canvas,22,22,59,60,2,0xfffffefa,0xffdae5e8);p.setColor(0xff7394a5);p.setStrokeWidth(2);for(int y=32;y<53;y+=7)canvas.drawLine(29,y,51,y,p);
        } else if(kind.equals("cut")) {
            p.setColor(0xffadbcc6);p.setStrokeWidth(5);canvas.drawLine(17,45,48,7,p);canvas.drawLine(44,45,17,7,p);p.setColor(0xff315f8b);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);canvas.drawOval(6,38,27,59,p);canvas.drawOval(35,38,56,59,p);p.setStyle(Paint.Style.FILL);p.setColor(0xffe6edf1);canvas.drawCircle(31,29,3,p);
        } else if(kind.equals("delete")) {
            p.setColor(0xffb63d2c);p.setStrokeWidth(11);p.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(15,15,49,49,p);canvas.drawLine(49,15,15,49,p);p.setColor(0xffed9e88);p.setStrokeWidth(3);canvas.drawLine(15,13,49,47,p);p.setStrokeCap(Paint.Cap.BUTT);
        } else if(kind.equals("calendar")) {
            rounded(canvas,8,9,56,58,4,0xfffefdf7,0xffd8e0e4);rounded(canvas,8,9,56,25,4,0xff8ac5e8,0xff306a9c);p.setColor(0xffd6e7f1);canvas.drawRect(8,21,56,25,p);p.setColor(0xff244b68);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(27);canvas.drawText("31",16,50,p);p.setColor(0xff6d7e89);canvas.drawRoundRect(17,5,22,16,2,2,p);canvas.drawRoundRect(42,5,47,16,2,2,p);
        } else if(kind.equals("sticky")) {
            rounded(canvas,8,7,56,58,2,0xffffef9e,0xffead062);p.setColor(0xffffe483);canvas.drawRect(8,7,56,18,p);p.setColor(0xff9a8337);p.setStrokeWidth(2);for(int y=27;y<45;y+=7)canvas.drawLine(17,y,46,y,p);Path fold=new Path();fold.moveTo(43,58);fold.lineTo(56,45);fold.lineTo(43,45);fold.close();p.setColor(0xffd1b34a);canvas.drawPath(fold,p);
        } else if(kind.equals("weather")) {
            p.setColor(0xffffcf55);canvas.drawCircle(24,24,14,p);p.setStrokeWidth(3);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(24+(float)Math.cos(a)*18,24+(float)Math.sin(a)*18,24+(float)Math.cos(a)*23,24+(float)Math.sin(a)*23,p);}rounded(canvas,13,35,59,54,9,0xfff5fcff,0xff81afca);p.setShader(new LinearGradient(0,26,0,51,0xfff7fdff,0xffaacede,Shader.TileMode.CLAMP));canvas.drawCircle(32,35,12,p);canvas.drawCircle(47,39,11,p);p.setShader(null);
        } else if(kind.equals("gadgets")) {
            int[] colors={0xff498fba,0xffefb44d,0xff80ad61,0xff9075ad};for(int i=0;i<4;i++){int x=6+(i%2)*28,y=6+(i/2)*28;rounded(canvas,x,y,x+24,y+24,4,0xffedf7ff,colors[i]);p.setColor(colors[i]);canvas.drawCircle(x+12,y+11,6,p);p.setColor(0xfff7fbfd);canvas.drawRect(x+5,y+19,x+19,y+21,p);}
        } else if(kind.equals("feeds")) {
            rounded(canvas,7,7,57,57,8,0xffffc56b,0xffd86616);p.setColor(Color.WHITE);canvas.drawCircle(21,43,4,p);p.setStrokeWidth(5);p.setStyle(Paint.Style.STROKE);canvas.drawArc(17,28,36,47,-90,90,false,p);canvas.drawArc(17,15,49,47,-90,90,false,p);p.setStyle(Paint.Style.FILL);
        } else if(kind.equals("checklist")) {
            rounded(canvas,10,6,54,59,3,0xfffdfefe,0xffbed4df);p.setStrokeWidth(3);for(int y=18;y<50;y+=13){p.setColor(0xff4c955e);canvas.drawLine(16,y,20,y+4,p);canvas.drawLine(20,y+4,26,y-4,p);p.setColor(0xff7790a4);canvas.drawLine(31,y,46,y,p);}
        } else if(kind.equals("timer")) {
            rounded(canvas,15,7,49,13,2,0xffcfe7f4,0xff527b9b);rounded(canvas,15,51,49,57,2,0xffcfe7f4,0xff527b9b);Path glass=new Path();glass.moveTo(19,13);glass.lineTo(45,13);glass.lineTo(43,22);glass.lineTo(34,32);glass.lineTo(43,42);glass.lineTo(45,51);glass.lineTo(19,51);glass.lineTo(21,42);glass.lineTo(30,32);glass.lineTo(21,22);glass.close();p.setColor(0xffb5d7e8);canvas.drawPath(glass,p);p.setColor(0xffe7b957);Path sand=new Path();sand.moveTo(23,18);sand.lineTo(41,18);sand.lineTo(32,29);sand.close();canvas.drawPath(sand,p);canvas.drawOval(23,43,41,49,p);
        } else if(kind.equals("stopwatch")) {
            rounded(canvas,25,4,39,13,2,0xffe3edf4,0xff6a8fa9);rounded(canvas,10,12,18,20,2,0xffb9d8ea,0xff4a7899);p.setShader(new LinearGradient(0,14,0,60,0xffdcecf5,0xff3d759d,Shader.TileMode.CLAMP));canvas.drawCircle(32,36,23,p);p.setShader(null);p.setColor(0xfff7fbff);canvas.drawCircle(32,36,18,p);p.setColor(0xff367aab);p.setStrokeWidth(3);canvas.drawLine(32,36,32,21,p);canvas.drawLine(32,36,43,42,p);
        } else if(kind.equals("calculator")) {
            rounded(canvas,11,5,53,60,5,0xffb4c8d7,0xff354c63);rounded(canvas,16,10,48,24,1,0xffcadbce,0xffa0b5aa);for(int i=0;i<12;i++)rounded(canvas,16+(i%3)*11,29+(i/3)*7,24+(i%3)*11,34+(i/3)*7,1,0xfff2f7fb,i%3==2?0xff78a5c4:0xffb5c9d7);
        } else if(kind.equals("converter")) {
            rounded(canvas,7,11,57,53,4,0xffedf8ff,0xff8caec7);p.setColor(0xff418dbc);Path arrow=new Path();arrow.moveTo(14,23);arrow.lineTo(40,23);arrow.lineTo(40,17);arrow.lineTo(51,28);arrow.lineTo(40,39);arrow.lineTo(40,32);arrow.lineTo(14,32);arrow.close();canvas.drawPath(arrow,p);p.setColor(0xff70a561);canvas.drawRect(14,40,43,46,p);
        } else if(kind.equals("compass")) {
            p.setShader(new LinearGradient(0,5,0,59,0xfff3d799,0xff816033,Shader.TileMode.CLAMP));canvas.drawCircle(32,33,27,p);p.setShader(null);p.setColor(0xfffcf4d5);canvas.drawCircle(32,33,22,p);Path needle=new Path();needle.moveTo(32,12);needle.lineTo(24,34);needle.lineTo(40,34);needle.close();p.setColor(0xffb14434);canvas.drawPath(needle,p);needle.reset();needle.moveTo(32,54);needle.lineTo(24,34);needle.lineTo(40,34);needle.close();p.setColor(0xff54798d);canvas.drawPath(needle,p);p.setColor(0xffd5b06d);canvas.drawCircle(32,33,3,p);
        } else if(kind.equals("puzzle")) {
            rounded(canvas,5,5,59,59,5,0xffad8752,0xff64472c);int[] colors={0xffbf8d49,0xff6b9b6b,0xff6196b1,0xff9c7eac};for(int i=0;i<8;i++){int x=9+i%3*16,y=9+i/3*16;rounded(canvas,x,y,x+14,y+14,2,0xfff5e9c2,colors[i%4]);p.setColor(0xfff9f7e7);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(10);canvas.drawText(Integer.toString(i+1),x+4,y+11,p);}
        } else if(kind.equals("worldclock")) {
            p.setShader(new RadialGradient(22,20,39,new int[]{0xffaee6ef,0xff317bab,0xff183958},null,Shader.TileMode.CLAMP));canvas.drawCircle(29,29,25,p);p.setShader(null);p.setColor(0xffb1e0e6);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);canvas.drawOval(17,5,41,53,p);canvas.drawOval(5,17,53,41,p);p.setStyle(Paint.Style.FILL);p.setColor(0xffdde8ec);canvas.drawCircle(46,46,15,p);p.setColor(0xfffdfbf1);canvas.drawCircle(46,46,12,p);p.setColor(0xff315468);p.setStrokeWidth(2);canvas.drawLine(46,46,46,37,p);canvas.drawLine(46,46,53,49,p);
        } else if(kind.equals("clock")) {
            p.setShader(new LinearGradient(0,6,0,59,0xffeff7fc,0xff6b879b,Shader.TileMode.CLAMP));canvas.drawCircle(32,32,27,p);p.setShader(null);p.setColor(0xfff9fbf5);canvas.drawCircle(32,32,22,p);p.setColor(0xff385565);p.setStrokeWidth(2);for(int i=0;i<12;i++){double a=i*Math.PI/6;canvas.drawLine(32+(float)Math.sin(a)*18,32-(float)Math.cos(a)*18,32+(float)Math.sin(a)*20,32-(float)Math.cos(a)*20,p);}p.setStrokeWidth(3);canvas.drawLine(32,32,23,22,p);canvas.drawLine(32,32,44,27,p);p.setColor(0xffa34731);p.setStrokeWidth(1);canvas.drawLine(32,35,32,14,p);
        } else if(kind.equals("accessibility")) {
            p.setShader(new LinearGradient(0,6,0,59,0xff70c5f2,0xff226ab5,Shader.TileMode.CLAMP));canvas.drawCircle(32,32,27,p);p.setShader(null);p.setColor(Color.WHITE);canvas.drawCircle(32,18,5,p);p.setStrokeWidth(4);p.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(19,28,45,28,p);canvas.drawLine(32,26,32,39,p);canvas.drawLine(32,38,24,50,p);canvas.drawLine(32,38,40,50,p);p.setStrokeCap(Paint.Cap.BUTT);
        } else if(kind.equals("sound")||kind.equals("hardware")) {
            rounded(canvas,7,24,23,43,2,0xffe2e9ee,0xff6d8d9f);Path cone=new Path();cone.moveTo(21,24);cone.lineTo(39,12);cone.lineTo(39,55);cone.lineTo(21,43);cone.close();p.setShader(new LinearGradient(20,10,40,55,0xffe8c274,0xff976728,Shader.TileMode.CLAMP));canvas.drawPath(cone,p);p.setShader(null);p.setColor(0xff568eac);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);canvas.drawArc(32,19,55,49,-65,130,false,p);canvas.drawArc(32,10,63,58,-58,116,false,p);p.setStyle(Paint.Style.FILL);
        } else if(kind.equals("color")) {
            int[] colors={0xff76bee8,0xffdb879e,0xff85b96c,0xffe6c36d};for(int i=0;i<4;i++)rounded(canvas,7+(i%2)*26,7+(i/2)*26,31+(i%2)*26,31+(i/2)*26,4,0xfff5fbff,colors[i]);
        } else if(kind.equals("power")) {
            rounded(canvas,10,16,54,53,4,0xffe6eaf0,0xff607c8e);rounded(canvas,15,21,49,48,1,0xffa0df88,0xff4a9b4d);rounded(canvas,25,10,39,17,2,0xffdce7ed,0xff748c99);p.setColor(0xffefffdf);Path bolt=new Path();bolt.moveTo(34,23);bolt.lineTo(24,36);bolt.lineTo(31,36);bolt.lineTo(28,46);bolt.lineTo(41,30);bolt.lineTo(33,30);bolt.close();canvas.drawPath(bolt,p);
        } else {
            for(int i=0;i<4;i++) rounded(canvas,10+(i%2)*25,10+(i/2)*25,29+(i%2)*25,29+(i/2)*25,4,0xffa9ddec,0xff3b8fba);
        }
        canvas.restore();
    }
    private void mediaBadge(Canvas canvas, boolean center) {
        p.setShader(new RadialGradient(22, 18, 47, center
                ? new int[]{0xff8ff8ff, 0xff2783d4, 0xff102d60}
                : new int[]{0xffffe6a9, 0xffed8b31, 0xffa43622}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(32, 32, 26, p);
        p.setShader(null);
        p.setColor(0xc4f5fcff);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        canvas.drawCircle(32, 32, 24, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xfff5fbff);
        if (center) {
            for (int i = 0; i < 4; i++) {
                float x = 19 + i % 2 * 14, y = 19 + i / 2 * 14;
                canvas.drawRoundRect(x, y, x + 11, y + 11, 2, 2, p);
            }
        } else {
            Path play = new Path();
            play.moveTo(26, 20);
            play.lineTo(45, 32);
            play.lineTo(26, 44);
            play.close();
            canvas.drawPath(play, p);
        }
    }

    private void rounded(Canvas c,float l,float t,float r,float b,int radius,int top,int bottom) {
        p.setAlpha(255); p.setShader(new LinearGradient(0,t,0,b,top,bottom,Shader.TileMode.CLAMP)); c.drawRoundRect(l,t,r,b,radius,radius,p); p.setShader(null);
    }
}
