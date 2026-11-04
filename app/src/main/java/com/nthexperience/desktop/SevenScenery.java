package com.nthexperience.desktop;

import android.graphics.*;

/** Original wallpaper illustrations drawn in a normalized viewport. */
final class SevenScenery {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();

    void draw(Canvas canvas,float width,float height,int id){
        canvas.save();canvas.scale(width/1000f,height/700f);
        switch(id){
            case 4: harmony(canvas);break;
            case 5: landscape(canvas,0);break;
            case 6: landscape(canvas,1);break;
            case 7: landscape(canvas,2);break;
            case 8: pavilion(canvas);break;
            case 9: circuit(canvas);break;
            case 10: paper(canvas);break;
        }
        canvas.restore();
    }
    private void gradient(Canvas c,int top,int bottom){
        paint.setShader(new LinearGradient(120,0,850,700,top,bottom,Shader.TileMode.CLAMP));
        c.drawRect(0,0,1000,700,paint);paint.setShader(null);
    }
    private void glow(Canvas c,float x,float y,float radius,int color){
        paint.setShader(new RadialGradient(x,y,radius,color,color&0xffffff,Shader.TileMode.CLAMP));
        c.drawRect(0,0,1000,700,paint);paint.setShader(null);
    }
    private void polygon(Canvas c,int color,float... xy){
        paint.setColor(color);path.rewind();path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);
        path.close();c.drawPath(path,paint);
    }
    private void ribbon(Canvas c,int color,float y,float width){
        paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);
        path.rewind();path.moveTo(-20,y+100);path.cubicTo(220,y-330,510,y+220,1030,y-260);
        c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);
    }
    private void harmony(Canvas c){
        gradient(c,0xff051e48,0xff0d80b5);glow(c,735,310,520,0xc76df9ff);
        for(int i=0;i<10;i++)ribbon(c,i%3==0?0x70d8fcff:0x2b81e6ff,455+i*11,1+i*.9f);
        glow(c,735,311,45,0xabffffff);
        paint.setColor(0x55defdff);
        for(int i=0;i<28;i++)c.drawCircle(435+(i*173)%535,60+(i*89)%470,1+i%3,paint);
    }
    private void landscape(Canvas c,int mode){
        int[][] palette={{0xff213964,0xffd6e6f0,0xff4e617e,0xff203b58,0xff86acc2},
                {0xff25365b,0xffe5a39d,0xff554b73,0xff222844,0xff606c9d},
                {0xff44344f,0xffffc490,0xff95616e,0xff4f374d,0xffbe836e}};
        int[] p=palette[mode];gradient(c,p[0],p[1]);glow(c,641,288,230,0x9affddae);
        paint.setColor(0xffffedcc);c.drawCircle(641,301,26,paint);
        polygon(c,p[2],0,402,0,241,96,299,198,156,279,244,348,198,482,332,582,292,686,328,793,226,920,292,1000,186,1000,432);
        if(mode==0){
            polygon(c,0xffd8e6f0,148,226,198,156,260,227,206,199,191,209,179,198);
            polygon(c,0xffc1d7e5,294,246,348,198,398,248,354,231,331,239);
            polygon(c,0xffbed5e4,752,271,793,226,841,252,795,246,781,260);
        }
        polygon(c,p[3],0,427,0,309,90,365,163,342,248,402,367,367,480,419,605,391,714,410,842,343,1000,318,1000,464);
        paint.setShader(new LinearGradient(0,425,0,700,p[4],p[3],Shader.TileMode.CLAMP));c.drawRect(0,425,1000,700,paint);paint.setShader(null);
        if(mode==2){
            polygon(c,0xff5c3945,0,510,192,455,333,528,255,584,545,646,566,700,0,700);
            polygon(c,0xffaa6657,1000,422,1000,700,515,700,648,640,579,593,736,539,704,503,853,485);
        }else{
            for(int i=0;i<46;i++){
                float y=431+(i*37)%259,x=(i*103)%980;
                paint.setColor(i%3==0?0x65fff1da:0x4059c3dc);paint.setStrokeWidth(1+i%3);
                c.drawLine(x,y,Math.min(1000,x+29+(i*47)%165),y,paint);
            }
            polygon(c,p[3],0,602,155,547,228,589,337,638,275,700,0,700);
            polygon(c,p[3],1000,494,860,516,780,575,799,626,676,700,1000,700);
        }
    }
    private void pavilion(Canvas c){
        gradient(c,0xff0f4f90,0xffb8e9f5);glow(c,725,110,470,0x9ff0fbff);
        polygon(c,0xff235779,0,700,0,408,351,202,1000,309,1000,700);
        polygon(c,0xff83bdd7,0,408,351,202,1000,309,673,439,263,459);
        polygon(c,0xffe5f3f4,132,700,263,459,673,439,1000,309,1000,543,706,700);
        polygon(c,0xff3c7caa,702,700,1000,531,1000,700);
        for(int i=0;i<13;i++){
            paint.setColor(i%3==0?0x55e8fbff:0x416b9dbd);paint.setStrokeWidth(3);
            c.drawLine(i*83f,420,109+i*69f,690,paint);
        }
        ribbon(c,0x65ffffff,599,6);
    }
    private void circuit(Canvas c){
        gradient(c,0xff05121d,0xff0d485a);glow(c,704,390,395,0x862de9ff);
        for(int i=0;i<10;i++){
            int x=104+i*87,y=135+(i*103)%455;
            paint.setColor(i%3==0?0x99a8faff:0x447ef6ff);paint.setStrokeWidth(i%3+1);paint.setStyle(Paint.Style.STROKE);
            path.rewind();path.moveTo(-20,y);path.lineTo(x-48,y);path.lineTo(x,y-43);path.lineTo(1020,y-43);
            c.drawPath(path,paint);c.drawCircle(x,y-43,5,paint);paint.setStyle(Paint.Style.FILL);
        }
        for(int i=0;i<3;i++){
            paint.setStyle(Paint.Style.STROKE);paint.setColor(0x9560f3ff);paint.setStrokeWidth(2+i*2);
            c.drawOval(505+i*34,164+i*21,900-i*23,547-i*18,paint);paint.setStyle(Paint.Style.FILL);
        }
    }
    private void paper(Canvas c){
        gradient(c,0xff267bab,0xffa8dfe8);glow(c,682,204,300,0xaafdf3be);
        for(int i=0;i<5;i++){
            float x=40+i*206,y=240+(i*77)%220;
            paint.setColor(0x95ecf8fa);c.drawOval(x-90,y,x+150,y+56,paint);
        }
        polygon(c,0xfff7d181,119,533,662,172,552,406,427,453,326,561);
        polygon(c,0xffe38866,662,172,391,457,326,561,427,453);
        polygon(c,0xffffeed0,662,172,552,406,828,433);
        polygon(c,0xff22667e,0,649,232,577,431,637,635,550,1000,610,1000,700,0,700);
    }
}
