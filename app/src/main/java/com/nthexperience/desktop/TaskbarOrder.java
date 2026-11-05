package com.nthexperience.desktop;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Stable ordering of real taskbar slots, independent of their running state. */
final class TaskbarOrder {
    static final String KEY="taskbar_order";
    static final int LIMIT=64;
    static boolean valid(String id){
        if(id==null||id.isEmpty()||id.length()>550)return false;
        for(int i=0;i<id.length();i++)if(Character.isISOControl(id.charAt(i)))return false;
        return true;
    }
    static List<String> clean(List<String> ids){
        LinkedHashSet<String> result=new LinkedHashSet<>();
        for(String id:ids)if(valid(id)&&result.size()<LIMIT)result.add(id);
        return new ArrayList<>(result);
    }
    static List<String> apply(List<String> available,List<String> saved){
        LinkedHashSet<String> remaining=new LinkedHashSet<>(available),result=new LinkedHashSet<>();
        remaining.remove(null);
        for(String id:clean(saved))if(remaining.remove(id))result.add(id);
        result.addAll(remaining);
        return new ArrayList<>(result);
    }
    static List<String> move(List<String> current,String id,int position){
        List<String> result=new ArrayList<>(current);
        if(!result.remove(id))return result;
        result.add(Math.max(0,Math.min(position,result.size())),id);
        return result;
    }
    static boolean startsDrag(float dx,float dy,long elapsed,boolean mouse){
        return elapsed>=0&&(mouse||elapsed>=160)&&Math.abs(dx)>=12&&Math.abs(dx)>Math.abs(dy)*1.35f;
    }
}
