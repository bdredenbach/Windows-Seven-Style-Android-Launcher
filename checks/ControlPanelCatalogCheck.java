package com.nthexperience.desktop;
import java.util.*;
public class ControlPanelCatalogCheck {
    public static void main(String[] args){
        Set<String> destinations=new HashSet<>();for(ControlPanelCatalog.Panel panel:ControlPanelCatalog.PANELS){check(destinations.add(panel.destination),"One applet owns "+panel.destination);check(ControlPanelCatalog.category(panel.category).contains(panel),"Applet has a supported category");for(ControlPanelCatalog.Task task:panel.tasks)check(destinations.add(task.destination),"One owner per task: "+task.destination);}
        check(ControlPanelCatalog.category("appearance").size()==4,"Appearance has four distinct applets");check(ControlPanelCatalog.panel("personalization").tasks.size()==3,"Personalization contains appearance tasks without cloning sibling applets");
        for(String query:new String[]{"desktop","sounds","wireless","opacity","gadgets","Window Color and Appearance"}){List<ControlPanelCatalog.Hit> hits=ControlPanelCatalog.search(query);check(!hits.isEmpty(),"Useful search: "+query);Set<String> found=new HashSet<>();for(ControlPanelCatalog.Hit hit:hits)check(found.add(hit.destination),"Search returns each destination once");}
        check(ControlPanelCatalog.search("Change your desktop name").stream().anyMatch(h->h.destination.equals("page:accounts")&&h.title.equals("Change your desktop name")),"Existing account task remains searchable");check(ControlPanelCatalog.search("wireless").stream().anyMatch(h->h.destination.equals("android:wifi")),"Wi-Fi synonym reaches the real Android route");check(ControlPanelCatalog.search("grid icons").stream().anyMatch(h->h.destination.equals("page:icons")),"Word order does not hide desktop grid");
        check(ControlPanelCatalog.parent("colors").equals("personalization"),"Color belongs to Personalization");check(ControlPanelCatalog.parent("icons").equals("category:appearance"),"Desktop Settings has its own canonical category");check(ControlPanelCatalog.parent("home")==null,"Root has no parent");
        String previous="";for(ControlPanelCatalog.Panel panel:ControlPanelCatalog.classic()){String title=panel.title.toLowerCase(Locale.ROOT);check(previous.compareTo(title)<=0,"Classic applets are alphabetized");previous=title;}
        System.out.println("PASS: unique Control Panel owners, distinct appearance applets, searchable aliases, canonical parents and alphabetical Classic View");
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
