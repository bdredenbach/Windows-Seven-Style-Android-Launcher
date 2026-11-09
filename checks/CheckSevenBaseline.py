"""Verify that this patch retains the established Seven 0.7.1 desktop shell."""
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
baseline = json.loads((ROOT / 'checks/seven-071-baseline.json').read_text())

def method(text, name):
    match = re.search(r'^    (?:(?:@Override|private|public|protected|static|final)\s+)*(?:[\w.<>?,\[\]]+)\s+' + re.escape(name) + r'\([^\n]*', text, re.M)
    if not match:
        raise AssertionError('Missing shell method: ' + name)
    start = text.index('{', match.start())
    tokens = re.finditer(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/|[{}]', text[start:], re.S)
    depth = 0
    for token in tokens:
        if token.group() == '{':
            depth += 1
        elif token.group() == '}':
            depth -= 1
            if depth == 0:
                return text[match.start():start + token.end()]
    raise AssertionError('Unclosed shell method: ' + name)

for path, expected in baseline['unchanged_runtime_files'].items():
    actual = hashlib.sha256((ROOT / path).read_bytes()).hexdigest()
    assert actual == expected, 'Existing Seven implementation changed: ' + path

main = (ROOT / 'app/src/main/java/com/nthexperience/desktop/MainActivity.java').read_text()
for name, expected in baseline['shell_methods'].items():
    actual = hashlib.sha256(method(main, name).encode()).hexdigest()
    assert actual == expected, 'Existing Seven shell behavior changed: ' + name

build = (ROOT / 'app/build.gradle').read_text()
assert "versionName '0.7.1'" in build
assert "versionCode 24" in build
assert "com.nthexperience.desktop.seven" in (ROOT / "build.gradle").read_text()
print('PASS: Seven 0.7.1 identity, %d runtime files and %d shell methods preserved' % (
    len(baseline['unchanged_runtime_files']), len(baseline['shell_methods'])))
