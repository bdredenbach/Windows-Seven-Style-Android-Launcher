package com.nthexperience.desktop;

import android.os.Build;
import android.view.*;
import android.widget.ListView;
import android.widget.AbsListView;

/** One Vista menu surface for touch, keyboard and secondary mouse buttons. */
final class ContextMenus {
    interface Open {void show(float x,float y);}
    static void show(DesktopContextMenu menu,View anchor,float x,float y){View root=anchor.getRootView();WindowInsets insets=root.getRootWindowInsets();int l=0,t=0,r=0,b=0;if(insets!=null){if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());l=safe.left;t=safe.top;r=safe.right;b=safe.bottom;}else{l=insets.getSystemWindowInsetLeft();t=insets.getSystemWindowInsetTop();r=insets.getSystemWindowInsetRight();b=insets.getSystemWindowInsetBottom();}}menu.show(root,x,y,l,t,r,b);}
    static void centered(View anchor,Open open){int[] p=new int[2];anchor.getLocationOnScreen(p);open.show(p[0]+anchor.getWidth()/2f,p[1]+anchor.getHeight()/2f);}
    static void bind(View view,Open open){float[] point={Float.NaN,Float.NaN};view.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN){point[0]=event.getRawX();point[1]=event.getRawY();if(event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){open.show(point[0],point[1]);return true;}}return false;});view.setOnLongClickListener(v->{if(Float.isFinite(point[0]))open.show(point[0],point[1]);else centered(v,open);return true;});view.setOnContextClickListener(v->{centered(v,open);return true;});view.setOnKeyListener((v,key,event)->{if(event.getAction()==KeyEvent.ACTION_DOWN&&key==KeyEvent.KEYCODE_MENU){centered(v,open);return true;}return false;});}
    static void bindJumpList(View view,Open open){
        bind(view,open);
        view.setOnTouchListener(new View.OnTouchListener(){
            private float startX,startY;
            private boolean tracking,swiped;

            @Override public boolean onTouch(View button,MotionEvent event){
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        startX=event.getRawX();startY=event.getRawY();
                        swiped=false;
                        tracking=!event.isButtonPressed(MotionEvent.BUTTON_SECONDARY);
                        if(!tracking){open.show(startX,startY);return true;}
                        return false;
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_CANCEL:
                        tracking=false;
                        return swiped;
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                        if(!tracking)return false;
                        if(!swiped){
                            float density=button.getResources().getDisplayMetrics().density;
                            float dx=(event.getRawX()-startX)/density;
                            float dy=(event.getRawY()-startY)/density;
                            if(TaskbarFlick.opensJumpList(dx,dy,event.getEventTime()-event.getDownTime())){
                                swiped=true;
                                MotionEvent cancel=MotionEvent.obtain(event);
                                cancel.setAction(MotionEvent.ACTION_CANCEL);
                                button.onTouchEvent(cancel);
                                cancel.recycle();
                                if(button.getParent()!=null)button.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }
                        if(event.getActionMasked()==MotionEvent.ACTION_UP){
                            tracking=false;
                            if(swiped)open.show(startX,startY);
                        }
                        return swiped;
                    default:
                        return false;
                }
            }
        });
    }
    static void blank(View view,Open open){GestureDetector gestures=new GestureDetector(view.getContext(),new GestureDetector.SimpleOnGestureListener(){public boolean onDown(MotionEvent e){return true;}public void onLongPress(MotionEvent e){if(!(view instanceof AbsListView)||((AbsListView)view).pointToPosition((int)e.getX(),(int)e.getY())==ListView.INVALID_POSITION)open.show(e.getRawX(),e.getRawY());}});view.setOnTouchListener((v,e)->{boolean blank=!(v instanceof ListView)||((ListView)v).pointToPosition((int)e.getX(),(int)e.getY())==ListView.INVALID_POSITION;if(e.getActionMasked()==MotionEvent.ACTION_DOWN&&blank&&e.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){open.show(e.getRawX(),e.getRawY());return true;}gestures.onTouchEvent(e);return false;});view.setOnContextClickListener(v->{centered(v,open);return true;});}
    static void desktopIcon(View view,Open open,Runnable drag){
        float[] down=new float[2];boolean[] held={false},moving={false};Runnable show=()->{if(held[0]&&!moving[0])open.show(down[0],down[1]);};
        view.setOnLongClickListener(v->{held[0]=true;view.postDelayed(show,800);return true;});
        view.setOnTouchListener((v,e)->{int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){v.removeCallbacks(show);held[0]=false;moving[0]=false;down[0]=e.getRawX();down[1]=e.getRawY();if(e.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){open.show(down[0],down[1]);return true;}}
            if(action==MotionEvent.ACTION_MOVE&&held[0]&&!moving[0]&&Math.hypot(e.getRawX()-down[0],e.getRawY()-down[1])>Aero.dp(v.getContext(),8)){moving[0]=true;held[0]=false;v.removeCallbacks(show);drag.run();return true;}
            if(action==MotionEvent.ACTION_UP){v.removeCallbacks(show);if(held[0]&&!moving[0]){held[0]=false;open.show(down[0],down[1]);return true;}}if(action==MotionEvent.ACTION_CANCEL){held[0]=false;v.removeCallbacks(show);}return false;});
        view.setOnContextClickListener(v->{centered(v,open);return true;});
    }
}
