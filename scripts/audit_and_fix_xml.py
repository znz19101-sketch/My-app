from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(".")
ignored = {".git", ".gradle", ".idea", "build", "node_modules"}
scanned = 0
fixed = []
invalid = []

for path in root.rglob("*.xml"):
    if not path.is_file():
        continue

    if any(part in ignored for part in path.parts):
        continue

    scanned += 1

    try:
        raw = path.read_bytes()

        # Remove UTF-8 BOM.
        if raw.startswith(b"\xef\xbb\xbf"):
            raw = raw[3:]

        text = raw.decode("utf-8")

        # Normalize line endings.
        text = text.replace("\r\n", "\n").replace("\r", "\n")

        # Remove blank lines, spaces and BOM characters before XML content.
        cleaned = text.lstrip("\ufeff \t\n")

        # Keep exactly one newline at the end.
        cleaned = cleaned.rstrip() + "\n"

        if cleaned != text:
            path.write_text(cleaned, encoding="utf-8", newline="\n")
            fixed.append(path)

        ET.parse(path)

    except Exception as error:
        invalid.append((path, str(error)))

print(f"XML files scanned: {scanned}")
print(f"XML files fixed: {len(fixed)}")

for path in fixed:
    print(f"FIXED: {path}")

if invalid:
    print("\nINVALID XML FILES:")
    for path, error in invalid:
        print(f"ERROR: {path}: {error}")
    sys.exit(1)

print("\nSUCCESS: All project XML files are structurally valid.")
