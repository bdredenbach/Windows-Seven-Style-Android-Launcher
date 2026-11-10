#!/usr/bin/env bash
set -euo pipefail
launcher=com.nthexperience.desktop.seven
collect() {
  mkdir -p build/usage-evidence
  timeout -k 2s 15s adb logcat -d -t 1000 > build/usage-evidence/device-log.txt || true
  for name in $(timeout -k 2s 10s adb shell run-as "$launcher" ls files 2>/dev/null | tr -d '\r'); do
    case "$name" in seven-usage-*.png) timeout -k 2s 10s adb exec-out run-as "$launcher" cat "files/$name" > "build/usage-evidence/$name" 2>/dev/null || break;; esac
  done
}
trap collect EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear "$launcher"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
mkdir -p build/usage-evidence
timeout -k 3s 300s adb shell am instrument -w "$launcher.test/com.nthexperience.desktop.SevenUsageAccessSmoke" | tee build/usage-evidence/usage-access.txt
grep -q 'PASS:' build/usage-evidence/usage-access.txt
! grep -Eq 'FAIL:|shortMsg=|Process crashed|INSTRUMENTATION_FAILED' build/usage-evidence/usage-access.txt
