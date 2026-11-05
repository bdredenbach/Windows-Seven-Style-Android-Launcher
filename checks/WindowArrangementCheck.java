package com.nthexperience.desktop;
public final class WindowArrangementCheck {
    public static void main(String[] args){
        for(int width:new int[]{1,180,360,800,1400})for(int height:new int[]{1,160,400,900})for(int n=0;n<=32;n++)for(String mode:new String[]{"cascade","stacked","side"}){
            int[][] r=WindowArrangement.rectangles(mode,n,width,height,240,190,30);check(r.length==n,"Every window gets a rectangle");for(int[] a:r)check(a[0]>=0&&a[1]>=0&&a[2]>0&&a[3]>0&&a[0]+a[2]<=width&&a[1]+a[3]<=height,"Window fits its actual work area");
        }
        int[][] side=WindowArrangement.rectangles("side",3,1001,600,240,190,30);check(side[0][0]+side[0][2]==side[1][0]&&side[2][0]+side[2][2]==1001,"Wide split has no rounding gap");
        int[][] stacked=WindowArrangement.rectangles("stacked",3,800,901,240,190,30);check(stacked[0][1]+stacked[0][3]==stacked[1][1]&&stacked[2][1]+stacked[2][3]==901,"Stacked split has no rounding gap");
        int[][] narrow=WindowArrangement.rectangles("side",4,360,600,240,190,30);check(narrow[0][2]>=240&&narrow[1][0]>0,"A phone cascades when columns would be unusable");
        int minimum=AeroSnap.MIN_SIDE_WIDTH_DP;
        check(!AeroSnap.canSplit(2*minimum-1,620,minimum,190),"A split keeps enough space for the narrow captions");
        check(AeroSnap.canSplit(2*minimum,620,minimum,190),"Minimum supported portrait split fits");
        check(AeroSnap.target(8,290,360,22,650,24,minimum,190)==AeroSnap.Placement.LEFT,"Left edge snaps on a 360 dp phone");
        check(AeroSnap.target(352,290,360,22,650,24,minimum,190)==AeroSnap.Placement.RIGHT,"Right edge snaps on a 360 dp phone");
        check(AeroSnap.target(8,32,360,22,650,24,minimum,190)==AeroSnap.Placement.MAXIMIZED,"Top corner still maximizes");
        check(AeroSnap.target(180,290,360,22,650,24,minimum,190)==AeroSnap.Placement.FLOATING,"Interior release stays floating");
        System.out.println("PASS: window arrangements fit tiny/cutout work areas, preserve usable captions, divide wide layouts exactly and snap in portrait");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
