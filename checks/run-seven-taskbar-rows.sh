#!/usr/bin/env bash
set -euo pipefail
launcher=com.nthexperience.desktop.seven
collect() {
 mkdir -p screenshots
 for name in $(timeout -k 2s 10s adb shell run-as "$launcher" ls files 2>/dev/null | tr -d '\r'); do
  case "$name" in seven-rows-*.png|seven-independent-rows-*.png|start-touch-*.png|seven-usage-*.png|seven-polish-*.png|seven-setup-*.png) timeout -k 2s 10s adb exec-out run-as "$launcher" cat "files/$name" > "screenshots/$name" 2>/dev/null || break;; esac
 done
 timeout -k 2s 10s adb logcat -d -t 500 > screenshots/rows-device-log.txt || true
}
trap collect EXIT
timeout -k 3s 120s adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
timeout -k 3s 120s adb install -r app/build/outputs/apk/debug/app-debug.apk
timeout -k 3s 120s adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
timeout -k 2s 15s adb shell input keyevent KEYCODE_WAKEUP
timeout -k 2s 15s adb shell wm dismiss-keyguard
# ATD deliberately omits full Android Settings. Its separate setup-recovery
# workflow uses a full system image; keep this matrix on the stable row-test image.
full_settings=false
if timeout -k 2s 15s adb shell cmd package resolve-activity --brief -a android.settings.USAGE_ACCESS_SETTINGS | grep -q 'com.android.settings/'; then full_settings=true; fi
for runner in SevenIndependentRowsSmoke SevenTaskbarRowsSmoke SevenStartMenuTouchSmoke SevenUsageAccessSmoke SevenSetupSmoke SevenPolishSmoke SnapSmoke FidelitySmoke; do
 if [[ "$runner" == SevenUsageAccessSmoke && "$full_settings" != true ]]; then
  printf 'SKIP: full Android Settings is absent from this test image; real Settings recovery is covered on API30 and in the separate setup workflow.\n' | tee seven-rows-SevenUsageAccessSmoke.txt
  continue
 fi
 printf 'Running %s\n' "$runner"
 timeout -k 2s 15s adb shell am force-stop "$launcher"
 timeout -k 2s 15s adb shell wm size reset
 timeout -k 2s 15s adb shell pm clear "$launcher"
 sleep 1
 if [[ "$runner" == FidelitySmoke ]]; then timeout -k 2s 15s adb shell appops set --uid "$launcher" MANAGE_EXTERNAL_STORAGE allow; fi
 log=seven-rows-${runner}.txt
 timeout -k 3s 300s adb shell am instrument -w "$launcher.test/com.nthexperience.desktop.$runner" | tee "$log"
 grep -q 'PASS:' "$log"
 ! grep -Eq 'FAIL:|shortMsg=|Process crashed|INSTRUMENTATION_FAILED' "$log"
 collect
done
