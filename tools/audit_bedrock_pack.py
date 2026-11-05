#!/usr/bin/env python3
"""Offline structural verifier for SimpleCoins Bedrock pack/mapping artifacts.

No Paper/Geyser/Bedrock runtime attestation is performed.
"""
import argparse
import json
import sys
import uuid
import zipfile
from pathlib import Path

VALUES = (1, 5, 10, 50, 100, 500, 1000, 5000, 10000)
IDS = {f"simplecoins:coin_{n}" for n in VALUES}
TEXTURES = {f"textures/items/coin_{n}.png" for n in VALUES}
MEMBERS = {"manifest.json", "textures/item_texture.json", *TEXTURES}
MAX_ARCHIVE = 512 * 1024 * 1024
MAX_UNCOMPRESSED = 256 * 1024 * 1024
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def audit(pack, mapping):
    """Return stable codes; valid_structure is not client acceptance."""
    errors = set()
    try:
        if pack.stat().st_size > MAX_ARCHIVE or mapping.stat().st_size > 1024 * 1024:
            return {"status": "invalid", "codes": ["INPUT_TOO_LARGE"]}
        with zipfile.ZipFile(pack) as archive:
            infos = archive.infolist()
            names = [item.filename for item in infos]
            if len(names) != len(set(names)):
                errors.add("DUPLICATE_ZIP_ENTRY")
            if set(names) != MEMBERS:
                errors.add("UNEXPECTED_OR_MISSING_ZIP_ENTRY")
            if sum(item.file_size for item in infos) > MAX_UNCOMPRESSED:
                return {"status": "invalid", "codes": ["ZIP_EXPANSION_LIMIT"]}
            if archive.testzip() is not None:
                errors.add("CORRUPT_ZIP_MEMBER")
            if not MEMBERS.issubset(set(names)):
                return {"status": "invalid", "codes": sorted(errors)}
            if any(archive.getinfo(name).file_size == 0 for name in TEXTURES):
                errors.add("EMPTY_TEXTURE")
            for name in TEXTURES:
                with archive.open(name) as image:
                    if image.read(8) != PNG_SIGNATURE:
                        errors.add("INVALID_PNG_SIGNATURE")
            manifest = json.loads(archive.read("manifest.json"))
            atlas = json.loads(archive.read("textures/item_texture.json"))
        mapped = json.loads(mapping.read_text(encoding="utf-8"))
        if not isinstance(manifest, dict) or manifest.get("format_version") != 2:
            errors.add("INVALID_MANIFEST")
        else:
            header = manifest.get("header")
            modules = manifest.get("modules")
            if not isinstance(header, dict) or header.get("name") != "SimpleCoins" or not isinstance(modules, list) or len(modules) != 1:
                errors.add("INVALID_MANIFEST")
            else:
                try:
                    if uuid.UUID(header["uuid"]) == uuid.UUID(modules[0]["uuid"]):
                        errors.add("DUPLICATE_PACK_UUID")
                    if modules[0].get("type") != "resources":
                        errors.add("INVALID_MANIFEST")
                except (KeyError, TypeError, ValueError, AttributeError):
                    errors.add("INVALID_MANIFEST")
        if not isinstance(atlas, dict) or atlas.get("resource_pack_name") != "SimpleCoins" or atlas.get("texture_name") != "atlas.items":
            errors.add("INVALID_TEXTURE_ATLAS")
        else:
            data = atlas.get("texture_data")
            if not isinstance(data, dict) or set(data) != IDS:
                errors.add("INVALID_TEXTURE_ATLAS")
            else:
                for n in VALUES:
                    entry = data[f"simplecoins:coin_{n}"]
                    if not isinstance(entry, dict) or entry.get("textures") != [f"textures/items/coin_{n}"]:
                        errors.add("TEXTURE_REFERENCE_MISMATCH")
        if not isinstance(mapped, dict) or mapped.get("format_version") != 2 or not isinstance(mapped.get("items"), dict):
            errors.add("INVALID_MAPPING")
        else:
            if set(mapped["items"]) != {"minecraft:paper"}:
                errors.add("UNEXPECTED_MAPPING_ITEM")
            entries = mapped["items"].get("minecraft:paper")
            if not isinstance(entries, list) or len(entries) != len(VALUES):
                errors.add("INVALID_MAPPING")
            else:
                seen = set()
                for entry in entries:
                    if not isinstance(entry, dict):
                        errors.add("INVALID_MAPPING_ENTRY")
                        continue
                    name = entry.get("model")
                    if not isinstance(name, str):
                        errors.add("INVALID_MAPPING_ENTRY")
                        continue
                    if name in seen:
                        errors.add("DUPLICATE_MAPPING_ID")
                    seen.add(name)
                    options = entry.get("bedrock_options")
                    components = entry.get("components")
                    if (name not in IDS or entry.get("type") != "definition" or
                            entry.get("bedrock_identifier") != name or
                            not isinstance(options, dict) or options.get("icon") != name or
                            not isinstance(components, dict) or components.get("minecraft:max_stack_size") != 64 or
                            not isinstance(entry.get("display_name"), str)):
                        errors.add("INVALID_MAPPING_ENTRY")
                if seen != IDS:
                    errors.add("MISSING_MAPPING_ID")
    except (OSError, zipfile.BadZipFile, RuntimeError, UnicodeError, ValueError, TypeError, KeyError, AttributeError, json.JSONDecodeError):
        errors.add("UNREADABLE_ARTIFACT")
    return {"status": "invalid" if errors else "valid_structure", "codes": sorted(errors)}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pack", type=Path)
    parser.add_argument("mapping", type=Path)
    args = parser.parse_args(argv)
    result = audit(args.pack, args.mapping)
    print(json.dumps(result, sort_keys=True))
    return 0 if result["status"] == "valid_structure" else 2


if __name__ == "__main__":
    sys.exit(main())
