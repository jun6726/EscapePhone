#!/usr/bin/env python3
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
required = ["game_spec.json", "naming_contract.json", "ui_strings_ko.json", "design_tokens.json"]
errors = []
documents = {}
for name in required:
    path = ROOT / "Shared/Specifications" / name
    try:
        documents[name] = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        errors.append(f"{name}: {error}")

game = documents.get("game_spec.json", {})
naming = documents.get("naming_contract.json", {})
for key in ("screenFlow", "puzzles", "storageKey"):
    if key not in game:
        errors.append(f"game_spec.json: missing {key}")
for puzzle_id in naming.get("puzzleIds", []):
    if puzzle_id not in game.get("puzzles", {}):
        errors.append(f"game_spec.json: missing puzzle {puzzle_id}")
if game.get("puzzles", {}).get("server_code", {}).get("answer") != "417121":
    errors.append("game_spec.json: server answer must be 417121")
if game.get("puzzles", {}).get("flashlight_search", {}).get("digits") != [4, 1, 7]:
    errors.append("game_spec.json: flashlight digits must be [4, 1, 7]")

if errors:
    print("Shared spec validation failed:\n- " + "\n- ".join(errors))
    sys.exit(1)
print(f"Shared spec validation passed (4 JSON files, {len(game.get('puzzles', {}))} puzzles).")
