#!/usr/bin/env python3
"""Read-only, filesystem-only evidence of deployed Paper plugin JAR bytes.

This does not query the Paper plugin manager, BAN providers, or live state.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys
import zipfile

DESCRIPTORS = frozenset({"plugin.yml", "paper-plugin.yml"})


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def inventory(plugin_dir: Path) -> dict:
    if plugin_dir.is_symlink() or not plugin_dir.is_dir():
        raise ValueError("plugins directory must be an existing, non-symlink directory")

    entries = []
    errors = []
    for path in sorted(plugin_dir.iterdir(), key=lambda p: (p.name.casefold(), p.name)):
        if path.suffix.lower() != ".jar":
            continue
        if path.is_symlink():
            errors.append({"jar": path.name, "reason": "symlink_not_followed"})
            continue
        if not path.is_file():
            errors.append({"jar": path.name, "reason": "not_a_regular_file"})
            continue
        try:
            before = path.stat()
            digest = sha256_file(path)
            with zipfile.ZipFile(path) as archive:
                descriptors = sorted(DESCRIPTORS.intersection(archive.namelist()))
            after = path.stat()
            identity = lambda stat: (stat.st_dev, stat.st_ino, stat.st_size, stat.st_mtime_ns)
            if identity(before) != identity(after):
                errors.append({"jar": path.name, "reason": "changed_during_scan"})
                continue
            entries.append({
                "jar": path.name,
                "size_bytes": after.st_size,
                "sha256": digest,
                "descriptors": descriptors,
            })
            if not descriptors:
                errors.append({"jar": path.name, "reason": "no_root_plugin_descriptor"})
        except (OSError, zipfile.BadZipFile, zipfile.LargeZipFile):
            errors.append({"jar": path.name, "reason": "unreadable_or_invalid_jar"})

    return {
        "schema_version": 1,
        "scope": "filesystem-root-jars-only",
        "loaded_plugins_verified": False,
        "scan_complete": not errors,
        "entries": entries,
        "errors": errors,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plugins-dir", required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        result = inventory(args.plugins_dir)
    except (OSError, ValueError) as exc:
        print(f"Plugin JAR inventory failed: {exc}", file=sys.stderr)
        return 2
    print(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2))
    return 0 if result["scan_complete"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
