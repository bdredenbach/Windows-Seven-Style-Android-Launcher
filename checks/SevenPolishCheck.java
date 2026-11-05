package com.nthexperience.desktop;
import java.util.*;
public final class SevenPolishCheck {
    public static void main(String[] args){
        List<String> available=Arrays.asList("notes","files","calendar","new");
        check(TaskbarOrder.apply(available,Arrays.asList("calendar","missing","notes","calendar")).equals(Arrays.asList("calendar","notes","files","new")),"Saved order ignores missing and duplicate slots; new tasks remain");
        check(TaskbarOrder.move(available,"notes",99).equals(Arrays.asList("files","calendar","new","notes")),"Drop clamps at the end");
        check(TaskbarOrder.move(available,"new",-2).get(0).equals("new"),"Drop clamps at the beginning");
        check(TaskbarOrder.move(available,"gone",0).equals(available),"A stale drag cannot create a task");
        check(!TaskbarOrder.valid("a\nb")&&!TaskbarOrder.valid("")&&!TaskbarOrder.valid(null),"Reject invalid saved IDs");
        check(!TaskbarOrder.startsDrag(30,2,90,false),"A quick horizontal swipe scrolls");
        check(TaskbarOrder.startsDrag(30,2,200,false),"A brief press plus horizontal drag reorders");
        check(TaskbarOrder.startsDrag(-20,1,30,true),"Mouse dragging has no touch delay");
        check(!TaskbarOrder.startsDrag(2,0,700,false),"A still hold keeps previews");
        check(!TaskbarOrder.startsDrag(4,-35,200,false)&&TaskbarFlick.opensJumpList(4,-35,200),"Upward flick remains a Jump List gesture");
        check(TaskbarColors.dominant(new int[]{0,0x00ff0000,0xffeeeeee,0xff161616})==0xff79c9f5,"Transparent and neutral padding use a legible fallback");
        int blue=TaskbarColors.dominant(new int[]{0xff2068de,0xff2878ed,0xfffafafa,0xff202020});
        check((blue&255)>((blue>>16)&255)*2,"Dominant icon blue stays blue");
        Set<Integer> hashes=new HashSet<>();
        for(String scheme:SoundSchemes.IDS)for(String event:SoundSchemes.EVENTS){
            short[] sound=SoundSchemes.samples(scheme,event);check(sound.length>2000&&sound.length<22050,"Sounds remain short");
            long energy=0;for(short sample:sound){check(Math.abs((int)sample)<=30000,"No PCM clipping");energy+=Math.abs((int)sample);}
            check(energy>100000,"Each event is audible");check(hashes.add(Arrays.hashCode(sound)),"Each scheme/event has its own cue");
            check(sound[0]==0,"Each cue begins without a click");
        }
        check(!SoundSchemes.valid("../other")&&SoundSchemes.index("unknown")==0,"Unknown scheme cannot select an external audio file");
        System.out.println("PASS: stable taskbar order, separate scroll/drag/flick/hold gestures, icon colors, and twelve bounded original sound cues");
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
