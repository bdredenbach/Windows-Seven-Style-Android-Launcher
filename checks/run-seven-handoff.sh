#!/usr/bin/env bash
set -euo pipefail
launcher=com.nthexperience.desktop.seven
mkdir -p build/handoff-evidence
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
failed=0
for runner in SevenSetupSmoke SevenTaskbarRowsSmoke SevenPolishSmoke SnapSmoke FidelitySmoke; do
  adb shell am force-stop "$launcher"
  adb shell pm clear "$launcher"
  if [[ "$runner" == FidelitySmoke ]]; then adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE allow; fi
  log="build/handoff-evidence/${runner}.txt"
  if ! timeout -k 3s 300s adb shell am instrument -w "$launcher.test/com.nthexperience.desktop.$runner" | tee "$log"; then failed=1; fi
  if ! grep -q 'PASS:' "$log" || grep -Eq 'FAIL:|shortMsg=|Process crashed|INSTRUMENTATION_FAILED' "$log"; then failed=1; fi
  for name in $(adb shell run-as "$launcher" ls files 2>/dev/null | tr -d '\r'); do
    case "$name" in *.png) adb exec-out run-as "$launcher" cat "files/$name" > "build/handoff-evidence/$name" 2>/dev/null || true;; esac
  done
  adb logcat -d -t 300 > "build/handoff-evidence/${runner}-logcat.txt"
done
exit "$failed"
