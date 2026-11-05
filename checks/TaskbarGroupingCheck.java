package com.nthexperience.desktop;
import java.util.*;
public final class TaskbarGroupingCheck {
    public static void main(String[] args){
        check(TaskbarFlick.opensJumpList(2,-12,80),"Short upward flick opens the Jump List");
        check(TaskbarFlick.opensJumpList(11,-14,90),"Diagonal upward flick tolerates sideways drift");
        check(TaskbarFlick.opensJumpList(15,-22,480),"Deliberate upward swipe still opens the Jump List");
        check(!TaskbarFlick.opensJumpList(1,-6,20),"Tap jitter is too small");
        check(!TaskbarFlick.opensJumpList(1,-12,340),"Small slow movement does not open the menu");
        check(!TaskbarFlick.opensJumpList(25,-12,70),"Horizontal scroll does not open the menu");
        check(!TaskbarFlick.opensJumpList(2,18,80),"Downward motion does not open the menu");
        check(!TaskbarFlick.opensJumpList(0,0,600),"A stationary hold does not open the menu");
        List<String> ids=Arrays.asList("notes","files1","calendar","files2","files3");Set<String> folders=new HashSet<>(Arrays.asList("files1","files2","files3"));
        check(TaskbarGrouping.groups(ids,folders,"Never",1,124).size()==5,"Never keeps each window");check(TaskbarGrouping.groups(ids,folders,"When full",620,124).size()==5,"Exact fit stays ungrouped");List<List<String>> groups=TaskbarGrouping.groups(ids,folders,"When full",619,124);check(groups.size()==3&&groups.get(1).equals(Arrays.asList("files1","files2","files3")),"Crowded folders combine in stable order");check(groups.get(0).equals(Collections.singletonList("notes"))&&groups.get(2).equals(Collections.singletonList("calendar")),"Other tasks keep relative order");check(TaskbarGrouping.groups(ids,folders,"Always",9999,124).equals(groups),"Always groups at wide sizes");for(int n=0;n<50;n++){List<String> input=new ArrayList<>();Set<String> family=new HashSet<>();for(int i=0;i<n;i++){input.add("task"+i);if(i%3==0)family.add("task"+i);}for(String mode:Arrays.asList("Never","When full","Always"))for(int width:Arrays.asList(0,1,300,2000)){List<String> flat=new ArrayList<>();for(List<String> group:TaskbarGrouping.groups(input,family,mode,width,124))flat.addAll(group);check(flat.size()==n&&new HashSet<>(flat).equals(new HashSet<>(input)),"Every window appears exactly once");}}check(TaskbarGrouping.groups(Collections.emptyList(),folders,"Always",0,124).isEmpty(),"No phantom tasks");System.out.println("PASS: taskbar grouping preserves all real windows, stable families, exact fit/overflow behavior and upward flick recognition");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
