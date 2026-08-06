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

themes = {theme.get("themeId"): theme for theme in game.get("themes", [])}
the_last_commit = themes.get("the_last_commit", {})
convenience_store = themes.get("convenience_store_loop", {})

if "the_last_commit" not in themes:
    errors.append("game_spec.json: missing theme the_last_commit")
if "convenience_store_loop" not in themes:
    errors.append("game_spec.json: missing theme convenience_store_loop")
if "storageKey" not in game:
    errors.append("game_spec.json: missing storageKey")
for key in ("screenFlow", "puzzles"):
    if key not in the_last_commit:
        errors.append(f"game_spec.json: the_last_commit theme missing {key}")

the_last_commit_puzzle_ids = ["messenger_order", "flashlight_search", "encrypted_note", "audio_record", "commit_graph", "access_log", "server_code"]
convenience_store_puzzle_ids = ["receipt_price", "barcode_rule", "shelf_difference", "cctv_sequence", "inventory_crosscheck", "customer_pattern", "incident_timeline"]

for puzzle_id in naming.get("puzzleIds", []):
    if puzzle_id in the_last_commit_puzzle_ids:
        if puzzle_id not in the_last_commit.get("puzzles", {}):
            errors.append(f"game_spec.json: missing the_last_commit puzzle {puzzle_id}")
    elif puzzle_id in convenience_store_puzzle_ids:
        if puzzle_id not in convenience_store.get("puzzleIds", []):
            errors.append(f"game_spec.json: missing convenience_store_loop puzzleId {puzzle_id}")

if the_last_commit.get("puzzles", {}).get("server_code", {}).get("answer") != "417121":
    errors.append("game_spec.json: the_last_commit server answer must be 417121")
if the_last_commit.get("puzzles", {}).get("flashlight_search", {}).get("digits") != [4, 1, 7]:
    errors.append("game_spec.json: the_last_commit flashlight digits must be [4, 1, 7]")

if errors:
    print("Shared spec validation failed:\n- " + "\n- ".join(errors))
    sys.exit(1)
total_puzzles = len(the_last_commit.get("puzzles", {})) + len(convenience_store.get("puzzleIds", []))
print(f"Shared spec validation passed (4 JSON files, {len(themes)} themes, {total_puzzles} puzzle definitions).")
