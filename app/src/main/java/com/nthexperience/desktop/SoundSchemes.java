package com.nthexperience.desktop;

/** Small original sound sets synthesized offline; no downloaded audio is required. */
final class SoundSchemes {
    static final int RATE=22050;
    static final String DEFAULT="seven";
    static final String[] IDS={"seven","meadow","twilight","classic"};
    static final String[] NAMES={"Seven Chimes","Meadow","Twilight","Classic cues"};
    static final String[] EVENTS={"complete","error","notice"};
    static boolean valid(String id){for(String value:IDS)if(value.equals(id))return true;return false;}
    static int index(String id){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return i;return 0;}
    static short[] samples(String scheme,String event){
        if(!valid(scheme))scheme=DEFAULT;
        boolean error=event.equals("error"),notice=event.equals("notice");
        if(scheme.equals("classic")){
            short[] pcm=new short[RATE/6];
            double a=error?330:notice?660:523.25,b=error?277.18:notice?880:783.99;
            for(int i=0;i<pcm.length;i++){
                double t=i/(double)RATE,envelope=Math.sin(Math.PI*i/pcm.length);
                pcm[i]=(short)(5500*envelope*(Math.sin(2*Math.PI*a*t)+.45*Math.sin(2*Math.PI*b*t)));
            }
            return pcm;
        }
        double[] notes;
        double spacing,decay,partial;
        switch(scheme){
            case "meadow":
                notes=error?new double[]{440,349.23}:notice?new double[]{659.25,880}:new double[]{523.25,659.25,880};
                spacing=.085;decay=11;partial=.24;break;
            case "twilight":
                notes=error?new double[]{293.66,220}:notice?new double[]{440,659.25}:new double[]{329.63,440,659.25};
                spacing=.11;decay=8;partial=.12;break;
            default:
                notes=error?new double[]{392,311.13}:notice?new double[]{783.99,1046.5}:new double[]{523.25,659.25,783.99};
                spacing=.095;decay=9;partial=.18;
        }
        double duration=spacing*(notes.length-1)+.29;
        short[] pcm=new short[(int)(RATE*duration)];
        for(int i=0;i<pcm.length;i++){
            double t=i/(double)RATE,value=0;
            for(int n=0;n<notes.length;n++){
                double age=t-n*spacing;if(age<0)continue;
                double envelope=(1-Math.exp(-age*110))*Math.exp(-age*decay);
                value+=envelope*(Math.sin(2*Math.PI*notes[n]*age)+partial*Math.sin(2*Math.PI*notes[n]*2*age));
            }
            double tail=Math.min(1,(duration-t)/.025);
            pcm[i]=(short)Math.max(-30000,Math.min(30000,Math.round(value*6500*tail)));
        }
        return pcm;
    }
    private SoundSchemes(){}
}
