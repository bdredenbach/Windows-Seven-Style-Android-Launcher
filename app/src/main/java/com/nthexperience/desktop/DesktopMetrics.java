package com.nthexperience.desktop;

/** Pixel geometry shared by the desktop grid and the settings preview. */
final class DesktopMetrics {
    static int cell(int available,int requested,int automatic,int minimum){
        if(requested<=0)return Math.max(1,Math.min(Math.max(1,available),Math.max(minimum,automatic)));
        int count=Math.max(1,Math.min(requested,Math.max(1,available/Math.max(1,minimum))));
        return Math.max(1,available/count);
    }
    static int fittedRow(int reference,int visible,int normal){
        normal=Math.max(1,normal);if(visible>=reference)return normal;
        int rows=Math.max(1,reference/normal);
        return Math.max(1,Math.min(normal,visible/rows));
    }
    static int rowTop(int row,int cellHeight,int available){int rows=Math.max(1,available/Math.max(1,cellHeight));return row/rows*Math.max(1,available)+row%rows*cellHeight;}
    static float fittedY(float y,int cellHeight,int available,int normal){return available<normal&&y+cellHeight<=normal?Math.min(y,Math.max(0,(available-cellHeight)/Math.max(1,cellHeight))*cellHeight):y;}
    static int icon(int requested,int width,int height,int horizontalPadding,int verticalPadding,int labelHeight){
        return Math.max(1,Math.min(requested,Math.min(Math.max(1,width-horizontalPadding),Math.max(1,height-verticalPadding-labelHeight))));
    }
}
