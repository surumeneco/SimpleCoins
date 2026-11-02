#!/usr/bin/env python3
"""Read-only, filesystem-only evidence of deployed Paper plugin JAR bytes.

This does not query the Paper plugin manager, BAN providers, or live state.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import stat
from pathlib import Path
import sys
import zipfile

DESCRIPTORS = frozenset({"plugin.yml", "paper-plugin.yml"})


def sha256_stream(stream) -> str:
    digest = hashlib.sha256()
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
            # Open once with O_NOFOLLOW so a symlink swap cannot redirect the
            # hash or descriptor read to another file after the initial check.
            flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
            fd = os.open(path, flags)
            with os.fdopen(fd, "rb") as stream:
                before = os.fstat(stream.fileno())
                if not stat.S_ISREG(before.st_mode):
                    errors.append({"jar": path.name, "reason": "not_a_regular_file"})
                    continue
                digest = sha256_stream(stream)
                stream.seek(0)
                with zipfile.ZipFile(stream) as archive:
                    descriptors = sorted(DESCRIPTORS.intersection(archive.namelist()))
                after = os.fstat(stream.fileno())
            identity = lambda st: (st.st_dev, st.st_ino, st.st_size, st.st_mtime_ns)
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
