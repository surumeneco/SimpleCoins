#!/usr/bin/env python3
"""Conservative bytecode inventory of SimpleCoins startup settlement wiring.

This is a *negative* evidence tool. A successful static inventory is never a
proof that Paper login denial, full settlement, or crash recovery work.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import struct
import sys
from zipfile import BadZipFile, ZipFile


class InvalidClassFile(ValueError):
    """Malformed or unsupported Java class-file constant pool."""


def utf8_pool(payload: bytes) -> frozenset[str]:
    """Read JVM CONSTANT_Utf8 entries, including long/double two-slot entries."""
    if len(payload) < 10 or payload[:4] != b"\xca\xfe\xba\xbe":
        raise InvalidClassFile("Invalid Java class-file header")
    count = struct.unpack_from(">H", payload, 8)[0]
    offset = 10
    index = 1
    values = set()

    def consume(size: int) -> bytes:
        nonlocal offset
        if size < 0 or offset + size > len(payload):
            raise InvalidClassFile("Truncated Java constant pool")
        result = payload[offset:offset + size]
        offset += size
        return result

    while index < count:
        tag = consume(1)[0]
        if tag == 1:
            length = struct.unpack(">H", consume(2))[0]
            raw = consume(length)
            # Java uses modified UTF-8, but ASCII class/member names are valid
            # in both representations; non-ASCII entries are not gate evidence.
            if all(byte < 128 for byte in raw):
                values.add(raw.decode("ascii"))
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            consume(4)
        elif tag in (5, 6):
            consume(8)
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            consume(2)
        elif tag == 15:
            consume(3)
        else:
            raise InvalidClassFile(f"Unsupported Java constant pool tag: {tag}")
        index += 1
    return frozenset(values)


def inspect_jar(jar_path: Path) -> dict:
    with ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        if "plugin.yml" not in names:
            raise ValueError("Missing plugin.yml")
        descriptor = jar.read("plugin.yml").decode("utf-8")
        main = re.search(r"(?m)^main:\s*([a-zA-Z0-9_.]+)\s*$", descriptor)
        if main is None:
            raise ValueError("Missing or invalid plugin main class")
        main_path = main.group(1).replace(".", "/") + ".class"
        if main_path not in names:
            raise ValueError("Declared plugin main class missing from JAR")
        pool = utf8_pool(jar.read(main_path))

        listener = "co/surumene/simplecoins/SettlementAdmissionListener"
        scanner = "co/surumene/simplecoins/domain/SettlementScanRunner"
        checks = {
            "listener_class_present": listener + ".class" in names,
            "main_mentions_listener": any(listener in item for item in pool),
            "main_mentions_scan_or_coordinator": any(
                "SettlementScanRunner" in item or "SettlementStartupCoordinator" in item
                for item in pool
            ),
            "main_mentions_event_registration": "registerEvents" in pool,
        }
        missing = sorted(key for key, value in checks.items() if not value)
        return {
            "kind": "phase6-startup-wiring-static-inventory",
            "main_class": main.group(1),
            "status": "missing" if missing else "candidate-only",
            "checks": checks,
            "missing": missing,
            "limitation": (
                "Constant-pool references are hints only. Even candidate-only does "
                "not prove onEnable execution, listener order, complete settlement, "
                "atomic snapshot publication, crash recovery, or JE/BE behavior."
            ),
        }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        result = inspect_jar(args.jar)
    except (OSError, BadZipFile, ValueError, UnicodeError, struct.error) as exc:
        print(json.dumps({"status": "invalid", "error": str(exc)}, ensure_ascii=False))
        return 3
    print(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2))
    return 0 if result["status"] == "candidate-only" else 2


if __name__ == "__main__":
    sys.exit(main())
