"""Observe a focused Home after reboot; retain and wait out an Android system ANR."""
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

boot = int(sys.argv[1])
evidence = pathlib.Path("screenshots")
evidence.mkdir(exist_ok=True)
recovered = False


def adb(*args, binary=False):
    return subprocess.check_output(["adb", *args], timeout=15, text=not binary)


for attempt in range(45):
    windows = adb("shell", "dumpsys", "window")
    focus = next((line for line in windows.splitlines() if "mCurrentFocus=" in line), "")
    if "Application Not Responding: system}" in focus and not recovered:
        # This modal belongs to Android itself. Never dismiss an app ANR or restart Nth.
        (evidence / "reboot-system-anr-window.txt").write_text(windows)
        (evidence / "reboot-system-anr.png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))
        adb("shell", "uiautomator", "dump", "/sdcard/nth-reboot-system-anr.xml")
        xml = adb("exec-out", "cat", "/sdcard/nth-reboot-system-anr.xml")
        (evidence / "reboot-system-anr.xml").write_text(xml)
        wait = next((n for n in ET.fromstring(xml).iter("node")
                     if n.get("package") == "android" and
                     (n.get("resource-id") == "android:id/aerr_wait" or n.get("text", "").lower() == "wait")), None)
        if wait is None:
            # The system modal can disappear while uiautomator waits for idle.
            current = adb("shell", "dumpsys", "window")
            current_focus = next((line for line in current.splitlines() if "mCurrentFocus=" in line), "")
            assert "Application Not Responding: system}" not in current_focus, "Android's system ANR must offer Wait"
            print("CHECK: Android's system modal cleared while collecting its evidence", flush=True)
            continue
        bounds = list(map(int, re.findall(r"\d+", wait.get("bounds", ""))))
        assert len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]
        adb("shell", "input", "tap", str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))
        recovered = True
        print("CHECK: retained Android system-ANR evidence and chose Wait before observing Home", flush=True)
    else:
        xml = adb("exec-out", "run-as", "com.nthexperience.desktop.seven", "cat", "shared_prefs/startup_session.xml")
        pathlib.Path("reboot-first.xml").write_text(xml)
        claimed = ET.fromstring(xml).find("int[@name='boot']")
        if "com.nthexperience.desktop.seven/com.nthexperience.desktop.MainActivity" in focus:
            if claimed is not None and int(claimed.get("value")) == boot:
                print("CHECK: Nth has real window focus and has claimed this Android boot", flush=True)
                break
    adb("shell", "wm", "dismiss-keyguard")
    time.sleep(1)
else:
    (evidence / "reboot-home-unavailable-window.txt").write_text(windows)
    raise AssertionError("The first focused Home did not claim this boot: " + focus)
