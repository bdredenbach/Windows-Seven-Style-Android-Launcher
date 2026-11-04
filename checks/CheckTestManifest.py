"""Verify suite registrations in the actual test APK, after AGP rewrites the first entry."""
from pathlib import Path
import struct
import zipfile

required = {'FidelitySmoke', 'StorageSmoke', 'BrowserSmoke', 'PhotoSmoke', 'ContinuitySmoke', 'AuthenticitySmoke', 'FileOpenSmoke', 'PolishSmoke', 'DesktopExtrasSmoke', 'MediaSmoke', 'SnapSmoke'}
apk = Path('app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
with zipfile.ZipFile(apk) as archive:
    data = archive.read('AndroidManifest.xml')
strings, suites = [], set()

def length(at, wide):
    value = struct.unpack_from('<H', data, at)[0] if wide else data[at]
    at += 2 if wide else 1
    mask = 0x8000 if wide else 0x80
    if value & mask:
        next_value = struct.unpack_from('<H', data, at)[0] if wide else data[at]
        value = ((value & (mask - 1)) << (16 if wide else 8)) | next_value
        at += 2 if wide else 1
    return value, at

offset = struct.unpack_from('<H', data, 2)[0]
while offset < len(data):
    kind, header, size = struct.unpack_from('<HHI', data, offset)
    assert size > 0, 'Invalid manifest chunk'
    if kind == 1:
        count, styles, flags, start = struct.unpack_from('<IIII', data, offset + 8)
        for i in range(count):
            at = offset + start + struct.unpack_from('<I', data, offset + header + i * 4)[0]
            count_chars, at = length(at, not flags & 0x100)
            if flags & 0x100:
                count_bytes, at = length(at, False)
                strings.append(data[at:at + count_bytes].decode('utf-8'))
            else:
                strings.append(data[at:at + count_chars * 2].decode('utf-16-le'))
    elif kind == 0x102:
        tag = strings[struct.unpack_from('<I', data, offset + 20)[0]]
        if tag == 'instrumentation':
            start, step, count = struct.unpack_from('<HHH', data, offset + 24)
            attributes = {}
            for i in range(count):
                at = offset + 16 + start + i * step
                namespace, key, raw = struct.unpack_from('<III', data, at)
                value = struct.unpack_from('<I', data, at + 16)[0]
                if strings[key] in ('name', 'targetPackage'):
                    attributes[strings[key]] = strings[raw if raw != 0xffffffff else value]
            assert attributes.get('targetPackage') == 'com.nthexperience.desktop.seven', attributes
            suites.add(attributes['name'].rsplit('.', 1)[-1])
    offset += size
assert required <= suites, 'Test APK is missing instrumentation: ' + ', '.join(sorted(required - suites))
print('PASS: all required native suites are registered in the packaged test APK')
