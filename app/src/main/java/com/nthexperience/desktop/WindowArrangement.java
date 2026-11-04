package com.nthexperience.desktop;

/** Window rectangles in the actual work area, independent of Android/density. */
final class WindowArrangement {
    static final int MIN_SIDE_WIDTH_DP=240, MIN_STACK_HEIGHT_DP=190;
    static int[][] rectangles(String mode,int count,int width,int height,int minWidth,int minHeight,int step){
        int n=Math.max(0,count),w=Math.max(1,width),h=Math.max(1,height);
        int[][] result=new int[n][4];
        boolean horizontal=mode.equals("side"),vertical=mode.equals("stacked");
        // Keep title controls/content usable. Crowded/narrow screens cascade instead.
        if(horizontal&&w/nOrOne(n)<minWidth||vertical&&h/nOrOne(n)<minHeight){horizontal=false;vertical=false;}
        int offset=Math.max(1,step),slots=Math.max(1,Math.min(6,Math.min(Math.max(0,(w-minWidth)/offset),Math.max(0,(h-minHeight)/offset))+1));
        int cw=Math.min(w,Math.max(minWidth,Math.round(w*.84f))),ch=Math.min(h,Math.max(minHeight,Math.round(h*.80f)));
        for(int i=0;i<n;i++){
            if(horizontal){int x=i*w/n,next=(i+1)*w/n;result[i]=new int[]{x,0,next-x,h};}
            else if(vertical){int y=i*h/n,next=(i+1)*h/n;result[i]=new int[]{0,y,w,next-y};}
            else{int k=i%slots;result[i]=new int[]{Math.min(k*offset,w-cw),Math.min(k*offset,h-ch),cw,ch};}
        }
        return result;
    }
    private static int nOrOne(int n){return Math.max(1,n);}
}
