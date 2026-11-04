package com.nthexperience.desktop;

import java.util.*;

/** One owner per setting. Category tasks and search resolve to the same destinations. */
final class ControlPanelCatalog {
    static final String[][] CATEGORIES={
        {"system","System and Security","security"},
        {"network","Network and Internet","network"},{"hardware","Hardware and Sound","hardware"},
        {"programs","Programs","programs"},{"accounts","User Accounts","accounts"},
        {"appearance","Appearance and Personalization","appearance"},{"clock","Clock, Language, and Region","clock"},
        {"accessibility","Ease of Access","accessibility"}};
    static final class Task {
        final String title,description,destination;
        Task(String title,String description,String destination){this.title=title;this.description=description;this.destination=destination;}
    }
    static final class Panel {
        final String id,category,title,icon,description,destination;final List<Task> tasks;final String[] aliases;
        Panel(String id,String category,String title,String icon,String description,String destination,String aliases,Task...tasks){this.id=id;this.category=category;this.title=title;this.icon=icon;this.description=description;this.destination=destination;this.aliases=aliases.isEmpty()?new String[0]:aliases.split(";");this.tasks=Collections.unmodifiableList(Arrays.asList(tasks));}
    }
    static final class Hit {
        final Panel owner;final String title,description,destination;
        Hit(Panel owner,String title,String description,String destination){this.owner=owner;this.title=title;this.description=description;this.destination=destination;}
    }
    private static Task t(String title,String description,String destination){return new Task(title,description,destination);}
    static final List<Panel> PANELS=Collections.unmodifiableList(Arrays.asList(
        new Panel("welcome","system","Welcome Center","orb","Set up your Nth desktop","open:@welcome","Getting started"),
        new Panel("system","system","System","computer","View device, memory and desktop information","page:system","Device information;RAM;processor",t("Task Manager","Switch windows, end a Nth task and view performance","open:@taskmanager"),t("Saved Workspaces","Save and reopen Nth window layouts","open:@workspaces"),t("Recycle Bin","Restore files deleted through Nth Explorer","open:@recycle"),t("Recent Items","Recent documents, pictures and folders","open:@recent")),
        new Panel("backup","system","Backup and Restore","drive","Save or restore your desktop, folders and notes","open:@backup","Backup and Restore Center"),
        new Panel("access","system","File access","files","Set up device access or connect folders","storage-access","permissions;storage access"),
        new Panel("storage","system","Storage","drive","Storage use and device cleanup · Android","android:storage","disk space"),
        new Panel("power","system","Power Options","power","Battery and power saving · Android","android:battery","battery settings"),
        new Panel("security","system","Security settings","security","Screen lock and device security · Android","android:security","lock screen"),
        new Panel("privacy","system","Privacy settings","security","Review privacy controls · Android","android:privacy","privacy permissions"),
        new Panel("network","network","Network and Sharing Center","network","View network connections · Android","android:network","Network connections;Internet settings",t("Wi-Fi","Choose a wireless network · Android","android:wifi")),
        new Panel("sound","hardware","Sound","sound","Volume and device sounds · Android","android:sound","audio volume",t("Desktop sound effects","Original optional completion and error cues","page:sounds")),
        new Panel("display","hardware","Display Settings","computer","Brightness, display size and rotation · Android","android:display","display brightness",t("Desktop brightness","Adjust Nth or whole-phone brightness","open:@brightness")),
        new Panel("bluetooth","hardware","Bluetooth Devices","hardware","Connect nearby devices · Android","android:bluetooth","Bluetooth devices"),
        new Panel("programs","programs","Programs and Features","programs","Manage installed Android apps","android:apps","installed applications"),
        new Panel("defaults","programs","Default Programs","programs","Choose default Android apps","android:defaults","default applications"),
        new Panel("managers","programs","File Managers","folderapps","Organize your file manager shortcuts","open:@filemanagers","file manager apps"),
        new Panel("accounts","accounts","User Accounts","accounts","Change your desktop name and profile picture","page:accounts","Change your desktop name;Change your profile picture",t("Device accounts","Manage accounts on your device · Android","android:accounts")),
        new Panel("personalization","appearance","Personalization","appearance","Change the appearance and sounds of your desktop","page:personalization","",t("Window Color","Window color and appearance, including glass intensity, transparency and opacity","page:colors"),t("Desktop Background","Choose a stock or personal wallpaper","page:background"),t("Startup and Sounds","Startup intro, reboot, chime and reduced motion","page:startup")),
        new Panel("icons","appearance","Desktop Settings","computer","Desktop icons, grid, spacing, labels and snapping","page:icons","Desktop Icon Settings;desktop layout",t("Manage desktop shortcuts","Desktop Organization: folders, shortcut names and ordering","open:@organize")),
        new Panel("taskbar","appearance","Taskbar and Start Menu","control","Status bar, Android app buttons and default home","page:taskbar","taskbar properties",t("Quick Launch","Choose and order taskbar shortcuts","open:@quicklaunch"),t("Taskbar grouping","Group similar folder windows when space runs out","page:grouping")),
        new Panel("sidebar","appearance","Gadget Settings","gadgets","Show, hide and arrange desktop gadgets","page:sidebar","Sidebar Properties;Windows Sidebar Properties",t("Desktop Gadgets","Add and arrange desktop gadgets","open:@gadgets")),
        new Panel("date","clock","Date and Time","clock","Device date and time zone · Android","android:date","",t("Clock appearance","Clock face and second hand","page:clock")),
        new Panel("region","clock","Regional and Language Options","network","Device language and region · Android","android:language","language locale"),
        new Panel("accessibility","accessibility","Ease of Access Center","accessibility","Accessibility services and display aids · Android","android:accessibility","accessibility")));
    static List<Panel> category(String id){List<Panel> result=new ArrayList<>();for(Panel panel:PANELS)if(panel.category.equals(id))result.add(panel);return result;}
    static Panel panel(String id){for(Panel panel:PANELS)if(panel.id.equals(id))return panel;return null;}
    static Panel owner(String destination){for(Panel panel:PANELS){if(panel.destination.equals(destination))return panel;for(Task task:panel.tasks)if(task.destination.equals(destination))return panel;}return null;}
    static List<Panel> classic(){List<Panel> result=new ArrayList<>(PANELS);result.sort(Comparator.comparing(p->p.title.toLowerCase(Locale.ROOT)));return result;}
    private static boolean matches(String query,String...values){String haystack=String.join(" ",values).toLowerCase(Locale.ROOT);for(String word:query.toLowerCase(Locale.ROOT).trim().split("\\s+"))if(!haystack.contains(word))return false;return true;}
    static List<Hit> search(String query){
        LinkedHashMap<String,Hit> result=new LinkedHashMap<>();for(Panel panel:PANELS){String label=panel.title;for(String alias:panel.aliases)if(alias.equalsIgnoreCase(query.trim())){label=alias;break;}if(matches(query,panel.title,panel.description,String.join(" ",panel.aliases)))result.put(panel.destination,new Hit(panel,label,panel.description,panel.destination));for(Task task:panel.tasks)if(matches(query,task.title,task.description))result.putIfAbsent(task.destination,new Hit(panel,task.title,task.description,task.destination));}return new ArrayList<>(result.values());
    }
    static String parent(String page){if(page.equals("home"))return null;if(page.startsWith("category:"))return "home";Panel owner=owner("page:"+page);if(owner==null)return "home";return owner.destination.equals("page:"+page)?"category:"+owner.category:owner.destination.startsWith("page:")?owner.destination.substring(5):"category:"+owner.category;}
}
