#!/usr/bin/env python3
"""Read-only filesystem evidence for deployed Paper plugin JAR bytes.

This does not query Paper's plugin manager, BAN providers, or live state.
A copied byte stream keeps the reported hash and descriptors consistent, but
cannot prove that a concurrently modified source had an atomic filesystem snapshot.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import stat
import sys
import tempfile
import zipfile
from pathlib import Path

DESCRIPTORS = frozenset({"plugin.yml", "paper-plugin.yml"})


def sha256_stream(stream) -> str:
    digest = hashlib.sha256()
    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
        digest.update(chunk)
    return digest.hexdigest()


def _open_directory_no_symlinks(directory: Path) -> int:
    """Open each path component without following a directory symlink."""
    if not getattr(os, "O_NOFOLLOW", 0) or not getattr(os, "O_DIRECTORY", 0):
        raise ValueError("directory-descriptor and no-follow support required")
    path = Path(directory)
    flags = os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW | getattr(os, "O_CLOEXEC", 0)
    current = os.open("/" if path.is_absolute() else ".", flags)
    try:
        for part in path.parts:
            if part in ("/", "."):
                continue
            if part == "..":
                raise ValueError("parent traversal is not accepted")
            following = os.open(part, flags, dir_fd=current)
            os.close(current)
            current = following
        return current
    except BaseException:
        os.close(current)
        raise


def _identity(st):
    return (st.st_dev, st.st_ino, st.st_size, st.st_mtime_ns, st.st_ctime_ns)


def inventory(plugin_dir: Path) -> dict:
    try:
        directory_fd = _open_directory_no_symlinks(plugin_dir)
    except (OSError, ValueError) as exc:
        raise ValueError("plugins directory must be an existing, non-symlink directory") from exc

    entries = []
    errors = []
    try:
        directory_before = os.fstat(directory_fd)
        with os.scandir(directory_fd) as scanner:
            candidates = sorted(scanner, key=lambda e: (e.name.casefold(), e.name))
        for candidate in candidates:
            name = candidate.name
            if not name.lower().endswith(".jar"):
                continue
            if candidate.is_symlink():
                errors.append({"jar": name, "reason": "symlink_not_followed"})
                continue
            if not candidate.is_file(follow_symlinks=False):
                errors.append({"jar": name, "reason": "not_a_regular_file"})
                continue
            try:
                flags = os.O_RDONLY | os.O_NOFOLLOW | getattr(os, "O_CLOEXEC", 0)
                fd = os.open(name, flags, dir_fd=directory_fd)
                with os.fdopen(fd, "rb") as source:
                    before = os.fstat(source.fileno())
                    if not stat.S_ISREG(before.st_mode):
                        errors.append({"jar": name, "reason": "not_a_regular_file"})
                        continue
                    if before.st_ino != candidate.inode():
                        errors.append({"jar": name, "reason": "changed_during_scan"})
                        continue
                    # Digest and ZIP descriptor inspection use the same immutable
                    # copy, not two reads of a mutable source descriptor.
                    with tempfile.SpooledTemporaryFile(max_size=8 * 1024 * 1024, mode="w+b") as snapshot:
                        while True:
                            chunk = source.read(1024 * 1024)
                            if not chunk:
                                break
                            snapshot.write(chunk)
                        snapshot.seek(0)
                        digest = sha256_stream(snapshot)
                        snapshot.seek(0)
                        with zipfile.ZipFile(snapshot) as archive:
                            descriptors = sorted(DESCRIPTORS.intersection(archive.namelist()))
                    after = os.fstat(source.fileno())
                if _identity(before) != _identity(after):
                    errors.append({"jar": name, "reason": "changed_during_scan"})
                    continue
                entries.append({
                    "jar": name,
                    "size_bytes": after.st_size,
                    "sha256": digest,
                    "descriptors": descriptors,
                })
                if not descriptors:
                    errors.append({"jar": name, "reason": "no_root_plugin_descriptor"})
            except (OSError, zipfile.BadZipFile, zipfile.LargeZipFile):
                errors.append({"jar": name, "reason": "unreadable_or_invalid_jar"})
        try:
            path_after = os.stat(plugin_dir, follow_symlinks=False)
            if not stat.S_ISDIR(path_after.st_mode) or (
                path_after.st_dev, path_after.st_ino
            ) != (directory_before.st_dev, directory_before.st_ino):
                errors.append({"jar": None, "reason": "directory_changed_during_scan"})
        except OSError:
            errors.append({"jar": None, "reason": "directory_changed_during_scan"})
    finally:
        os.close(directory_fd)

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
