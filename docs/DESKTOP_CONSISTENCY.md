# Desktop consistency fixes on Seven 0.7.1

The initial test build used `main`, which still contained Seven 0.4.1. The user's existing 0.7.1 desktop is saved on `feature/seven-window-resize` at `e796b0ff3699621a49ceaee364ac0f430ac07fcc`. This corrected patch starts from that existing version. Its normal package name, version code, Start menu, taskbar layout, wallpapers, icon artwork and window styling are preserved.

## Requested changes

- Retains the existing classic Task Manager and adds its Services/Users navigation without replacing the window or taskbar integration.
- Adds an atomic, rebuildable app-name/icon cache. Cached inventories appear before worker refresh; package/locale/manual refresh invalidates the short memory cache. Failed scans retain a working inventory, and a first-scan failure leaves existing desktop state in place.
- Changes Welcome Center to **Restore a desktop** and places restoring first in Backup and Restore.
- Imports compatible backup settings, filters unsupported shortcuts and backgrounds, lists skipped items, preserves destination values for skipped settings, and retains restore preview and undo. New backup metadata identifies the variant and stock background names; older files and the historical Eight format remain supported.

The 0.7.1 backup schema is kept, including expanded taskbar, icon/grid, folder and window preferences. Installing or opening the launcher does not apply a desktop backup. File permission grants and the contents of linked files are not transferred by desktop backups.

## Validation

`python checks/CheckSevenBaseline.py` verifies the unchanged identity, 126 existing runtime files and eight existing shell/layout methods against the 0.7.1 baseline. In addition, the unit suite checks five source-family fixtures, unsupported items, background names, corruption rejection, restore/undo, existing expanded-taskbar preferences and saved themes, cached icons, cold scan failure and Task Manager navigation.

Build and test with Java 17 / Android SDK 36:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

The instrumented test APK is compiled. Android device/emulator instrumentation has not been run in this workspace. Test APKs use this workspace's development signing certificate; updating an older handoff APK requires its matching private signing key. No pull request is merged without the user's individual approval.
