#!/usr/bin/env python3
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
contract = json.loads((ROOT / "Shared/Specifications/naming_contract.json").read_text(encoding="utf-8"))

def combined(path: pathlib.Path, suffix: str) -> str:
    return "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in path.rglob(f"*{suffix}"))

android = combined(ROOT / "AOS", ".kt")
ios = combined(ROOT / "ios", ".swift")
errors = []
for category in ("models", "enums", "interfaces", "functions", "variables", "puzzleIds", "screenIds", "screens"):
    for name in contract.get(category, []):
        if name not in android:
            errors.append(f"Android missing {category}: {name}")
        if name not in ios:
            errors.append(f"iOS missing {category}: {name}")

required_theme_symbols = {
    "types": ["ThemeId", "ThemeMetadata", "ThemeProgress", "ThemeStatus", "ThemeRegistry", "ThemeProgressStore"],
    "screens": ["ThemeSelectionScreen"],
}
for category, names in required_theme_symbols.items():
    for name in names:
        if name not in android:
            errors.append(f"Android missing {category}: {name}")
        if name not in ios:
            errors.append(f"iOS missing {category}: {name}")

for theme_id in contract.get("themeIds", []):
    if theme_id not in android:
        errors.append(f"Android missing themeId literal: {theme_id}")
    if theme_id not in ios:
        errors.append(f"iOS missing themeId literal: {theme_id}")

test_names = []
for line in (ROOT / "Documentation/PARITY_CONTRACT.md").read_text(encoding="utf-8").splitlines():
    if line.startswith("| ") and "_" in line:
        test_names.append(line.split("|")[1].strip())
for name in test_names:
    if name not in android:
        errors.append(f"Android missing test scenario: {name}")
    if name not in ios:
        errors.append(f"iOS missing test scenario: {name}")

root_spec = (ROOT / "Shared/Specifications/game_spec.json").read_bytes()
for relative in ("AOS/app/src/main/assets/game_spec.json", "ios/EscapePhone/Resources/game_spec.json"):
    path = ROOT / relative
    if not path.exists() or path.read_bytes() != root_spec:
        errors.append(f"spec copy differs: {relative}")
for name in ("ui_strings_ko.json", "design_tokens.json"):
    root_data = (ROOT / "Shared/Specifications" / name).read_bytes()
    for relative in (f"AOS/app/src/main/assets/{name}", f"ios/EscapePhone/Resources/{name}"):
        path = ROOT / relative
        if not path.exists() or path.read_bytes() != root_data:
            errors.append(f"shared resource differs: {relative}")

validation = subprocess.run([sys.executable, str(ROOT / "tools/validate_shared_spec.py")], capture_output=True, text=True)
if validation.returncode:
    errors.append(validation.stdout.strip())
if errors:
    print("Parity check failed:\n- " + "\n- ".join(errors))
    sys.exit(1)
print(f"Parity check passed ({sum(len(contract[k]) for k in contract if isinstance(contract[k], list))} contract symbols, {len(test_names)} test scenarios).")
