#!/usr/bin/env bash
set -euo pipefail
launcher=com.nthexperience.desktop.seven
mkdir -p build/start-touch-evidence
collect() {
  timeout -k 2s 10s adb logcat -d -t 700 > build/start-touch-evidence/device-log.txt || true
  for name in $(timeout -k 2s 10s adb shell run-as "$launcher" ls files 2>/dev/null | tr -d '\r'); do
    case "$name" in start-touch-*.png) timeout -k 2s 10s adb exec-out run-as "$launcher" cat "files/$name" > "build/start-touch-evidence/$name" 2>/dev/null || break;; esac
  done
}
trap collect EXIT
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb install -r build/start-baseline/app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
timeout -k 3s 100s adb shell am instrument -w "$launcher.test/com.nthexperience.desktop.SevenStartMenuTouchSmoke" | tee build/start-touch-evidence/baseline.txt
grep -q 'FAIL:.*Tap on pinned app icon opens installed app' build/start-touch-evidence/baseline.txt
! grep -q 'PASS:' build/start-touch-evidence/baseline.txt
collect
if [[ -f build/start-touch-evidence/start-touch-failure.png ]]; then mv build/start-touch-evidence/start-touch-failure.png build/start-touch-evidence/baseline-failure.png; fi
if [[ -f build/start-touch-evidence/start-touch-failure-system.png ]]; then mv build/start-touch-evidence/start-touch-failure-system.png build/start-touch-evidence/baseline-failure-system.png; fi
adb shell am force-stop "$launcher"
adb shell pm clear "$launcher"
adb install -r app/build/outputs/apk/debug/app-debug.apk
timeout -k 3s 300s adb shell am instrument -w "$launcher.test/com.nthexperience.desktop.SevenStartMenuTouchSmoke" | tee build/start-touch-evidence/fixed.txt
grep -q 'PASS:' build/start-touch-evidence/fixed.txt
! grep -Eq 'FAIL:|shortMsg=|Process crashed|INSTRUMENTATION_FAILED' build/start-touch-evidence/fixed.txt
