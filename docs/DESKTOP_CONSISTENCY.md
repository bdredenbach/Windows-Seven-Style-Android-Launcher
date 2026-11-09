# Desktop consistency fixes on Seven 0.7.1

The initial test build used `main`, which still contained Seven 0.4.1. The user's existing 0.7.1 desktop is saved on `feature/seven-window-resize` at `e796b0ff3699621a49ceaee364ac0f430ac07fcc`. This corrected patch starts from that existing version. Its normal package name, version code, Start menu, wallpapers, icon artwork and window styling are preserved. The requested taskbar label fit and consistent gadget visibility are applied to that established desktop.

## Requested changes

- Retains the existing classic Task Manager and adds its Services/Users navigation without replacing the window or taskbar integration.
- Adds an atomic, rebuildable app-name/icon cache. Cached inventories appear before worker refresh; package/locale/manual refresh invalidates the short memory cache. Failed scans retain a working inventory, and a first-scan failure leaves existing desktop state in place.
- Changes Welcome Center to **Restore a desktop** and places restoring first in Backup and Restore.
- Imports compatible backup settings, filters unsupported shortcuts and backgrounds, lists skipped items, preserves destination values for skipped settings, and retains restore preview and undo. New backup metadata identifies the variant and stock background names; older files and the historical Eight format remain supported.

The 0.7.1 backup schema is kept, including expanded taskbar, icon/grid, folder and window preferences. Installing or opening the launcher does not apply a desktop backup. File permission grants and the contents of linked files are not transferred by desktop backups.

## Validation

`python checks/CheckSevenBaseline.py` verifies the unchanged identity, 114 existing runtime files and seven existing shell/layout methods, plus explicitly pinned requested rendering/gadget changes and unchanged original icon artwork against the 0.7.1 baseline. In addition, the unit suite checks five source-family fixtures, unsupported items, background names, corruption rejection, restore/undo, existing expanded-taskbar preferences and saved themes, cached icons, cold scan failure and Task Manager navigation.

Build and test with Java 17 / Android SDK 36:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

The instrumented test APK is compiled. Android device/emulator instrumentation has not been run in this workspace. Test APKs use this workspace's development signing certificate; updating an older handoff APK requires its matching private signing key. No pull request is merged without the user's individual approval.

## Extended taskbars and rendering caches

The desktop grid uses the actual work area above the taskbar. It measures each label before fitting the icon, keeps complete rows visible, and starts overflow rows outside the first viewport so they can be scrolled into view. Expanded taskbars temporarily fit saved positions without writing those coordinates back to preferences. The normal taskbar layout returns when the taskbar collapses. Larger system text participates in row sizing.

All five variants now share static built-in artwork in a bounded 4 MiB bitmap cache keyed by icon kind, pixel size, padding, density and UI mode. Existing drawing commands remain unchanged; live gadget faces are separate. Folder names, shortcut aliases and normalized preference lists reuse parsed snapshots until their raw saved data changes. Mutable folder/list return values remain independent so edits cannot accidentally corrupt the cache. Desktop placement plans are reused until dimensions, orientation, cutouts, shortcut membership or saved positions change. An unchanged in-memory app catalog no longer recreates the desktop and hidden Start contents on every Home/resume.

`LauncherRenderingTest` checks expanded taskbars with automatic/requested rows, two-line labels, larger system text, whole overflow rows, temporary saved-position fitting, geometry invalidation, mutable model safety, static icon reuse and bounded cache size. Phone/emulator timing and visual checks remain pending; the APKs are development-signed test builds.

Seven keeps the same selected gadgets and sidebar visibility in portrait and landscape. Older conflicting visibility settings use the established portrait choice, then both orientations stay synchronized. Position/order/scale data remains separate. `GadgetRotationTest` covers conflicting old layouts, stale unselected gadget IDs, removals, visibility toggles and independent positions. The original baseline hashes remain in the check manifest; requested changes have explicit replacement hashes and reasons.

## Ordered initialization and restored folders

After the existing startup introduction, all five desktops use the same device-local setup sequence:

1. Default Launcher.
2. Enable File Browsing.
3. Enable Usage Access.
4. Allow Notifications Access (Android Notification access for showing the phone's notifications).
5. Modify System Settings (Brightness).
6. Allow Location with approximate/precise access.
7. Fix unlinked folders from a restored backup.

Granted steps are skipped. Each permission remains optional; Later/cancellation advances, and unavailable Settings activities do not block setup. Pending Android requests and progress survive recreation. An interrupted folder transaction can be retried, and a fresh launch clears an orphaned picker request. The existing Welcome/start surface follows this flow. The introduction artwork and boot-playback policy are retained. Setup progress and Android grants are excluded from desktop backups.

A successful restore/undo queues folder repair after any remaining setup steps. Folder checks and path lookup run on the worker. Reconnecting a root updates its existing Computer link, favorites and desktop folder-shortcut pages without recreating shortcut IDs, titles, positions or unrelated preferences. Descendants are verified by document IDs or resolved by unique folder names. Missing/ambiguous subfolders are kept, and partial repairs can be retried with **Fix linked folders** in Backup and Restore. A plan is applied only by its live setup flow and only if the referenced saved data has not changed.

Links to a known Nth storage provider can translate to this desktop's own provider when its file permission is already granted; this verifies the destination path and does not transfer permission from another app. Other folders use Android's folder picker and retain its read grant. The setup cannot grant Android permissions itself.

Eight, Ten and Eleven use their existing notification-listener components. Vista and Seven gain the same in-memory listener support, protected by Android's notification-listener binding permission, and phone notifications on their classic tray surfaces. In Vista, Notification Center is available from Device controls (hold the tray chevron); Seven also shows phone notifications in its existing Action Center. Notification content remains in memory and respects the existing locked-phone filtering.

`InitializationFlowTest` exercises the ordered dialogs, granted-step skipping, Settings intents, denial/cancellation, focus/recreation, interrupted/orphaned requests, unavailable Settings, notification-service declarations and last-step folder repair. `FolderRepairTest` exercises root/favorite/shortcut updates, preserved identities/layouts, concurrent-edit protection, partial repairs, de-duplication, real provider grants/path lookup, and missing/ambiguous descendants. The instrumentation APK is compiled; Android device/emulator instrumentation is not run in this workspace.

## Location choice and uninstall from Start

Initialization now follows Default Launcher, File Browsing, Usage Access, Notifications Access, System Settings for Brightness, Location, then restored-folder repair. Location stays optional. Android 12 and later receive fine and coarse requests together so Android offers its native approximate/precise choice. Older Android versions receive separate approximate/precise choices in the setup dialog. Coarse-only grants are accepted, saved weather choices are kept, and background-location permission is not requested. Precise permission enables the GPS provider when available; all location work remains foreground-only.

Device-local initialization schema v2 inserts Location without replaying completed earlier steps. Existing completed or folder-repair-stage v1 sessions offer Location once and retain in-flight folder picker state; folder repair remains last. Desktop backups still do not transfer permissions or initialization progress.

Installed removable Android apps now have an explicit **Uninstall** command in their Start context menus. Vista/Seven support touch hold and mouse context clicks on program rows; Ten exposes it directly for apps and pinned tiles; Eleven exposes it directly in Start app options; Eight exposes it in its selected-app command bar and 8.1 Update tile popup, including app-row context clicks. Built-in launcher tools, folders, missing apps, this launcher and protected system apps do not offer the command. Unpinning/removing a shortcut remain separate actions.

Uninstall opens Android's package-uninstall confirmation using ACTION_DELETE, with REQUEST_DELETE_PACKAGES declared. It does not open App info/settings. Android retains its confirmation and policy checks. Canceled or blocked requests keep launcher data. Confirmed removal updates Start pins (including both Eight tile banks) and the cached app inventory; package replacements do not count as uninstalls. The worker forgets an explicitly removed app from disk/memory so removing the last cached app cannot revive it through the empty-scan fallback.

`LocationAccessTest`, the updated `InitializationFlowTest`, `AppUninstallerTest` and per-variant `StartUninstallMenuTest` cover coarse/fine declarations and requests, migration, real Start menu commands, normal app opening, native uninstall intents, cancellation, policy failures, protected/built-in filtering, both Eight banks and cold-cache removal. Real-device Android permission/uninstall dialogs still require phone/emulator verification.
