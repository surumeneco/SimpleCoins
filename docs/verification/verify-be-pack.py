#!/usr/bin/env python3
"""Offline, read-only structural verifier for SimpleCoins Bedrock pack + mapping.

This does not prove Geyser installation, atomic two-file publication, or client display.
"""

import hashlib
from io import BytesIO
import json
import os
from pathlib import Path, PurePosixPath
import stat
import struct
import sys
import uuid
import zipfile
import zlib

DENOMINATIONS = (1, 5, 10, 50, 100, 500, 1000, 5000, 10000)


class VerificationError(Exception):
    pass


def require_regular_unlinked(path: Path) -> Path:
    absolute = path.absolute()
    for prefix in (absolute, *absolute.parents):
        if prefix.is_symlink():
            raise VerificationError("Artifact path contains a symbolic link")
    if not absolute.is_file():
        raise VerificationError("Artifact must be an existing regular file")
    return absolute


def no_duplicate_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise VerificationError("Duplicate JSON member")
        result[key] = value
    return result


def load_json(data: bytes):
    try:
        return json.loads(data.decode("utf-8"), object_pairs_hook=no_duplicate_keys)
    except (UnicodeError, json.JSONDecodeError) as error:
        raise VerificationError("Invalid UTF-8 JSON") from error


def require(condition: bool, message: str) -> None:
    if not condition:
        raise VerificationError(message)


def java_name_uuid(value: str) -> str:
    raw = bytearray(hashlib.md5(value.encode("utf-8")).digest())
    raw[6] = (raw[6] & 0x0F) | 0x30
    raw[8] = (raw[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(raw)))


def verify_png(data: bytes) -> None:
    require(data.startswith(b"\x89PNG\r\n\x1a\n"), "Invalid PNG signature")
    offset = 8
    kinds = []
    while offset < len(data):
        require(offset + 12 <= len(data), "Truncated PNG chunk header")
        size = struct.unpack_from(">I", data, offset)[0]
        require(size <= 16 * 1024 * 1024 and offset + 12 + size <= len(data),
                "Truncated or oversized PNG chunk")
        kind = data[offset + 4:offset + 8]
        payload = data[offset + 8:offset + 8 + size]
        expected = struct.unpack_from(">I", data, offset + 8 + size)[0]
        require(zlib.crc32(kind + payload) == expected, "PNG chunk CRC mismatch")
        kinds.append(kind)
        if kind == b"IHDR":
            require(len(kinds) == 1 and size == 13, "Invalid PNG IHDR")
            width, height = struct.unpack_from(">II", payload)
            require(width > 0 and height > 0, "Invalid PNG dimensions")
        offset += size + 12
        if kind == b"IEND":
            require(size == 0 and offset == len(data), "Invalid PNG end marker")
            break
    require(kinds and kinds[0] == b"IHDR" and kinds.count(b"IHDR") == 1
            and b"IDAT" in kinds and kinds[-1] == b"IEND", "Incomplete PNG image")


def verify(pack: Path, mapping: Path) -> dict:
    pack = require_regular_unlinked(pack)
    mapping = require_regular_unlinked(mapping)
    require(not os.path.samefile(pack, mapping), "Pack and mapping are the same physical file")
    # Verify and hash one immutable in-memory copy, not two possibly different reads.
    require(pack.stat().st_size <= 64 * 1024 * 1024, "Bedrock ZIP exceeds verifier size budget")
    require(mapping.stat().st_size <= 2 * 1024 * 1024, "Mapping exceeds verifier size budget")
    pack_bytes = pack.read_bytes()
    mapping_bytes = mapping.read_bytes()
    require(len(pack_bytes) <= 64 * 1024 * 1024, "Bedrock ZIP changed beyond size budget")
    require(len(mapping_bytes) <= 2 * 1024 * 1024, "Mapping changed beyond size budget")
    expected_entries = {"manifest.json", "textures/item_texture.json"}
    expected_entries.update(f"textures/items/coin_{value}.png" for value in DENOMINATIONS)
    try:
        with zipfile.ZipFile(BytesIO(pack_bytes)) as archive:
            names = archive.namelist()
            require(all(info.file_size <= 16 * 1024 * 1024 and
                        stat.S_IFMT(info.external_attr >> 16) != stat.S_IFLNK and
                        not info.is_dir() for info in archive.infolist()),
                    "Unsupported or oversized Bedrock ZIP entry")
            require(len(names) == len(set(names)), "Duplicate ZIP member")
            for name in names:
                path = PurePosixPath(name)
                require(not name.startswith("/") and "\\" not in name
                        and ".." not in path.parts and name == str(path),
                        "Unsafe ZIP member name")
            require(set(names) == expected_entries, "Unexpected or missing Bedrock ZIP member")
            require(archive.testzip() is None, "Bedrock ZIP CRC mismatch")
            manifest = load_json(archive.read("manifest.json"))
            textures = load_json(archive.read("textures/item_texture.json"))
            images = {value: archive.read(f"textures/items/coin_{value}.png")
                      for value in DENOMINATIONS}
    except (OSError, zipfile.BadZipFile, RuntimeError, KeyError, ValueError) as error:
        raise VerificationError("Unreadable or malformed Bedrock ZIP") from error

    definitions = load_json(mapping_bytes)
    require(isinstance(definitions, dict) and definitions.get("format_version") == 2,
            "Invalid Geyser v2 mapping version")
    items = definitions.get("items")
    require(isinstance(items, dict) and set(items) == {"minecraft:paper"},
            "Unexpected Geyser item mapping")
    entries = items["minecraft:paper"]
    require(isinstance(entries, list) and len(entries) == len(DENOMINATIONS),
            "Incorrect denomination mapping count")
    require(isinstance(textures, dict) and textures.get("resource_pack_name") == "SimpleCoins"
            and textures.get("texture_name") == "atlas.items", "Invalid texture atlas metadata")
    texture_data = textures.get("texture_data")
    require(isinstance(texture_data, dict)
            and set(texture_data) == {f"simplecoins:coin_{v}" for v in DENOMINATIONS},
            "Incorrect texture atlas denominations")

    digest = hashlib.sha256()
    for value, entry in zip(DENOMINATIONS, entries):
        model = f"simplecoins:coin_{value}"
        require(isinstance(entry, dict) and entry.get("type") == "definition"
                and entry.get("model") == model
                and entry.get("bedrock_identifier") == model,
                "Incorrect Geyser denomination identity")
        name = entry.get("display_name")
        require(isinstance(name, str), "Missing Geyser denomination display name")
        require(entry.get("bedrock_options") == {"icon": model}
                and entry.get("components") == {"minecraft:max_stack_size": 64},
                "Incorrect Geyser denomination components")
        require(texture_data[model] == {"textures": [f"textures/items/coin_{value}"]},
                "Geyser texture mapping differs from pack")
        image = images[value]
        verify_png(image)
        try:
            name_bytes = name.encode("utf-8")
        except UnicodeError as error:
            raise VerificationError("Invalid display name Unicode") from error
        digest.update(struct.pack(">III", value, len(name_bytes), len(image)))
        digest.update(name_bytes)
        digest.update(image)

    fingerprint = digest.hexdigest()
    require(isinstance(manifest, dict) and manifest.get("format_version") == 2,
            "Invalid Bedrock manifest format")
    header = manifest.get("header")
    modules = manifest.get("modules")
    require(isinstance(header, dict) and isinstance(modules, list) and len(modules) == 1
            and isinstance(modules[0], dict), "Invalid Bedrock manifest structure")
    expected_header = java_name_uuid("simplecoins:header:" + fingerprint)
    expected_module = java_name_uuid("simplecoins:module:" + fingerprint)
    require(header.get("uuid") == expected_header and modules[0].get("uuid") == expected_module,
            "Bedrock manifest UUID does not match content fingerprint")
    require(header.get("version") == [1, 0, 0] and modules[0].get("version") == [1, 0, 0]
            and modules[0].get("type") == "resources", "Invalid Bedrock manifest version/module")
    return {"pack_sha256": hashlib.sha256(pack_bytes).hexdigest(),
            "mapping_sha256": hashlib.sha256(mapping_bytes).hexdigest(),
            "fingerprint": fingerprint, "header_uuid": expected_header,
            "denominations": len(DENOMINATIONS)}


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print("Usage: verify-be-pack.py <SimpleCoins.mcpack> <simplecoins.json>", file=sys.stderr)
        return 2
    try:
        print(json.dumps(verify(Path(argv[1]), Path(argv[2])), sort_keys=True))
        return 0
    except (VerificationError, OSError) as error:
        print(f"Bedrock artifact verification failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
