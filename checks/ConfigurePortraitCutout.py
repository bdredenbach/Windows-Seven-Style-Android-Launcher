"""Configure the emulator display before the instrumentation process caches DisplayInfo."""
import re
import subprocess
import time

def adb(*args):
    return subprocess.check_output(['adb', 'shell', *args], text=True, timeout=8)

overlays = adb('cmd', 'overlay', 'list')
overlay = next(('com.android.internal.display.cutout.emulation.' + name for name in ('tall', 'corner', 'double') if 'com.android.internal.display.cutout.emulation.' + name in overlays), None)
assert overlay, 'Portrait fixture needs a cutout overlay'
adb('cmd', 'overlay', 'enable-exclusive', '--user', '0', '--category', overlay)
# Start a fresh real window after the overlay. Android 11 can retain an empty
# WindowManager cutout cache when a live application was created without it.
adb('settings', 'put', 'system', 'accelerometer_rotation', '0')
adb('settings', 'put', 'system', 'user_rotation', '0')
adb('am', 'force-stop', 'com.nthexperience.desktop.seven')
adb('am', 'start', '-W', '-n', 'com.nthexperience.desktop.seven/com.nthexperience.desktop.MainActivity')
deadline = time.monotonic() + 15
while time.monotonic() < deadline:
    state = adb('dumpsys', 'display')
    # Observe the actual display device, then let instrumentation require real
    # Display.getCutout and WindowInsets and physically tap the reclaimed area.
    if re.search(r'DisplayDeviceInfo\{[^\n]*cutout DisplayCutout\{insets=Rect\(0, [1-9]\d* -', state):
        print('CHECK: portrait cutout is active before starting instrumentation')
        break
    time.sleep(.15)
else:
    print(adb('cmd', 'overlay', 'list'))
    print(adb('dumpsys', 'window', 'displays'))
    print(state)
    raise AssertionError('WindowManager has not activated the portrait cutout')
adb('am', 'force-stop', 'com.nthexperience.desktop.seven')
