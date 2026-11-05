package com.nthexperience.desktop;

/** Chooses a useful saturated icon color without letting white/grey icon padding win. */
final class TaskbarColors {
    static final int FALLBACK=0xff79c9f5;
    static int dominant(int[] pixels){
        double[] weights=new double[24],red=new double[24],green=new double[24],blue=new double[24];
        for(int color:pixels){
            int a=color>>>24,r=color>>16&255,g=color>>8&255,b=color&255;
            int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),delta=max-min;
            if(a<96||max<48||delta<24)continue;
            double saturation=delta/(double)max;
            double hue=max==r?(g-b)/(double)delta:max==g?2+(b-r)/(double)delta:4+(r-g)/(double)delta;
            if(hue<0)hue+=6;
            int bucket=Math.min(23,(int)(hue*4));
            double weight=(a/255d)*saturation*saturation*(.25+.75*max/255d);
            weights[bucket]+=weight;red[bucket]+=r*weight;green[bucket]+=g*weight;blue[bucket]+=b*weight;
        }
        int best=0;for(int i=1;i<24;i++)if(weights[i]>weights[best])best=i;
        if(weights[best]<.02)return FALLBACK;
        int r=(int)(red[best]/weights[best]),g=(int)(green[best]/weights[best]),b=(int)(blue[best]/weights[best]);
        int peak=Math.max(r,Math.max(g,b));
        double scale=peak<180?180d/Math.max(1,peak):1;
        return 0xff000000|Math.min(255,(int)(r*scale))<<16|Math.min(255,(int)(g*scale))<<8|Math.min(255,(int)(b*scale));
    }
}
