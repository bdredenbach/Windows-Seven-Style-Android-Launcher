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
    expected = baseline.get('approved_runtime_changes', {}).get(path, {}).get('sha256', expected)
    assert actual == expected, 'Existing Seven implementation changed: ' + path

main = (ROOT / 'app/src/main/java/com/nthexperience/desktop/MainActivity.java').read_text()
for name, expected in baseline['shell_methods'].items():
    actual = hashlib.sha256(method(main, name).encode()).hexdigest()
    expected = baseline.get('approved_shell_changes', {}).get(name, {}).get('sha256', expected)
    assert actual == expected, 'Existing Seven shell behavior changed: ' + name

# The cache wrapper must preserve every existing artwork command byte for byte.
icon_path = 'app/src/main/java/com/nthexperience/desktop/AeroIcon.java'
icon = (ROOT / icon_path).read_text()
start = icon.index('    private Bitmap cachedIcon;')
end = icon.index('    private void drawArtwork(Canvas canvas) {', start)
original_icon = icon[:start] + icon[end:].replace('    private void drawArtwork(Canvas canvas) {', '    @Override protected void onDraw(Canvas canvas) {', 1)
assert hashlib.sha256(original_icon.encode()).hexdigest() == baseline['unchanged_runtime_files'][icon_path], 'Original icon artwork changed'

build = (ROOT / 'app/build.gradle').read_text()
assert "versionName '0.7.1'" in build
assert "versionCode 24" in build
assert "com.nthexperience.desktop.seven" in (ROOT / "build.gradle").read_text()
print('PASS: Seven 0.7.1 identity, original icon artwork, %d unchanged runtime files, %d unchanged shell methods and explicitly pinned requested fixes' % (
    len(baseline['unchanged_runtime_files'])-len(baseline['approved_runtime_changes']), len(baseline['shell_methods'])-len(baseline['approved_shell_changes'])))
