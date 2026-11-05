# Nth Desktop Seven for Android

Nth Desktop Seven is a Windows 7–inspired Android home launcher. Its first test build, **0.1.0**, adds Aero Snap and installs alongside Nth Desktop Vista under the separate app ID `com.nthexperience.desktop.seven`.

**0.6.0** completes the Seven interaction pass:

- Briefly press a taskbar icon, then drag sideways to reorder. Quick horizontal swipes still scroll, upward flicks open Jump Lists, and stationary holds show previews. Order survives restarts; Ctrl+Shift+Left/Right and accessibility actions also move icons.
- Taskbar hot tracking uses the icon's dominant color while respecting glass opacity. Caption controls use consistent drawn glyphs, with shared Seven toolbar and menu highlights.
- Hold the far-right Show desktop strip for temporary hollow-window Aero Peek. Release or cancel to restore contents; a short tap still toggles the desktop. Mouse hover also Peeks. Window geometry, minimized states and private-preview rules are preserved.
- Four original offline sound schemes provide completion, error and notification cues. Test buttons preview without enabling automatic sounds. Schemes follow themes and portable theme files; older theme files remain compatible. Android silent mode, Do Not Disturb, system volume and active media are respected.
- The Action Center flag opens a compact flyout for actual Nth file/backup notices and lost linked-folder access, with links to the full Action Center. Optional Home and file-access setup choices do not create warnings.

The signed 0.6.0 APK updates the original Seven 0.5.12 installation in place. Android apps continue to open through Android; floating windows and Peek apply to Nth's built-in tools.

**0.5.12** adds resizing from any corner of built-in windows. The opposite corner stays anchored and dimensions are saved for each orientation. Resizing a snapped window returns it to a floating size. Maximize and Restore retain the resized dimensions.

Usage Access setup remains available during first launch, Welcome Center and Taskbar settings. Returning from Android Settings without a grant reopens the guide. Start menu rows show an icon and label.

**0.5.1** recognizes short upward flicks on taskbar icons, including quick gestures that report most of their movement at release. Hold an icon to see window previews.

**0.5.0** adds phone-width Aero Snap, title-bar Aero Shake, Jump Lists, media controls on previews, taskbar progress, multi-folder Libraries, Explorer previews, portable My Themes, and a tray Action Center with Steps Recorder.

- Drag a title bar to a side edge, or use **Window menu → Snap left / Snap right**. The Super key and arrow keys provide keyboard Snap where Android passes the shortcut through.
- Swipe up on a taskbar button or right-click it to open a Jump List. Hold the taskbar button for window previews.
- In Explorer, use **Include in library…** on a folder, then **Manage library locations…** to choose the default save location. The Preview Pane button or **Alt+P** opens image and text previews.
- Open **Personalization → My Themes** to save the current look and export an Nth theme with its pictures. Open the tray flag to review launcher alerts or export an opt-in Steps Recorder report.

**0.4.2** browses for desktop background pictures and slideshow folders in Nth Explorer. Use device storage or a linked folder and apply your selection with Save changes. Android handles the permission prompt when you connect a new folder.

**0.4.1** fixes the first render of Flip 3D for small window stacks.

**0.4.0** opens taskbar previews on a hold, adds mouse Aero Peek, and keeps Explorer and shortcut folders in the same taskbar family. Flip 3D works with a desktop-only stack and is searchable from Start. Double-click a gadget to add it to the right edge. Personalization adds seven original backgrounds, new themes, and a checked picture gallery with folder browsing, timed rotation and shuffle.

**0.3.0** groups Explorer windows into one stacked taskbar button by default. Hold it to see updating thumbnails and choose a window. Start has a raised profile picture, and the taskbar shows the time above the date. Personalization offers theme previews and the desktop context menu changes the wallpaper. Gadgets can be placed or resized directly on the desktop.

**0.2.0** adds an N⁷ orb and four blue flags that swirl into it during startup. Seven Blue is the default desktop background when no background has been saved. Pinned and running apps share icon slots. Taskbar and Start Menu settings can show labels and the classic Quick Launch strip. Explorer’s Libraries view opens the real document, picture, music and video folders. Media Player has a light Library and dark Now Playing view, while full-screen Media Center uses a blue Start screen with Music, Videos and Now Playing hubs.

**0.1.1** opens Android’s default Home chooser once after the intro or as soon as the desktop is ready when the intro is off. Existing Seven 0.1.0 installs receive the offer on their next launch if Seven is still eligible.

- Drag an Nth window by its title bar to the top to maximize it, or to the left or right to fill that half of the usable desktop.
- The blue outline shows where the window will land. Drag a snapped window away by its title bar to restore its previous floating size.
- Side snap requires 160 dp of width per window and 190 dp of usable height. The work area accounts for the taskbar, system insets and any space reserved for an always-on-top Sidebar.
- Android grants Seven its own file access. Desktop settings, media libraries and Recycle Bin entries belong to each app's install.

Seven starts from [Vista 0.24.0 at commit `7c3fd10`](https://github.com/bdredenbach/Windows-Vista-Style-Android-Launcher/commit/7c3fd109ac1fae51486c8f645b5f7cba7370bdeb). Use `./gradlew :app:assembleDebug` to build it. Android 11 and Android 16 emulator checks cover the inherited tools, side-by-side installation and title-bar gestures.

## Inherited Vista features and history

A Vista-style Android home launcher by Nth Experience. **0.22.0 — Vista finishing touches** adds glass-aware active task buttons, folder-window grouping, a notification-area chevron with original Vista-inspired device icons, matching thin Quick Launch separators, an Explorer-style Recycle Bin, a thumbnail background picker, independent Explorer windows, verified file drag/drop, real-folder desktop shortcuts and a framed Gadget Gallery with Sidebar close/options/move tools (hover or hold the options button to reveal the full strip).

**Task Manager** now has Applications, Processes, Performance and Networking tabs. Its original animated green graphs interpolate real samples: Nth own-process CPU, physical device RAM, Nth Java heap and device-wide received/sent interface counters. View offers High/Normal/Low speeds, Refresh and Pause. Options toggles animation; reduced motion, hiding and backgrounding stop it. Processes shows actual Nth-owned PIDs and PSS. Other Android apps remain recent launch entries with App Info; no imaginary process list or other-app termination is offered.

**Explorer:** hold a file/folder icon to drag it into a folder or another Explorer window; choose Copy here or Move here. Moves retain the existing byte-verification path before original deletion. Hold its name for the context menu, including Open in new window and Add desktop shortcut. Dropping a directory on the desktop creates a link; files remain in place. Up to 12 Explorer windows keep separate paths and view state. Taskbar Properties chooses Never, When full or Always grouping; a group opens real window snapshots and individual switching/closing controls.

**Desktop Background:** Personalization offers thumbnail previews, Fill/Fit/Stretch/Tile/Center, background colours, Save changes and Cancel changes. In Seven, both Browse buttons use Nth Explorer. Preview does not apply until saved. Selected picture references and positions are backed up; Android permission grants are not transferred.

Expand the notification area and tap the **Task Manager** icon to open Task Manager. Hold the taskbar grip and choose **Switch Windows** for snapshots of actual Nth windows. Mouse hover previews are optional; InPrivate browser windows never show previews. Android apps have labelled launch entries. Tap the **sun** for Nth-only brightness or explicitly enable Android's Modify system settings access for whole-phone control. Adjusting whole-phone brightness selects manual mode; Follow phone brightness removes the Nth override.

**Recycle Bin** keeps verified private copies before deleting supported regular files, up to 100 MB per file and 200 files; Properties sets total capacity from 100 MB to 2 GB (250 MB by default). Restore verifies the bytes and keeps both names when the original path is occupied. Folders and unsupported locations use an explicitly confirmed permanent-deletion path. It does not collect files deleted in other apps. Recovery copies stay until Restore or explicit deletion; uninstalling removes them. Desktop JSON backups contain presets/settings and file references, not Recycle Bin contents, thumbnails, browser data, permission grants or device brightness choices.

**Saved Workspaces** is under the taskbar menu, desktop menu and Start search. It restores Nth window positions, tool view state and Sidebar visibility while minimizing unrelated windows and retaining current notes. Browser windows reopen their current normal session; private sessions and Android app state are not stored. Six named presets are supported. Start's Recent Items now includes folder visits and documents, with a history switch and clear control. Browser Favorites are searchable; private browsing is excluded from browser history.

**Control Panel → Hardware and Sound → Sound → Desktop sound effects** offers opt-in completion, error and notification cues with volume. Startup retains its own original chime setting. Silent mode, Do Not Disturb and active music are respected.

Hold the Sidebar header or its dark navigation pill for **Bring Gadgets to Front, Add Gadgets, Properties, Help and Close Sidebar**. Closing keeps gadget contents and positions. **Desktop context menu → Show Sidebar** restores it directly; Start → Gadgets also opens the gallery; Sidebar Properties can keep it above windows while reserving room for window controls.

The built-in icons are original Canvas artwork inspired by Vista's forms and materials. They include perspective computers, golden folders, a blue Internet glyph with a gold orbit, paper documents, drives and Control Panel. No Microsoft icon binaries are bundled.

Desktop cleanup runs once on upgrade, preserving other shortcut order, file-manager app selections, linked folders, notes and gadget layouts. Restoring original desktop icons uses Computer, Internet, Notepad and Personalize. The default-home prompts follow Android's actual home-role state and refresh when the launcher resumes or a menu opens. Long-press blank desktop space or right-click with a mouse for the context menu.

The taskbar and dock use the full width available beside Android's navigation controls. Landscape camera-cutout insets no longer shrink the entire launcher horizontally; desktop icons keep a consistent grid origin and avoid actual cutout rectangles, and portrait top/bottom cutout protection and keyboard/navigation insets are retained.

## Start menu and navigation

Start’s glass navigation pane follows Window Color and Appearance’s transparency and opacity controls, including zero opacity and solid mode. The white programs pane stays readable. The three lower-right controls share one centered row; layout lock reflects its saved setting. **Start → › → Restart Nth Desktop** recreates the launcher while restoring open windows, notes and settings. Battery settings remains separately named. Android’s native power dialog requires an enabled accessibility service; this launcher uses the requested app-restart fallback without adding that access.

Up follows the parent shown in the address bar; Back and Forward follow visits. Granted file roots return to Computer without escaping their permission boundary. Add programs returns to its shortcut folder, rename/dissolve pages include their folder parent, and root pages disable Up. Control Panel has clickable category, Personalization and settings breadcrumbs. Long paths scroll to show the current location.

## Desktop Settings and folder layout

Open **Control Panel → Appearance and Personalization → Desktop Settings**. Both desktop-app and gadget snap switches, plus layout locking, live here. Set icon size (60–150%), show/hide labels, choose label size and horizontal/vertical spacing, and use the live preview. Desktop grid chooses fitted columns and rows independently for portrait and landscape; Automatic retains normal cell spacing. The available area beside the Sidebar sets readable limits. Extra shortcuts remain scrollable. Changes keep shortcuts and saved positions; snapped collisions reflow into free cells. Restore default icon layout resets presentation without changing shortcut membership. Built-in icon visibility and shortcut management remain on this page. New settings are validated in backups.

Nth Explorer and shortcut folders show navigation by default, including on phones. **Folders** or **View → Navigation pane** changes a saved preference retained across folder navigation, closing/reopening and Activity recreation. **More »** contains Videos, Downloads, File Managers and Add a folder, with no repeated Documents/Pictures/Music entries.

New and existing desktop shortcut folders share Explorer’s File/Edit/View/Tools/Help row, teal Organize/Views toolbar, circular navigation, address links, refresh, persistent navigation, icon/list/details views, name/type sorting and item counts. Views are remembered. Add programs, shortcut cut/copy/paste, rename, reorder, move to desktop and dissolve still use actual launcher data; these folders contain app shortcuts. File operations continue in the file Explorer.

## Control Panel and Personalization

Control Panel Home shows Vista's supported categories with green titles, original icons and common blue task links. Open a category to see its applets and their related tasks. **Classic View** is a fitted alphabetical icon grid containing each applet once. Use the task pane's Home/Classic links, or **Control Panel ▾** on a narrow window. Wide windows keep search beside the address bar and show a blue-to-green task pane with category navigation or relevant See also links. Narrow windows move related links below the page. The Explorer Folders toolbar and repeated status strip are removed from Control Panel.

Appearance and Personalization has four applets: Personalization, Desktop Settings, Taskbar and Start Menu, and Windows Sidebar Properties. Personalization contains Window Color and Appearance, Desktop Background, and Startup and Sounds. Desktop icon/grid/label/snap options belong to Desktop Settings. Quick Launch belongs to Taskbar. Gadget Gallery belongs to Sidebar. Links elsewhere open those same settings. Search groups matches by their owning applet, deduplicates destinations and retains familiar task names and synonyms. Existing page IDs, preferences and backup compatibility are retained.

This is a native Android adaptation, using original artwork and working launcher settings or corresponding Android settings screens. Unsupported Windows-only applets are not added. The Vista-specific category and task-link references are Microsoft's [category assignments](https://learn.microsoft.com/en-us/previous-versions/windows/desktop/legacy/cc144183(v=vs.85)) and [searchable task links](https://learn.microsoft.com/en-us/windows/win32/shell/creating-searchable-task-links). The later [Control Panels UX guide](https://learn.microsoft.com/en-us/windows/win32/uxguide/winenv-ctrl-panels) informs the shared task/related-link organization; its own notice identifies it as a Windows 7 guide.

## Computer, Explorer and File Managers

**Computer** opens the primary shared-storage root in Nth Explorer and shows actual internal-storage usage, favorite file shortcuts, linked folders and links to System properties/Control Panel. **Files** opens a native Explorer-style hub with Back/Forward/Up navigation, clickable address breadcrumbs, a scrollable navigation pane shown by default on phones and larger screens, search, Details/List/Tiles/Large/Medium/Small icon views, a status bar and a file-details pane. The classic File/Edit/View/Tools/Help menu row and teal Organize/Views/Send to toolbar expose real commands. Favorite Links and a toggleable Folders tree organize navigation. Folder captions and taskbar labels follow the current folder.

- With optional device access, **Internal storage** opens the primary shared-storage root; Documents, Pictures, Music, Videos and Downloads open its actual standard folders in the same Nth window. Downloads maps to Android's `Download` directory; Videos maps to `Movies`. A missing standard directory offers an explicit Create action. Existing custom favorite mappings are preserved; hold a favorite and choose **Use device …** to reset it.
- On Android 11+, device browsing uses the **All files access** special setting for Nth. Android 8–10 uses runtime read/write storage grants, with legacy shared access on Android 10. Denial/revocation shows an in-launcher recovery page and leaves individual SAF folder links usable. Permission grants are never imported from a backup.
- The first setup dialog appears after the startup animation (or at the first visible desktop if startup is off/already seen). Later keeps the launcher usable; Choose folders opens Android's folder-access picker. The dialog survives activity recreation and stays dismissed after a choice. Reopen it through **Control Panel → File access** or Welcome Center. Existing installs get it once for this feature.
- The device root is shared user storage, not Linux `/`, operating-system directories or other apps' private data. `Android/data`, `Android/obb` and other protected Android subtrees are blocked; `Android/media` remains shared. Storage root and the Android directory cannot be renamed/deleted. Symbolic links and parent-path escapes are rejected. SD/USB/cloud locations can still be connected with Android's folder picker; direct device browsing currently covers primary shared storage.
- Device files and linked provider files use the same stream verification and capability-aware operations. Open files use installed Android viewers. APK files open Android’s package installer, which asks you to allow Nth as an installation source when needed and requires confirmation for every install. Generic provider MIME types are resolved from filenames; Open and Open with use the same read-only sharing path. Unsupported types show an explanation. A visible Done selecting control restores normal opening even when no items remain selected. The bridge is protected with Android's `MANAGE_DOCUMENTS` permission and is not advertised as a system-picker root. Only explicit selected-file read grants reach other apps; root/prefix/write/persistable grants are not returned.
- **Modify system settings**, accessibility-service access, contacts and phone permissions are not requested for file management. Optional Usage Access is a separate taskbar feature described below. Google Play separately reviews `MANAGE_EXTERNAL_STORAGE`; see [Android's guidance](https://developer.android.com/training/data-storage/manage-all-files) and [Play's permitted-use/declaration policy](https://support.google.com/googleplay/android-developer/answer/10467955). Store approval is not implied by the working installer.
- **Add a folder…** connects a user-selected folder. Android grants only the access you approve. Existing read-only links stay read-only; **Enable changes for this folder…** reopens Android's access picker to request editing. Android may restrict some roots and app-private locations.
- Click path segments, expand the folder tree, hold Back/Forward for history, or edit a friendly path inside a linked root. Search checks names in the current directory. Click Details column headings to sort by name, modified date, type or size, with folders first. Group by name, modified date, type or size, or choose no grouping. Per-folder view, grouping and sort choices are remembered for up to 64 folders. Tools → Save Search remembers up to 16 named folder searches; Searches reopens their original folder/query. Recently Changed filters the current folder to the last 30 days; it is not a device-wide index. Tools → Folder Options controls hidden files. Listings show at most 2,000 items and explicitly label that limit.
- Hold or right-click an item for Open/Open with, Cut, Copy, Paste, Rename, Delete, selection and properties. Blank space offers View, Sort, Group, Refresh, Paste, New, selection and folder properties. New Text Document creates a real UTF-8 file from the supplied name and text. Send to shares explicitly selected files through Android, with temporary read access only. Keyboard shortcuts support Ctrl+A/C/X/V/Z/F, F5 and Alt+Left/Right while preserving text-field editing. Editing actions depend on the provider's capabilities and actual write grant.
- Copy verifies bytes and a SHA-256 digest by reopening the destination. Cut removes originals only after all selected copies verify and the source tree is checked again. Name collisions keep both files. Transfers support up to 5,000 source entries and 32 levels; destination name queries cap at 10,000 immediate entries. Providers must support creating and reading copied files.
- Delete requires confirmation and permanently deletes real provider files; there is no Recycle Bin. **Edit → Undo Copy** can remove unchanged files created by the last successful copy in that Explorer window, after confirmation. It preserves originals and preexisting destination files and refuses edited copies. This in-memory receipt does not undo moves/deletes and is lost when the window closes or the process ends. Cancel, close or activity recreation cancels a transfer. Failed/cancelled copies attempt to remove newly created destination entries; a provider that refuses deletion can leave partial copies. Check the result message before retrying a move. Operations do not continue as a background service.
- The clipboard is local to the running launcher, supports either files or shortcuts, and is excluded from backups. Shortcut deletion removes a launcher link, never uninstalls the app. App folders do not contain other app folders.
- Link up to 16 folders. Removing a link releases its retained grants; it does not delete that folder. Revoked/unavailable links provide a recovery action. File Managers continues to group selected installed apps through Computer.
- Explorer navigation, sort/view mode, expanded folders and bounded selection state survive activity recreation. Active transfers are cancelled on recreation.
- Compatible apps can choose **Nth Explorer** through Android's `ACTION_GET_CONTENT`, or explicitly use our picker contract. It returns temporary read access only to chosen files. Android's `OPEN_DOCUMENT`, `CREATE_DOCUMENT` and `OPEN_DOCUMENT_TREE` screens remain the system permission/save pickers. This cannot replace an arbitrary app's private file browser. See [the integration contract](EXPLORER_INTEGRATION.md).

## Desktop gadgets

Open **Start → Gadgets**, or hold blank desktop space and choose **Gadgets**. Add or remove gadgets in the gallery. Hold a gadget and drag it onto the desktop to detach it, or drop it on the Sidebar to dock it again. Drop onto another docked gadget to place it before that gadget. Use **⋮** for size presets, docking and removal; drag the **◢** corner grip to resize a floating gadget. Tap its face to use or configure it. The gallery includes search, categories, per-gadget details/settings, optional grid snapping and a desktop layout lock. The six original gadget types remain compatible with saved layouts and notes. Each type can be enabled once; World Clock contains two independently chosen zones.

| Gadget | Behavior |
| --- | --- |
| Clock | Live local time, ivory/black faces, optional second hand |
| Calendar | Today's date on a tear-off page; tap for the native month grid |
| Weather | Fixed city, one-time approximate location or foreground Follow my location; °C/°F and cached MET Norway forecasts |
| Notes | Yellow sticky note, saved automatically and independently of Notepad |
| Photo Frame | Choose up to 12 images with Android's document picker; slideshow while the launcher is visible |
| System Meter | Actual battery level and physical memory usage on two analog dials |
| World Clock | Two independently selected Android time zones with local date/time |
| Feed Headlines | User-selected HTTPS RSS/Atom feed; cached titles and working article links |
| Checklist | Add, check, delete and clear completed saved tasks |
| Countdown | Start, pause, resume and reset a persistent countdown |
| Stopwatch | Real elapsed time, pause/reset and up to 20 saved laps |
| Calculator | Parentheses, arithmetic, unary signs, decimals/exponents and postfix percent |
| Unit Converter | Length, mass, temperature, speed, area, volume, data and time |
| Storage Meter | Actual available/used/total internal storage and Android storage settings |
| Network Meter | Measured receive/send byte rates, 32-sample receive graph and interface totals since device boot |
| Compass | Live magnetic north from Android rotation-vector or accelerometer/magnetometer sensors, with screen-rotation correction and accuracy status |
| Tile Puzzle | Solvable fifteen-tile puzzle, legal adjacent moves, move count, New puzzle and saved board |
| Days Until | Saved event name/date and local calendar-day countdown |
| Dice Roller | One to six dice, supported side counts, saved rolls and total |
| Color Studio | RGB sliders, hex input and explicit clipboard copy |
| Text Counter | Saved text, Unicode character/word counts and reading estimate |

Each type has its own original native Canvas design: analog clock, tear-off calendar, amber weather card, folded sticky note, wooden photo frame, twin gauges, travel clocks, newspaper, clipboard, brass hourglass, metal chronograph, pocket calculator, ruler/conversion plate, exposed hard drive, network instrument, brass compass and colored wooden tiles. The faces remain sharp when resized. Original six gadget designs and saved contents are preserved.

Network rates are sampled only while Nth is visible and the gadget or tool is enabled/visible. They are aggregate interface byte counters, not carrier billing totals or an Internet speed test; counters may include local traffic and reset at reboot. An unavailable counter is shown as unavailable. Compass sensor subscriptions stop when Nth leaves the foreground; sensor availability/accuracy depend on the phone. It shows magnetic north, needs no location permission and is intended to be held flat. Its status provides calibration guidance. Tile Puzzle shuffles only through legal moves, saves automatically, and rejects impossible imported boards. The gallery has a Games category.

The initial Sidebar includes Clock, Calendar and Weather. Weather shows a setup prompt until you choose a location; it never displays a sample forecast as real weather. Gadgets stop ticking while the launcher is in the background. Weather and feed requests honor cache expiry. They run while a corresponding face or its settings/reader window is visible, and stop initiating requests when Nth is in the background. Floating gadgets continue updating while the launcher is visible, even if the Sidebar is hidden.

### Weather and following your location

Open Weather settings. **Follow my location** is optional and off by default. It uses permitted approximate fixes from fused/network providers, accepts recent usable cached fixes, and requests new updates while Nth is visible. Returning Home refreshes the area. It requests no background location permission, runs no location foreground service and does not track while another app is in front. Provider updates are limited to five-minute/three-kilometre requests; a forecast is changed only for a material area change. Choosing a fixed city turns following off. Denial and disabled providers have clear recovery text and a Device location settings button. If no fresh fix arrives, existing location/forecast data remain in place.

Manual city search uses Photon/OSM with request timeouts, cancellation, stale-result rejection and a small in-memory query cache. It has a dedicated worker, so a slow geocoder cannot block app discovery or backups. Forecast requests still use MET Norway and round coordinates to three decimals. A new area needs a network connection; saved forecasts remain available offline for their area. Availability and forecast freshness depend on the providers.

### Gadget data and timing

New gadget contents and settings are included in desktop backups. The original sticky note still uses its original saved text. Checklist entries are limited to 50 and stopwatch laps to 20. Running timers use elapsed device time within a boot and wall time after a reboot; portable backups discard device-specific uptime anchors. Countdown completion appears when the gadget or tool is visible; it does not create an Android alarm or background notification. Stopwatch and countdown values are recovered when Nth resumes. Removed gadgets keep their saved content for when they are added again.

Feed Headlines accepts HTTPS RSS/Atom feeds and stores up to 12 article titles/links, with a minimum 15-minute cache. It supports relative Atom links, rejects unsafe URL schemes and XML document declarations, and keeps cached headlines on a fetch failure. It does not download full articles. The optional NASA button fills its public feed address; saving the address adds the configured gadget. Feed and forecast caches are excluded from portable backups. Existing Windows `.gadget` archives are not installed; this gallery contains original native Android implementations.

Reference galleries: [WinCustomize Sidebar Gadgets](https://www.wincustomize.com/explore/sidebar_gadgets) and [Notebooks’ gadget-gallery walkthrough](https://notebooks.com/2010/11/29/how-to-find-and-add-windows-gadgets/). We used their categories and gallery behavior as references; no third-party gadget code or artwork is bundled.

## Landscape and saved layouts

Enable Android auto-rotate and rotate your device. Landscape uses a matching wide lossless WebP wallpaper, reflows desktop icons and keeps Start navigation scrollable. Portrait and landscape each remember gadget positions, sizes, dock order, Sidebar visibility and built-in window geometry. The first arrangement for a new orientation starts from your current one; changes afterward are independent. Enabled gadgets, notes, photos and preferences are shared.

Open tool windows, their stacking/minimized state, editor cursors, calendar selection, scroll positions and an open Start search survive Android activity recreation, including rotation. Note contents also persist on the device. Saved arrangements survive app restarts; open tool sessions are restored through Android's saved activity state, rather than reopening after a force-stop or explicit fresh launch. Gadgets fit temporarily smaller viewports, including the keyboard, without overwriting their saved position or requested size.

## Start menu and system tray

The Start menu uses a white program pane, charcoal glass navigation pane, profile picture/name, pinned-program separator, All Programs folders and search. Accessories and Games expose working Nth tools. Hold an app to Pin/Unpin to Start Menu; pins are saved and included in backups. Documents/Pictures/Music open the built-in Explorer. Games, Recent Items, Computer, Network, Connect To, Control Panel, Default Programs, Help and Support, and Gadgets route to actual Nth tools or Android settings. Recent Items contains up to 20 documents explicitly opened in Nth; it grants no new file access and can be cleared. Search Everywhere opens the same scoped results in Explorer; Search the Internet opens Nth Internet Explorer. The lower controls show the desktop, lock the layout, or offer relevant settings.

Apps opened from Nth receive icon-and-label buttons in its taskbar. Press Android Home to return to Nth, then tap the desired button to switch back. Android controls task restoration; a button does not prove the app is still running. Hold a button to switch, remove it, or open Taskbar Properties. Removal does not close the app. Buttons last for the current launcher process and are bounded to 32; they are not backed up. This taskbar is inside Nth, not an overlay on other apps.

**Control Panel → Taskbar and Start Menu → Include apps opened outside Nth Desktop** is off by default. The checkbox opens Seven’s Usage Access switch where Android supports the direct link. If Android blocks access, **Open App info** leads to its **Allow restricted settings** menu. Press Back to continue to the Usage Access switch, then return to Seven to finish enabling the checkbox. With that separate permission, Nth reads recent foreground events when returning to the desktop to add launchable app buttons. It uses no background service and saves no usage history. Turning it off or revoking permission removes observation-only buttons; directly opened app buttons remain. This device-specific opt-in is excluded from backups.

Click the Start profile picture, or open **Control Panel → User Accounts → Change your desktop name**, to choose a picture. Nth copies a centered 256px WebP portrait into its private settings. That small copy is included in backups and does not depend on keeping the original image accessible. “Use the Nth picture” restores the original orb.

Start search finds installed apps (including renamed shortcuts), the same working settings catalog used by Control Panel, the saved Notepad and sticky-note text, and names of files you explicitly add through **Computer → Add a file to Start search**. It does not scan storage or document contents. Hold a linked file in Computer to remove it from search. File bookmarks are included in backups, but read grants must be authorized again on another device. A moved/deleted file may need to be chosen again.

Tap the tray arrow to show or hide battery, network, volume, brightness and Task Manager together. Hold the arrow to open Device controls directly. The clock stays visible. The taskbar's battery, network, volume and clock controls open compact Vista-style flyouts. Battery shows Android's level, charging state and Battery Saver. Network reports the default transport and verified/local/offline status without requesting Wi-Fi names or location. Volume changes the real **media** stream and provides mute/restore; fixed-volume outputs direct the user to Sound settings. Calendar browses months and links to the full Calendar tool and Android date/time settings. Flyouts close on outside tap, Back, Home, leaving the launcher or rotation. Subscriptions run only while the launcher is in front.

## Aero appearance and motion

Open **Personalization → Window Color and Appearance**. Choose Frost, Graphite, Seafoam, Sage, Lavender, Gold, Ruby or Clear, then adjust color intensity and glass transparency. When **Enable glass transparency** is checked, **Glass opacity** appears: 0% removes the glass surface, 100% makes it opaque. It updates all Nth glass frames, Start, taskbar, sidebar and tray frames live; text, buttons and content panes remain readable. Turning transparency off hides the slider and uses opaque glass, while remembering your percentage. Restore default glass resets opacity to 85%. These controls affect the launcher’s glass independently of its wallpaper. Opaque mode avoids wallpaper sampling. The softened background is the wallpaper; it is not a capture of another Android app.

Native Nth windows have a soft caption glow and distinct active/inactive frames. Touch a window to activate it. Minimize shrinks toward the taskbar; its taskbar button restores it with the saved window geometry and note contents intact. Buttons, checks, radio choices, scrollbars, popup surfaces and launcher-owned dialogs use matching Vista-era treatments. Android file pickers, permission screens and other system settings keep their native interface.

## Startup and Welcome Center

Open **Personalization → Startup and Sounds**. The default is **First launch only**, with the original chime **off**. Other choices are **Once per phone reboot** and **Off**; **Preview startup** works without changing automatic-playback identity. The introduction lasts about three seconds: a restrained green loading sweep, the original glowing Nth pearl, then Welcome. **Skip** and Android Back dismiss it immediately.

Startup runs inside the home activity when it is visible and unlocked. It does not replace Android’s boot animation. Ordinary Home presses, returning from another app, Activity rotation and process recreation in the same boot do not replay it. Reboot identity uses Android’s boot counter and separate device-local state, which is excluded from desktop backups. If the boot counter is unavailable, automatic playback falls back to the initial introduction. The first introduction is also shown when updating an earlier Nth Desktop release that has never presented it.

The chime is generated from original overlapping tones, with no downloaded Windows audio. It respects silent/vibrate mode, system sound volume and Do Not Disturb, and stays quiet while music is playing. Skip/backgrounding stops it. **Reduce motion** and Android’s animation setting disable window transitions and use a short, silent still welcome. A backgrounded introduction is dismissed, rather than starting over when you return. Android 12+ keeps its standard system splash before app content; there is no separate splash activity.

**Welcome Center** opens after the first introduction and remains available from Start and Control Panel. Its working links lead to Personalization, app folders, gadgets, backup and default-home selection when needed. An optional checkbox opens it at later Nth Desktop starts; it is not a screen-by-screen tour.

## Launcher behavior

- Native HOME activity and standard optional default-home chooser.
- Start search and All Programs list real launchable apps in the current Android profile. Hold an app for desktop pin/unpin, naming, Quick Launch, folder placement or Android app info.
- Vertical desktop icon columns. Hold and drag a shortcut onto the center of another to group them, or near its edge to swap slots; their order is saved.
- Taskbar Quick Launch, buttons for built-in windows, and session buttons for Android apps opened from Nth. Active native window buttons minimize; minimized or covered windows restore/raise.
- Drag, minimize, maximize/restore and close built-in tool windows. Android apps open normally under Android's window management.
- Notepad saves automatically on the device. Files and Music use Nth Explorer; file types open in Gallery or appropriate installed Android handlers.
- The right edge of the taskbar shows the desktop. Back dismisses Start or minimizes the top built-in tool.
- Three wallpaper/color treatments. The aurora artwork uses lossless WebP; see [artwork and prompt details](ASSETS.md).
- Android's status bar is hidden by default for the desktop look; swipe from the top to reveal system controls, or enable it in Personalization. Navigation and cutout insets remain respected.

These are native built-in desktop gadgets. This release does not yet host third-party Android home-screen widgets, work-profile apps, or move other Android apps into its built-in windows.

## Install and update

The supplied Seven 0.6.0 test APK uses the original `com.nthexperience.desktop.seven` identity and the recovered Seven-specific test key, matching the original builds through 0.5.2. Open **Nth Desktop Seven**, then choose it under **Android Settings → Apps → Default apps → Home app** when ready.

The later 0.5.3–0.5.11 Preview builds used `com.nthexperience.desktop.seven.preview` and a different key that is no longer available in this workspace. Version 0.5.12 cannot update that Preview install in place. Before switching, export its desktop backup through Control Panel → Backup and Restore, install 0.5.12, and restore the backup there. Keep Preview installed until the restored setup is verified. Android permissions and file access must be granted again; Recycle Bin contents and browser data are not included in desktop backups.

Notes, selected photo references, linked folder references, file-manager shortcuts, preferences and cached forecasts are stored locally. Photo access uses the document picker's retained grants; device-wide shared-file browsing is an optional separate all-files grant. Approximate location is requested when you tap **Use my approximate location** or enable **Follow my location**. A city can be chosen without granting location access. Forecasts require Internet access; city search uses Photon/OSM. City search text is sent to that service. Selected weather coordinates are sent to MET Norway. Uninstalling clears local app data.

GitHub Actions uses its runner's debug key. Its APK artifact may not have the same signing identity as the handoff APK. Reuse the private test signing key for in-place hand-built test updates, and never commit signing keys or use the debug key for a production release.

## Build and checks

JDK 17, Android SDK platform 36, Build Tools 35.0.0, Gradle 8.13 and AGP 8.13.2. Minimum Android 8.0 (API 26), target API 36. AndroidX WebKit 1.13.0 supplies compatibility APIs; the independently updated Android WebView supplies the browser engine.

```sh
./gradlew assembleDebug assembleDebugAndroidTest
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
mkdir -p build/state-check
javac -d build/state-check app/src/main/java/com/nthexperience/desktop/LauncherState.java checks/LauncherStateCheck.java app/src/main/java/com/nthexperience/desktop/DesktopGeometry.java checks/DesktopGeometryCheck.java app/src/main/java/com/nthexperience/desktop/StartupPolicy.java checks/StartupPolicyCheck.java
java -cp build/state-check LauncherStateCheck
java -cp build/state-check DesktopGeometryCheck
java -cp build/state-check StartupPolicyCheck
```

On a disposable emulator:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.AuthenticitySmoke
adb shell am instrument -w -e suite storage com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
adb shell am instrument -w -e suite legacy com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
adb shell am instrument -w -e suite aero com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
adb shell am instrument -w -e suite gadgets com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
```

The device-storage suite verifies first-run setup after intro, Later/recreation, actual all-files grants and revocation, all standard directories opening inside Explorer, native filesystem operations, protected/root guards and a separate app receiving only its chosen native file. It also checks denial of write/rename/persistable/root-listing/unselected-file access. The Explorer suite uses a separate fixture APK/UID to test provider grants, verified copy/cut, keep-both naming, creation/rename/permanent-delete confirmation, read-only actions, failed and cancelled copies, self-descendant rejection, navigation, shortcut menus and taskbar cleanup. A separate caller UID verifies single/multiple picker results, MIME filtering, recreation, cancellation and denial of write/unselected-file access. Test and fixture APKs are not part of the delivered launcher. The dependency-free checks exercise boundary fitting, normalized positions, grid snapping and corrupt geometry. The instrumentation runner exercises real touch drag, docking, corner resizing, layout lock, independent orientation layouts, rotation and explicit activity recreation with note/cursor and Start query continuity. An isolated test folder in the disposable emulator’s Documents directory exercises Android’s real folder picker and storage provider, retained read grants, tree-boundary enforcement, file reads, nested navigation/search and rotation restoration. The test cleans up its own fixture folder; fixture data are absent from the production APK. The runner also checks upgrade cleanup, before/after default-home prompts using the real Android HOME role, the desktop context menu, and taskbar/dock bounds in both landscape rotations with a real emulated cutout and three-button navigation. It also checks Control Panel search/Classic view, actual settings changes and dispatch to Android Wi-Fi settings. It also checks app discovery, Start search/All Programs, note persistence, window controls, wallpaper settings, gadget selection, sticky notes, clock settings, calendar, weather parsing/unit conversion and configuration. It captures real desktop, menu, window and gadget-gallery screens. The established suite does not require live weather. The new gadget suite grants approximate/mock location only in disposable emulators to exercise real Android provider callbacks; those test grants and fixtures are not bundled into the production APK. The runner also groups shortcuts using real touch input, renames/reorders/dissolves a folder, checks folder continuity through rotation and edits Quick Launch. It saves and opens an actual backup through Android’s document picker, verifies the preview leaves live settings unchanged, restores both orientation layouts and Unicode notes, undoes the restore and rejects malformed imports. GitHub Actions runs the full established suite on Android 11. A focused Aero/startup suite also runs on Android 16, with real emulator reboots on both versions. It checks independent color controls, active/inactive windows, live minimize/restore, old backup compatibility, first-run skip/Welcome Center, ordinary Home suppression, preview cancellation during rotation, reduced motion and once-per-reboot identity. It also exercises opacity visibility, persistence, backup compatibility and exact 0/50/100% rendered pixels for every glass style; grouped Start search and destinations; profile validation; and native tray/calendar behavior with actual media volume changes. Android 11 chooses a real portrait and linked file through the document picker and checks retained read permission. It records an actual startup preview. The focused gadget suite adds real caption-icon rendering comparisons, seventeen-gadget enablement and backup validation, calculator/converter/checklist actions, measured stopwatch/countdown behavior, world zones, storage, RSS/Atom and city-response parsing, activity recreation and Android fused test-provider follow/pause/resume callbacks. The added checks exercise legal puzzle moves and solvability, network delta/reset handling, magnetic-bearing screen transforms, hardware lifecycle, and fixed icon columns in both landscape directions. The full Android 11 SystemUI image covers real status bars, notification-shade return and both three-button/gesture navigation. The Android 16 ATD image has no SystemUI; it covers rotations, layout bounds and gadget behavior. Physical-device acceptance testing is separate.

## Origin and credits

The original idea was prompted by [jovanovski/windowslauncher](https://github.com/jovanovski/windowslauncher). At inspection it had no declared source license. No source code or proprietary Windows graphics from that project were copied. The application, icons and wallpaper were independently created. This is not a Microsoft product.

The openly licensed Selawik font is copyright Microsoft Corporation and is included under SIL OFL 1.1. Weather data are provided by MET Norway under CC BY 4.0. Gradle's wrapper is build tooling under its own license. Full notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Licensing of Nth Experience's application code remains for the owner to decide; no open-source license is selected by this setup.

## Desktop organization and backup

Drag a shortcut onto the center of another to create a folder, or near its edge to swap positions. Use Desktop Organization to rename, reorder, add or remove shortcuts; dissolving a folder returns its contents to the desktop. Quick Launch can hold up to 12 chosen shortcuts, including folders. The desktop context menu includes New folder, icon sizes and sorting.

Control Panel → Backup and Restore exports desktop settings, app folders, names, Quick Launch, notes and both orientation layouts to a user-chosen JSON file. Opening a backup displays a preview; applying it keeps the previous setup for Undo last restore. Backups contain references to linked folders and photos, not their contents or portable access grants. Restore on another device may require relinking those locations. Unknown/newer backup versions, malformed data, nested folders, invalid settings and excessively large files are rejected before changing live settings.

## Nth Internet Explorer

The desktop **Nth Internet Explorer** icon opens the browser in a movable, maximizable Aero window. Original drawn controls recall IE9: a large blue Back button, combined address/search field, glass tabs, Home, Favorites and Tools. Modern pages use the installed Chromium-based Android WebView; this app does not bundle an obsolete IE engine or a full Chromium distribution. Keep the device's WebView provider updated. Tools → About shows the actual engine version.

- Eight tabs, actual Back/Forward/reload, Bing/Google/DuckDuckGo address-bar search, Favorites, bounded history, Find, text size, desktop site, share/copy, print/save PDF and full-screen video over the desktop. Entering video fullscreen requests landscape; exiting restores the previous orientation and system bars.
- Normal tab addresses recover after Activity recreation and reopening Internet. Cookies and website storage persist normally. InPrivate requires both WebView multi-profile isolation and complete browsing-data deletion; it is unavailable on older providers. Private tabs are never saved, remain live during handled orientation changes, close when Internet closes or the Activity is destroyed, and use a fresh isolated profile. Downloads and explicitly saved Favorites remain.
- Website uploads use Nth Explorer with MIME filters and multiple selection. Camera, microphone and approximate location require a site prompt and Android permission. Local file/content URLs, arbitrary intent URLs, JavaScript address-bar URLs, mixed content and invalid TLS certificates are blocked. No native JavaScript bridge is exposed. First-party cookies work; third-party cookies are blocked.
- Downloads use Android DownloadManager, visible in Tools → Downloads. Confirm before saving. API 29+ saves to public Downloads; API 26–28 uses the app's external downloads directory (removed on uninstall). Completed files use the same read-only file handoff as Explorer, including Android's APK installer. DownloadManager does not receive browsing cookies, so sign-in protected and blob/data downloads should use Tools → Open in another browser. The app does not claim Chrome sync, extensions, a password manager, identical DRM support or complete Chrome API parity.
- HTTP websites remain supported and are labeled **Not encrypted**. Browser data is separate from desktop backup settings. Tools → Internet options clears history or website data; older providers explicitly disclose incomplete data-clearing support.

The emulator browser suite uses only test-APK HTTP fixtures and verifies real WebView navigation/JavaScript/cookies/storage, tabs, Favorites/history, Find, desktop UA, unsafe URL rejection, actual downloaded bytes and content URI reads, Explorer upload picker cancellation, conditional private-profile separation, live document/JavaScript/video continuity across rotation, landscape video fullscreen and exit-state restoration, and normal tab recovery after Activity recreation. Test fixtures are not included in the app APK.

## Photo Gallery and shared Vista controls

Picture taps in Nth Explorer, linked files and Internet downloads now open in a Photo Gallery-style desktop window. Open with remains an explicit external-app choice. Pictures in Start opens the built-in Explorer Pictures folder; Photo Gallery is searchable in Start and available in desktop/Quick Launch organization and Desktop Icon Settings.

The viewer supports previous/next in the current folder's filtered/sorted picture order, fit, zoom buttons, pinch zoom/pan, double-tap zoom, EXIF orientation, view-only quarter-turn rotation, a four-second slideshow, properties and read-only sharing. Back to folder restores the originating Explorer window. A single-file opening does not infer permission to enumerate neighboring pictures. Rotation, zoom and selected picture survive Activity recreation/portrait/landscape. Slideshows stop when minimized, backgrounded or closed. Read access is checked again on resume; revocation removes the displayed bitmap and shows recovery guidance.

Decoding runs on a cancellable worker, bounds decoded dimensions/pixel counts, rejects corrupt/unsupported data gracefully and ignores stale results after a new picture or closed window. PNG/JPEG/WebP are exercised by the emulator suite. Other image formats depend on the platform decoder; animated files show the first frame. Source files are never edited by view rotation or zoom. The current scope is a still-picture viewer, with Media Player and Recycle Bin left for later releases.

All built-in message/list dialogs share Vista captions, fonts, buttons and selection styling while retaining Android cancellation, selection and permission semantics. Internet Tools and Favorites now use the same pointer/touch/keyboard menu surface as Explorer. System-owned Android permission/installer screens keep their native behavior.

The PhotoSmoke instrumentation suite uses actual native and granted SAF images, drawn-pixel assertions, EXIF-tagged JPEGs, a large image, corrupt data, physical toolbar/taskbar taps, timed slideshow behavior, source-byte hashes, Activity recreation, both orientations and actual URI revocation. FileOpenSmoke checks picture taps in Gallery and still verifies external read-only byte delivery through Open with. Test-only fixtures never ship in the app APK.

## Vista desktop continuity (0.16)

Hold the Sidebar header, its navigation pill, or blank Sidebar space for a compact menu: Bring Gadgets to Front, Add Gadgets, Properties, Help and Close Sidebar. Closing hides the dock and keeps gadget data. Gadget Gallery now keeps Show Sidebar in its toolbar. Sidebar Properties can keep the dock above windows; windows reserve its width so their captions remain usable.

Original perspective artwork gives Computer, Network, folders, Internet, Notepad, Control Panel, drives and file types a closer Vista-era appearance. Start has a darker navigation pane, pinned programs, Accessories and Games folders, user-folder navigation, Recent Items, network/default-program destinations, clearable search, Search Everywhere and Search the Internet. Hold an app for Pin to Start Menu. The bottom controls show the desktop, lock its layout and open more options; phone power remains under Android's control.

Apps launched from Nth get session taskbar buttons alongside built-in windows. These buttons return to Android's existing app task where available. Optional Usage Access, enabled in Taskbar and Start Menu, adds externally opened launchable apps when returning home. This is a session switcher, not an assertion that Android still has every task running. Remove taskbar button dismisses the button without terminating another app. No accessibility service or background tracking service is used.

Nth Explorer adds the File/Edit/View/Tools/Help row, a glass command bar, Favorite Links and expandable folders, sortable Name/Date modified/Type/Size columns, real grouping, Details/List/Tiles and icon views, and a bottom information pane. Folder presentation is remembered per location. Folders toggles the navigation pane on phones. Recently Changed filters the current folder to the last 30 days; saved searches retain their original folder and text. New creates folders or UTF-8 text documents with supplied contents. Send to shares only selected files with read-only URI grants. Undo Copy removes only newly copied destinations after checking their contents and keeps originals, existing conflicts and edited copies. Keyboard shortcuts include Ctrl+A/C/X/V/Z/F, F5 and Alt+Left/Right; text fields retain their normal editing shortcuts. Optical-disc burning and Windows .lnk files are not Android file operations.
