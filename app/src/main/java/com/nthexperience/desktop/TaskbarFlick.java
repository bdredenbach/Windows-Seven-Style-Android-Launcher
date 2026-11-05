package com.nthexperience.desktop;

/** Classifies a taskbar gesture in density-independent pixels. */
final class TaskbarFlick {
    static boolean pointsUp(float dx,float dy){
        return dy<0&&-dy>=Math.abs(dx);
    }

    static boolean opensJumpList(float dx,float dy,long elapsedMillis){
        if(!pointsUp(dx,dy))return false;
        float rise=-dy;
        return rise>=20||rise>=10&&rise*1000>=85*elapsedMillis;
    }
}
