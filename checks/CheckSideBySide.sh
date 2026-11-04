#!/usr/bin/env bash
set -euo pipefail

vista=com.nthexperience.desktop
seven=com.nthexperience.desktop.seven
activity=com.nthexperience.desktop.MainActivity
adb install -r "$1"

for launcher in "$vista" "$seven"; do
  launch_output=$(adb shell am start -W -n "$launcher/$activity")
  printf '%s\n' "$launch_output"
  grep -q 'Status: ok' <<<"$launch_output"
  adb shell run-as "$launcher" id
done
providers=$(adb shell dumpsys package providers)
[[ "$providers" == *"[$vista.storage]"* && "$providers" == *"[$seven.storage]"* ]]

adb shell appops set --uid "$vista" MANAGE_EXTERNAL_STORAGE deny
adb shell appops set --uid "$seven" MANAGE_EXTERNAL_STORAGE allow
adb shell appops get --uid "$vista" MANAGE_EXTERNAL_STORAGE | grep -q 'deny'
adb shell appops get --uid "$seven" MANAGE_EXTERNAL_STORAGE | grep -q 'allow'
adb shell am force-stop "$seven"
adb shell "run-as $seven sh -c 'mkdir -p shared_prefs files/recycle-bin && touch shared_prefs/seven-probe.xml shared_prefs/media.xml files/recycle-bin/seven-probe'"
adb shell "run-as $vista sh -c 'test ! -e shared_prefs/seven-probe.xml && test ! -e shared_prefs/media.xml && test ! -e files/recycle-bin/seven-probe'"

adb shell appops set --uid "$seven" MANAGE_EXTERNAL_STORAGE deny
adb uninstall "$vista"
adb shell pm clear "$seven"
printf 'PASS: Vista and Seven launch side by side with separate provider authorities, storage grants and private state.\n'
