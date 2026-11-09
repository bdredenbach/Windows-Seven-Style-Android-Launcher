package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import java.util.*;
import org.json.*;

/** Desktop positions are stored per shortcut and orientation, independent of icon order. */
final class DesktopIconGrid extends ViewGroup {
    private final DesktopStore store;
    private String key;
    private int tileWidth,tileHeight,preferredWidth,preferredHeight,viewportHeight,layoutHeight,plannedX,plannedY;
    private String lastPlanKey,lastPositions,lastDesktops;private int cachedBottom,planBuilds;
    int planBuildCount(){return planBuilds;}
    private final List<Rect> cutouts=new ArrayList<>(),positions=new ArrayList<>();
    DesktopIconGrid(Context context,DesktopStore store,String orientation){super(context);this.store=store;key="desktop_positions_"+orientation;setLayerType(LAYER_TYPE_SOFTWARE,null);}
    void orientation(String orientation){key="desktop_positions_"+orientation;requestLayout();}
    private JSONObject saved(){try{return new JSONObject(store.prefs.getString(key,"{}"));}catch(JSONException|ClassCastException ex){return new JSONObject();}}
    void clearPositions(){store.prefs.edit().remove(key).apply();requestLayout();}
    void configure(int width,int height,List<Rect> exclusions){if(preferredWidth==width&&preferredHeight==height&&cutouts.equals(exclusions))return;preferredWidth=width;preferredHeight=height;cutouts.clear();for(Rect r:exclusions)cutouts.add(new Rect(r));requestLayout();}
    int cellWidth(){return Math.max(1,tileWidth);}
    int cellHeight(){return Math.max(1,tileHeight);}
    int viewportHeight(){return viewportHeight;}
    void viewport(int height){viewport(height,height);}
    void viewport(int height,int gridHeight){if(viewportHeight!=height||layoutHeight!=gridHeight){viewportHeight=height;layoutHeight=gridHeight;requestLayout();}}
    private boolean blocked(Rect rect){Rect content=new Rect(rect);content.inset(Aero.dp(getContext(),7),Aero.dp(getContext(),3));content.offset(plannedX,plannedY);for(Rect hole:cutouts)if(Rect.intersects(content,hole))return true;return false;}
    private boolean occupied(Rect rect,List<Rect> used){for(Rect other:used)if(other!=null&&Rect.intersects(rect,other))return true;return false;}
    private Rect at(float x,float y,int width,int height,boolean snap){int w=Math.max(1,tileWidth),h=Math.max(1,tileHeight);if(snap){x=DesktopGeometry.clamp(DesktopGeometry.snap(x,w),0,Math.max(0,(width-w)/w)*w);y=DesktopGeometry.clamp(DesktopGeometry.snap(y,h),0,Math.max(0,(height-h)/h)*h);}int left=Math.round(DesktopGeometry.clamp(x,0,Math.max(0,width-w))),top=Math.round(DesktopGeometry.clamp(y,0,Math.max(0,height-h)));return new Rect(left,top,left+w,top+h);}
    void place(String id,float x,float y){
        if(store.prefs.getBoolean("layout_locked",false))return;
        Rect target=at(x,y,getWidth(),Math.max(viewportHeight,getHeight()),store.prefs.getBoolean("snap_desktop",true));
        if(blocked(target))return;
        JSONObject all=saved(),point=new JSONObject();for(int i=0;i<getChildCount();i++){View child=getChildAt(i);String other=String.valueOf(child.getTag());if(all.has(other))continue;JSONObject previous=new JSONObject();GadgetLayout.put(previous,"x",DesktopGeometry.fraction(child.getLeft(),getWidth()-tileWidth));GadgetLayout.put(previous,"y",child.getTop()/getResources().getDisplayMetrics().density);GadgetLayout.put(all,other,previous);}GadgetLayout.put(point,"x",DesktopGeometry.fraction(target.left,getWidth()-tileWidth));GadgetLayout.put(point,"y",target.top/getResources().getDisplayMetrics().density);GadgetLayout.put(all,id,point);store.prefs.edit().putString(key,all.toString()).apply();requestLayout();
    }
    void swapPositions(String moving,String target){
        View a=null,b=null;for(int i=0;i<getChildCount();i++){View v=getChildAt(i);if(moving.equals(v.getTag()))a=v;if(target.equals(v.getTag()))b=v;}
        if(a==null||b==null)return;float ax=a.getX(),ay=a.getY(),bx=b.getX(),by=b.getY();place(moving,bx,by);place(target,ax,ay);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec);String orientation=key.endsWith("landscape")?"landscape":"portrait";
        tileWidth=DesktopMetrics.cell(width,store.gridColumns(orientation),preferredWidth,Aero.dp(getContext(),48+store.spacingX()));
        tileHeight=DesktopMetrics.cell(Math.max(1,viewportHeight),store.gridRows(orientation),DesktopMetrics.fittedRow(Math.max(1,layoutHeight),Math.max(1,viewportHeight),preferredHeight),Aero.dp(getContext(),store.iconLabels()?Math.max(72,store.labelSize()*getResources().getConfiguration().fontScale*2.6f+29+store.spacingY()):48+store.spacingY()));
        int w=tileWidth,h=tileHeight,count=getChildCount();
        int[] origin=new int[2];getLocationOnScreen(origin);plannedX=origin[0];plannedY=origin[1];String raw=store.prefs.getString(key,"{}"),desktops="";
        StringBuilder signature=new StringBuilder(key).append(':').append(width).append(':').append(w).append(':').append(h).append(':').append(viewportHeight).append(':').append(layoutHeight).append(':').append(plannedX).append(':').append(plannedY).append(':').append(cutouts.hashCode()).append(':').append(store.prefs.getBoolean("snap_desktop",true));for(int i=0;i<count;i++)signature.append('|').append(getChildAt(i).getTag());String planKey=signature.toString();
        if(!planKey.equals(lastPlanKey)||!java.util.Objects.equals(raw,lastPositions)||!java.util.Objects.equals(desktops,lastDesktops)){
            planBuilds++;
            positions.clear();for(int i=0;i<count;i++)positions.add(null);
        JSONObject all=saved();boolean snap=store.prefs.getBoolean("snap_desktop",true);int bottom=Math.max(viewportHeight,h);float density=getResources().getDisplayMetrics().density;
        int extent=bottom;for(int i=0;i<count;i++){JSONObject point=all.optJSONObject(String.valueOf(getChildAt(i).getTag()));if(point!=null)extent=Math.max(extent,Math.round(DesktopGeometry.clamp((float)point.optDouble("y",0),0,60000)*density)+h);}
        for(int i=0;i<count;i++){JSONObject point=all.optJSONObject(String.valueOf(getChildAt(i).getTag()));if(point==null)continue;Rect cell=at((float)point.optDouble("x",0)*Math.max(0,width-w),DesktopMetrics.fittedY((float)point.optDouble("y",0)*density,h,viewportHeight,layoutHeight),width,extent,snap);if(!blocked(cell)&&(!snap||!occupied(cell,positions))){positions.set(i,cell);bottom=Math.max(bottom,cell.bottom);}}
        int columns=Math.max(1,width/w),rows=Math.max(1,viewportHeight/h);
        for(int i=0;i<count;i++)if(positions.get(i)==null){boolean found=false;for(int extra=0;!found;extra++){for(int column=0;column<columns&&!found;column++)for(int row=0;row<rows+extra&&!found;row++){int top=DesktopMetrics.rowTop(row,h,viewportHeight);Rect cell=new Rect(column*w,top,(column+1)*w,top+h);if(!blocked(cell)&&!occupied(cell,positions)){positions.set(i,cell);bottom=Math.max(bottom,cell.bottom);found=true;}}}}
            cachedBottom=bottom;lastPlanKey=planKey;lastPositions=raw;lastDesktops=desktops;
        }
        for(int i=0;i<count;i++){View child=getChildAt(i);if(child instanceof android.widget.LinearLayout){android.widget.LinearLayout tile=(android.widget.LinearLayout)child;if(tile.getChildCount()>0){int labelHeight=0;if(tile.getChildCount()>1&&tile.getChildAt(1).getVisibility()!=GONE){android.widget.TextView label=(android.widget.TextView)tile.getChildAt(1);label.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w-tile.getPaddingLeft()-tile.getPaddingRight()),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));labelHeight=label.getMeasuredHeight();}int size=DesktopMetrics.icon(Aero.dp(getContext(),store.iconSize()),w,h,tile.getPaddingLeft()+tile.getPaddingRight()+Aero.dp(getContext(),4),tile.getPaddingTop()+tile.getPaddingBottom(),labelHeight);View icon=tile.getChildAt(0);ViewGroup.LayoutParams lp=icon.getLayoutParams();if(lp.width!=size||lp.height!=size){lp.width=size;lp.height=size;icon.setLayoutParams(lp);}}}child.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));}setMeasuredDimension(width,resolveSize(cachedBottom,heightSpec));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){for(int i=0;i<getChildCount()&&i<positions.size();i++){Rect p=positions.get(i);getChildAt(i).layout(p.left,p.top,p.right,p.bottom);}int[] origin=new int[2];getLocationOnScreen(origin);if(origin[0]!=plannedX||origin[1]!=plannedY)post(this::requestLayout);}
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-2,-2);}
}
