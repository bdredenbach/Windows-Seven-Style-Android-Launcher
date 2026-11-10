package com.nthexperience.desktop;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/** Separates brief horizontal drags, fast scrolling, upward flicks, and still holds. */
final class TaskbarTouch {
    static void bind(View view,TaskbarReorder reorder,ContextMenus.Open jump){
        view.setOnTouchListener(new View.OnTouchListener(){
            float x,y;boolean tracking,swiped,scrolling;
            public boolean onTouch(View button,MotionEvent event){
                AeroGlass glass=button.getBackground() instanceof AeroGlass?(AeroGlass)button.getBackground():null;
                int action=event.getActionMasked();
                if(glass!=null)glass.press(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE,event.getX(),event.getY());
                if(action==MotionEvent.ACTION_DOWN){
                    x=event.getRawX();y=event.getRawY();tracking=true;swiped=scrolling=false;
                    if(event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){tracking=false;button.performContextClick();return true;}
                    if(button.getParent()!=null)button.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                }
                if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_CANCEL){tracking=false;if(glass!=null)glass.press(false,0,0);return swiped;}
                if(!tracking)return false;
                if(action!=MotionEvent.ACTION_MOVE&&action!=MotionEvent.ACTION_UP)return false;
                float density=button.getResources().getDisplayMetrics().density;
                float dx=(event.getRawX()-x)/density,dy=(event.getRawY()-y)/density;
                long elapsed=event.getEventTime()-event.getDownTime();
                if(action==MotionEvent.ACTION_MOVE&&!scrolling&&!swiped&&reorder.expanded()&&elapsed>=160&&(Math.abs(dx)>=12||Math.abs(dy)>=12)){
                    button.cancelLongPress();if(reorder.begin(button)){tracking=false;cancelButton(button,event);return true;}
                }
                if(!scrolling&&!swiped&&jump!=null&&TaskbarFlick.opensJumpList(dx,dy,elapsed)){
                    swiped=true;cancelButton(button,event);
                }
                if(action==MotionEvent.ACTION_MOVE&&!scrolling&&!swiped){
                    boolean mouse=event.isFromSource(InputDevice.SOURCE_MOUSE);
                    if(TaskbarOrder.startsDrag(dx,dy,elapsed,mouse)){
                        button.cancelLongPress();
                        if(reorder.begin(button)){tracking=false;cancelButton(button,event);return true;}
                    }else if(Math.abs(dx)>=12&&Math.abs(dx)>Math.abs(dy)*1.35f&&elapsed<160){
                        scrolling=true;
                        if(button.getParent()!=null)button.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                if(action==MotionEvent.ACTION_UP){
                    tracking=false;
                    if(swiped&&jump!=null)jump.show(x,y);
                    if(!swiped&&(scrolling||Math.abs(dx)>=12||Math.abs(dy)>=12)){cancelButton(button,event);return true;}
                }
                return swiped;
            }
        });
        view.setOnKeyListener((button,key,event)->{
            if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if((key==KeyEvent.KEYCODE_MENU||(key==KeyEvent.KEYCODE_F10&&event.isShiftPressed()))){button.performContextClick();return true;}
            if(event.isCtrlPressed()&&event.isShiftPressed()&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT))
                return reorder.move(TaskbarReorder.key(button),key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);
            if(event.isCtrlPressed()&&event.isShiftPressed()&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN))
                return reorder.moveRow(TaskbarReorder.key(button),key==KeyEvent.KEYCODE_DPAD_UP?0:1);
            return false;
        });
        view.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){
                super.onInitializeAccessibilityNodeInfo(host,info);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.taskbar_move_left,"Move left on taskbar"));
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.taskbar_move_right,"Move right on taskbar"));
                if(reorder.expanded()){
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.taskbar_move_top,"Move to top taskbar row"));
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.taskbar_move_bottom,"Move to bottom taskbar row"));
                }
            }
            @Override public boolean performAccessibilityAction(View host,int action,android.os.Bundle args){
                if(action==R.id.taskbar_move_left||action==R.id.taskbar_move_right)
                    return reorder.move(TaskbarReorder.key(host),action==R.id.taskbar_move_left?-1:1);
                if(action==R.id.taskbar_move_top||action==R.id.taskbar_move_bottom)return reorder.moveRow(TaskbarReorder.key(host),action==R.id.taskbar_move_top?0:1);
                return super.performAccessibilityAction(host,action,args);
            }
        });
    }
    private static void cancelButton(View button,MotionEvent event){
        MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);
        button.onTouchEvent(cancel);cancel.recycle();button.setPressed(false);
    }
}
