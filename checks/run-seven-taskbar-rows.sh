#!/usr/bin/env bash
set -euo pipefail
launcher=com.nthexperience.desktop.seven
collect() {
 mkdir -p screenshots
 for name in $(adb shell run-as "$launcher" ls files 2>/dev/null | tr -d '\r'); do
  case "$name" in seven-rows-*.png|seven-independent-rows-*.png|start-touch-*.png|seven-usage-*.png|seven-polish-*.png|seven-setup-*.png) adb exec-out run-as "$launcher" cat "files/$name" > "screenshots/$name" 2>/dev/null || true;; esac
 done
 adb logcat -d -t 500 > screenshots/rows-device-log.txt || true
}
trap collect EXIT
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
for runner in SevenIndependentRowsSmoke SevenTaskbarRowsSmoke SevenStartMenuTouchSmoke SevenUsageAccessSmoke SevenSetupSmoke SevenPolishSmoke SnapSmoke FidelitySmoke; do
 adb shell am force-stop "$launcher"
 adb shell wm size reset
 adb shell pm clear "$launcher"
 sleep 1
 if [[ "$runner" == FidelitySmoke ]]; then adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE allow; fi
 log=seven-rows-${runner}.txt
 timeout -k 3s 300s adb shell am instrument -w "$launcher.test/com.nthexperience.desktop.$runner" | tee "$log"
 grep -q 'PASS:' "$log"
 ! grep -Eq 'FAIL:|shortMsg=|Process crashed|INSTRUMENTATION_FAILED' "$log"
 collect
done
