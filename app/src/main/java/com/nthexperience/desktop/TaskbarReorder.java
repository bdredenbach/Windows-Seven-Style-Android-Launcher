package com.nthexperience.desktop;

import android.content.ClipData;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Local drag/drop with a drop marker; children are rebuilt only after the drag ends. */
final class TaskbarReorder {
    private final LinearLayout items;
    private final HorizontalScrollView scroll;
    private final Consumer<List<String>> finished;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final GradientDrawable marker=new GradientDrawable();
    private Move active;
    private boolean closed,markerAdded;
    private float rawX;
    private final Runnable autoscroll=new Runnable(){public void run(){
        if(active==null||closed)return;
        int[] at=new int[2];scroll.getLocationOnScreen(at);
        int edge=Aero.dp(items.getContext(),28),step=Aero.dp(items.getContext(),6);
        int direction=rawX<at[0]+edge?-1:rawX>at[0]+scroll.getWidth()-edge?1:0;
        if(direction!=0){scroll.scrollBy(direction*step,0);updateMarker();}
        main.postDelayed(this,16);
    }};
    private static final class Move {
        final TaskbarReorder owner;final View source;final String id;
        List<String> order;boolean ended;
        Move(TaskbarReorder owner,View source,String id){this.owner=owner;this.source=source;this.id=id;}
    }
    TaskbarReorder(LinearLayout items,HorizontalScrollView scroll,Consumer<List<String>> finished){
        this.items=items;this.scroll=scroll;this.finished=finished;
        marker.setColor(0xff80d7ff);marker.setStroke(Aero.dp(items.getContext(),1),0xfff0fbff);
        items.setOnDragListener((view,event)->drag(event));
    }
    static String key(View view){Object value=view.getTag(R.id.taskbar_order_key);return value instanceof String?(String)value:null;}
    List<String> keys(){
        List<String> result=new ArrayList<>();
        for(int i=0;i<items.getChildCount();i++){String key=key(items.getChildAt(i));if(key!=null)result.add(key);}
        return result;
    }
    boolean dragging(){return active!=null;}
    boolean begin(View button){
        String id=key(button);
        if(closed||active!=null||id==null||keys().size()<2)return false;
        Move move=new Move(this,button,id);active=move;int[] start=new int[2];button.getLocationOnScreen(start);rawX=start[0]+button.getWidth()/2f;
        boolean started=button.startDragAndDrop(ClipData.newPlainText("taskbar-order",id),new View.DragShadowBuilder(button),move,0);
        if(!started){active=null;return false;}
        button.setAlpha(.4f);main.post(autoscroll);return true;
    }
    boolean move(String id,int delta){
        if(closed||active!=null)return false;
        List<String> current=keys();int index=current.indexOf(id);
        if(index<0||index+delta<0||index+delta>=current.size())return false;
        finished.accept(TaskbarOrder.move(current,id,index+delta));return true;
    }
    private boolean drag(DragEvent event){
        Object state=event.getLocalState();
        if(!(state instanceof Move)||((Move)state).owner!=this)return false;
        Move move=(Move)state;
        if(move.ended)return true;
        switch(event.getAction()){
            case DragEvent.ACTION_DRAG_STARTED:return !closed;
            case DragEvent.ACTION_DRAG_LOCATION:
            case DragEvent.ACTION_DRAG_ENTERED:
                int[] at=new int[2];items.getLocationOnScreen(at);rawX=at[0]+event.getX();updateMarker();return true;
            case DragEvent.ACTION_DRAG_EXITED:removeMarker();return true;
            case DragEvent.ACTION_DROP:
                move.order=TaskbarOrder.move(keys(),move.id,indexAt(event.getX(),move.id));return true;
            case DragEvent.ACTION_DRAG_ENDED:finish(move,event.getResult());return true;
            default:return true;
        }
    }
    private int indexAt(float x,String moving){
        int index=0;
        for(int i=0;i<items.getChildCount();i++){
            View child=items.getChildAt(i);String id=key(child);
            if(id!=null&&!id.equals(moving)&&x>child.getLeft()+child.getWidth()/2f)index++;
        }
        return index;
    }
    private void updateMarker(){
        if(active==null)return;
        int[] at=new int[2];items.getLocationOnScreen(at);
        int index=indexAt(rawX-at[0],active.id),seen=0,x=0;
        for(int i=0;i<items.getChildCount();i++){
            View child=items.getChildAt(i);String id=key(child);
            if(id==null||id.equals(active.id))continue;
            if(seen++==index){x=child.getLeft();break;}
            x=child.getRight();
        }
        int width=Aero.dp(items.getContext(),2),pad=Aero.dp(items.getContext(),3);
        marker.setBounds(Math.max(0,x-width),pad,Math.max(width,x+width),Math.max(pad+1,items.getHeight()-pad));
        if(!markerAdded){items.getOverlay().add(marker);markerAdded=true;}
        marker.invalidateSelf();
    }
    private void removeMarker(){if(markerAdded){items.getOverlay().remove(marker);markerAdded=false;}}
    private void finish(Move move,boolean accepted){
        if(move.ended)return;move.ended=true;move.source.setAlpha(1);
        main.removeCallbacks(autoscroll);removeMarker();if(active==move)active=null;
        List<String> order=accepted?move.order:null;
        if(!closed)items.post(()->{if(!closed)finished.accept(order);});
    }
    void cancel(){if(active!=null){Move move=active;move.source.cancelDragAndDrop();finish(move,false);}}
    void close(){closed=true;cancel();main.removeCallbacksAndMessages(null);removeMarker();}
}
