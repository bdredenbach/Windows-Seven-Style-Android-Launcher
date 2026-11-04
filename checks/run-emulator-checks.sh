#!/usr/bin/env bash
set -euo pipefail
launcher=com.nthexperience.desktop.seven
runner="${launcher}.test/com.nthexperience.desktop"
component="$launcher/com.nthexperience.desktop.MainActivity"
collect() {
  mkdir -p screenshots
  free -m > screenshots/host-memory.txt
  if ! timeout -k 2s 4s adb shell true; then return; fi
  timeout -k 2s 5s adb exec-out run-as "$launcher" cat shared_prefs/desktop.xml > screenshots/test-desktop-prefs.xml 2>/dev/null || true
  timeout -k 2s 5s adb shell dumpsys window > screenshots/test-window-state.txt 2>/dev/null || true
  timeout -k 2s 8s adb logcat -d -t 500 > screenshots/device-log.txt 2>/dev/null || true
  for name in aero-snap-left-preview aero-snap-top-preview aero-snap-failure nth-media-player nth-media-center nth-media-video nth-media-failure fidelity-failure vista-tray-expanded vista-background-picker vista-taskmanager-graphs vista-folder-windows desktop-large-fonts vista-flip-3d window-switcher window-hover window-private-preview tray-brightness tray-brightness-system explorer-thumbnails recycle-bin saved-workspace recent-items extras-failure vista-taskbar-menu vista-task-manager vista-task-performance vista-window-menu vista-windows-side vista-windows-cascade polish-failure control-panel-appearance personalization-vista control-panel-search control-panel-classic-vista control-panel-vista-landscape desktop-settings shortcut-folder-explorer vista-sidebar-menu vista-explorer-details vista-explorer-groups vista-explorer-icons vista-explorer-landscape vista-start-menu vista-app-taskbar vista-icon-sheet vista-authenticity-failure portrait-cutout-failure vista-system start-orb-menu start-hover start-music desktop-top-hidden desktop-top-shown browser-fullscreen browser-fullscreen-restored browser-rotation sidebar-five sidebar-last-page gadget-dice gadget-colors desktop-free desktop-snapped continuity-failure photo-viewer photo-landscape photo-restored photo-failure vista-dialog browser-home browser-page browser-security browser-restored browser-failure file-open-install-blocked file-open-install file-open-installed file-open-unsupported file-open-failure storage-setup storage-root storage-documents storage-device-menu storage-picker storage-revoked storage-failure desktop start-menu notepad gadget-gallery gadgets-desktop calendar floating-gadgets landscape landscape-notepad landscape-start landscape-calendar restored-portrait control-panel control-panel-classic personalization system computer computer-landscape file-managers explorer-folder desktop-clean default-home-start desktop-context-menu landscape-cutout-left landscape-cutout-right app-folder folder-portrait folder-landscape quick-launch desktop-organization backup-preview backup-restored backup-recovered failure aero-opaque aero-colors aero-windows startup-pearl welcome-center startup-settings aero-failure glass-opacity glass-start-0 glass-start-50 glass-start-100 start-search start-portrait tray-battery tray-network tray-volume tray-calendar gadget-gallery-expanded gadget-pack-desktop gadget-calculator gadget-converter gadget-checklist gadget-stopwatch gadget-countdown gadget-world-clock gadget-headlines explorer-navigation explorer-file-menu explorer-folder-menu explorer-copied-files explorer-read-only explorer-picker explorer-picker-multiple taskbar-no-recents explorer-failure weather-follow-settings gadget-network gadget-compass gadget-puzzle gadget-designs aligned-threebutton-0-status-hidden aligned-threebutton-0-status-shown aligned-threebutton-1-status-hidden aligned-threebutton-1-status-shown aligned-gestural-0-status-hidden aligned-gestural-0-status-shown aligned-gestural-1-status-hidden aligned-gestural-1-status-shown gadgets-failure; do
    [[ -s "screenshots/$name.png" ]] && continue
    timeout -k 2s 4s adb exec-out run-as "$launcher" cat "files/$name.png" > "screenshots/$name.png" 2>/dev/null || true
  done
  timeout -k 2s 4s adb exec-out run-as "$launcher" cat files/fidelity-tree.txt > screenshots/fidelity-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as "$launcher" cat files/extras-tree.txt > screenshots/extras-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as "$launcher" cat files/polish-tree.txt > screenshots/polish-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as "$launcher" cat files/vista-authenticity-tree.txt > screenshots/vista-authenticity-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as "$launcher" cat files/file-open-tree.txt > screenshots/file-open-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as "$launcher" cat files/storage-tree.txt > screenshots/storage-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as "$launcher" cat files/explorer-tree.txt > screenshots/explorer-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as "$launcher" cat files/picker-tree.txt > screenshots/picker-tree.txt 2>/dev/null || true
  timeout -k 2s 8s adb exec-out cat /sdcard/Nth-Desktop-Startup.mp4 > screenshots/startup-preview.mp4 2>/dev/null || true
}
trap collect EXIT
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if [[ -f vista-baseline/app/build/outputs/apk/debug/app-debug.apk ]]; then
  bash checks/CheckSideBySide.sh vista-baseline/app/build/outputs/apk/debug/app-debug.apk | tee side-by-side-test.txt
fi
snap_status=0
if ! timeout -k 3s 100s adb shell am instrument -w "$runner".SnapSmoke | tee snap-test.txt ||
   ! grep -q 'PASS:' snap-test.txt; then
  snap_status=1
fi
mkdir -p screenshots
for name in aero-snap-left-preview aero-snap-top-preview aero-snap-failure; do
  adb exec-out run-as "$launcher" cat "files/$name.png" > "screenshots/$name.png" 2>/dev/null || rm -f "screenshots/$name.png"
done
adb shell pm clear "$launcher"
adb shell pm list instrumentation | tee instrumentation-test.txt
grep -q 'com.nthexperience.desktop.AuthenticitySmoke' instrumentation-test.txt
timeout -k 3s 240s adb shell am instrument -w "$runner".PolishSmoke | tee polish-test.txt
grep -q 'PASS:' polish-test.txt
adb shell am force-stop "$launcher"
# Isolated fixtures only; exercise both denied and granted settings access in separate processes.
adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE allow
adb shell appops set "$launcher" WRITE_SETTINGS deny
timeout -k 3s 130s adb shell am instrument -w "$runner".MediaSmoke | tee media-test.txt
grep -q "PASS:" media-test.txt
adb shell am force-stop "$launcher"
timeout -k 3s 200s adb shell am instrument -w "$runner".FidelitySmoke | tee fidelity-test.txt
grep -q 'PASS:' fidelity-test.txt
adb shell am force-stop "$launcher"
timeout -k 3s 260s adb shell am instrument -w "$runner".DesktopExtrasSmoke | tee extras-test.txt
grep -q 'PASS:' extras-test.txt
adb shell am force-stop "$launcher"
adb shell appops set "$launcher" WRITE_SETTINGS allow
timeout -k 3s 70s adb shell am instrument -w -e phase system "$runner".DesktopExtrasSmoke | tee brightness-test.txt
grep -q 'PASS:' brightness-test.txt
adb shell am force-stop "$launcher"
adb shell appops set "$launcher" WRITE_SETTINGS deny
adb shell settings put system font_scale 1.3
timeout -k 3s 90s adb shell am instrument -w -e phase usability "$runner".DesktopExtrasSmoke | tee usability-test.txt
grep -q 'PASS:' usability-test.txt
adb shell am force-stop "$launcher"
adb shell settings put system font_scale 1.0
adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE deny
# Validate the new shell commands first; every existing suite still follows.
timeout -k 3s 330s adb shell am instrument -w "$runner".AuthenticitySmoke | tee authenticity-test.txt
grep -q 'PASS:' authenticity-test.txt
adb shell am force-stop "$launcher"
# Exercise the integrated browser before the storage suites reset launcher preferences.
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
timeout -k 3s 240s adb shell am instrument -w "$runner".BrowserSmoke | tee browser-test.txt
grep -q 'PASS:' browser-test.txt
adb shell am force-stop "$launcher"
# Gallery uses real native and granted SAF pictures before setup resets preferences.
adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE allow
timeout -k 3s 230s adb shell am instrument -w "$runner".PhotoSmoke | tee photo-test.txt
grep -q 'PASS:' photo-test.txt
adb shell am force-stop "$launcher"
timeout -k 3s 210s adb shell am instrument -w "$runner".ContinuitySmoke | tee continuity-test.txt
grep -q 'PASS:' continuity-test.txt
adb shell am force-stop "$launcher"
# Permission changes can restart an Android app; apply them outside instrumentation.
adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE deny
timeout -k 3s 90s adb shell am instrument -w -e suite storage -e phase setup "$runner".StorageSmoke | tee storage-setup-test.txt
grep -q 'PASS:' storage-setup-test.txt
adb shell am force-stop "$launcher"
adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE allow
timeout -k 3s 150s adb shell am instrument -w -e suite storage -e phase access "$runner".StorageSmoke | tee storage-access-test.txt
grep -q 'PASS:' storage-access-test.txt
adb shell mkdir -p /sdcard/Download
adb push installprobe/build/outputs/apk/debug/installprobe-debug.apk /sdcard/Download/Nth-Install-Probe.apk
adb shell am force-stop "$launcher"
adb shell appops set "$launcher" REQUEST_INSTALL_PACKAGES deny
timeout -k 3s 90s adb shell am instrument -w -e phase blocked "$runner".FileOpenSmoke | tee file-open-blocked-test.txt
grep -q 'PASS:' file-open-blocked-test.txt
adb shell am force-stop "$launcher"
adb shell appops set "$launcher" REQUEST_INSTALL_PACKAGES allow
timeout -k 3s 230s adb shell am instrument -w "$runner".FileOpenSmoke | tee file-open-test.txt
grep -q 'PASS:' file-open-test.txt
adb shell rm -f /sdcard/Download/Nth-Install-Probe.apk
adb shell appops set "$launcher" REQUEST_INSTALL_PACKAGES deny
adb shell am force-stop "$launcher"
adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE deny
timeout -k 3s 60s adb shell am instrument -w -e suite storage -e phase revoked "$runner".StorageSmoke | tee storage-test.txt
grep -q 'PASS:' storage-test.txt
adb shell am force-stop "$launcher"
timeout -k 3s 260s adb shell am instrument -w "$runner".StorageSmoke | tee explorer-test.txt
grep -q 'PASS:' explorer-test.txt
if [[ "$1" == 30 ]]; then
  timeout -k 3s 300s adb shell am instrument -w -e suite legacy "$runner".StorageSmoke | tee smoke-test.txt
  grep -q 'PASS:' smoke-test.txt
fi
adb shell pm list instrumentation
adb shell am force-stop "$launcher"
timeout -k 3s 230s adb shell am instrument -w -e suite aero "$runner".StorageSmoke | tee aero-test.txt
grep -q 'PASS:' aero-test.txt
adb shell am force-stop "$launcher"
timeout -k 3s 230s adb shell am instrument -w -e suite gadgets "$runner".StorageSmoke | tee gadgets-test.txt
grep -q 'PASS:' gadgets-test.txt
# Apply the cutout while a real portrait launcher window is present so WindowManager relayouts.
if [[ "$1" == 30 ]]; then
  python3 checks/ConfigurePortraitCutout.py
  timeout -k 3s 90s adb shell am instrument -w -e suite gadgets -e top-only true "$runner".StorageSmoke | tee desktop-top-test.txt
  grep -q 'PASS:' desktop-top-test.txt
  adb shell am force-stop "$launcher"
fi
adb devices -l
timeout -k 3s 10s adb reboot
sleep 4
timeout -k 3s 8s adb kill-server || true
timeout -k 3s 8s adb start-server
if ! timeout -k 3s 35s adb wait-for-device; then
  adb connect localhost:5555
  export ANDROID_SERIAL=localhost:5555
  timeout -k 3s 70s adb wait-for-device
fi
adb devices -l
ready=0
for attempt in $(seq 1 75); do
  if [[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == 1 ]]; then ready=1; break; fi
  sleep 2
done
[[ "$ready" == 1 ]]
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell input keyevent KEYCODE_HOME
adb shell am start -W -n "$component"
# Boot completion, Activity resume and keyguard dismissal are distinct asynchronous events.
# Observe the launcher's own committed boot identity before taking the first snapshot.
boot=$(adb shell settings get global boot_count | tr -d '\r')
python3 checks/WaitForHomeAfterReboot.py "$boot"
printf 'CHECK: verifying the first visible launcher after a real reboot\n'
boot=$(adb shell settings get global boot_count | tr -d '\r')
adb exec-out run-as "$launcher" cat shared_prefs/startup_session.xml > reboot-first.xml
mkdir -p screenshots
adb shell dumpsys window > screenshots/reboot-first-window.txt
adb shell dumpsys activity activities > screenshots/reboot-first-activity.txt
adb shell cat /proc/uptime > screenshots/reboot-first-uptime.txt
adb logcat -d -t 1000 > screenshots/reboot-first-log.txt
adb shell input keyevent KEYCODE_HOME
adb shell input keyevent KEYCODE_HOME
adb shell am force-stop "$launcher"
adb shell am start -W -n "$component"
sleep 2
adb exec-out run-as "$launcher" cat shared_prefs/startup_session.xml > reboot-again.xml
python3 checks/VerifyReboot.py "$boot" reboot-first.xml reboot-again.xml | tee reboot-test.txt
grep -q 'PASS:' reboot-test.txt
mkdir -p screenshots
adb exec-out screencap -p > screenshots/startup-after-reboot.png
exit "$snap_status"
