#!/usr/bin/env bash
set -euo pipefail
launcher=com.nthexperience.desktop.seven
collect() {
 mkdir -p screenshots
 for name in $(adb shell run-as "$launcher" ls files 2>/dev/null | tr -d '\r'); do
  case "$name" in seven-rows-*.png) adb exec-out run-as "$launcher" cat "files/$name" > "screenshots/$name" 2>/dev/null || true;; esac
 done
 adb logcat -d -t 500 > screenshots/rows-device-log.txt || true
}
trap collect EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
for runner in SevenTaskbarRowsSmoke SevenPolishSmoke SnapSmoke; do
 adb shell am force-stop "$launcher"
 adb shell pm clear "$launcher"
 log=seven-rows-${runner}.txt
 timeout -k 3s 300s adb shell am instrument -w "$launcher.test/com.nthexperience.desktop.$runner" | tee "$log"
 grep -q 'PASS:' "$log"
 ! grep -Eq 'FAIL:|shortMsg=|Process crashed|INSTRUMENTATION_FAILED' "$log"
 collect
done
