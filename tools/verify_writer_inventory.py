#!/usr/bin/env python3
"""Read-only, point-in-time comparison of installed plugin JARs to an audited snapshot.

Input is the exact JSON produced by audit_writer_surface.py from a pinned
distribution; this is NOT a writer-lock, runtime loaded-plugin inventory,
Paper/OS snapshot, or permission to open the admission guard.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys


def _valid_name(name: object) -> bool:
    return (isinstance(name, str) and name.endswith(".jar")
            and len(name) < 256 and not name.startswith(".")
            and name.strip() == name
            and name not in (".", "..")
            and "/" not in name and "\\" not in name
            and "\x00" not in name)


def reference_roster(path: Path) -> tuple[dict[str, tuple[str, int]], str]:
    """Trust the JSON only as a pinned expected inventory, not as authority."""
    raw = path.read_bytes()
    document = json.loads(raw)
    if not isinstance(document, dict) or (
            document.get("kind") != "STATIC_CANDIDATE_CALL_REFERENCES_ONLY"):
        raise ValueError("Not an audit_writer_surface.py distribution report")
    jars = document.get("jars")
    if not isinstance(jars, list) or not jars:
        raise ValueError("Expected JAR inventory absent")
    expected: dict[str, tuple[str, int]] = {}
    names: set[str] = set()
    for entry in jars:
        if not isinstance(entry, dict):
            raise ValueError("Invalid expected JAR record")
        name, sha, size = (entry.get("jar"), entry.get("sha256"), entry.get("size"))
        if not _valid_name(name):
            raise ValueError("Invalid expected JAR basename")
        if name.casefold() in names:
            raise ValueError("Expected JAR list contains duplicate or case alias")
        names.add(name.casefold())
        if not isinstance(sha, str) or re.fullmatch(r"[0-9a-f]{64}", sha) is None:
            raise ValueError("Invalid expected SHA256")
        if not isinstance(size, int) or isinstance(size, bool) or size <= 0:
            raise ValueError("Invalid expected JAR byte size")
        expected[name] = (sha, size)
    return expected, hashlib.sha256(raw).hexdigest()


def _unchanged_digest(path: Path) -> tuple[str, int]:
    """Never follow symlinked JARs; check same inode and metadata around read."""
    before = path.lstat()
    if not stat.S_ISREG(before.st_mode):
        raise ValueError("Installed JAR is not a regular, nonlinked file")
    flags = os.O_RDONLY | getattr(os, "O_CLOEXEC", 0)
    flags |= getattr(os, "O_NOFOLLOW", 0)
    fd = os.open(path, flags)
    try:
        opened = os.fstat(fd)
        if (not stat.S_ISREG(opened.st_mode)
                or (opened.st_dev, opened.st_ino) != (before.st_dev, before.st_ino)):
            raise ValueError("Installed JAR replaced before reading")
        sha = hashlib.sha256()
        size = 0
        with os.fdopen(fd, "rb", closefd=False) as stream:
            while True:
                data = stream.read(1024 * 1024)
                if not data:
                    break
                sha.update(data)
                size += len(data)
        after_fd = os.fstat(fd)
        after_path = path.lstat()
    finally:
        os.close(fd)
    attrs = ("st_dev", "st_ino", "st_size", "st_mtime_ns", "st_ctime_ns")
    if any(getattr(before, field) != getattr(after_fd, field)
           or getattr(before, field) != getattr(after_path, field)
           for field in attrs):
        raise ValueError("Installed JAR changed during SHA256 read")
    if size != before.st_size:
        raise ValueError("Installed JAR read is incomplete")
    return sha.hexdigest(), size


def verify_installed(reference: Path, installed: Path) -> dict:
    expected, reference_digest = reference_roster(reference)
    root = installed.lstat()
    if not stat.S_ISDIR(root.st_mode):
        raise ValueError("Plugins directory is missing, linked, or not a directory")
    seen: dict[str, str] = {}
    found: dict[str, tuple[str, int]] = {}
    rejects: list[dict[str, str]] = []
    nonjar_entries = 0
    with os.scandir(installed) as entries:
        for entry in entries:
            name = entry.name
            if not name.casefold().endswith(".jar"):
                nonjar_entries += 1
                continue
            if name.casefold() in seen:
                rejects.append({"jar": name, "reason": "casefold_duplicate"})
                continue
            seen[name.casefold()] = name
            if not _valid_name(name):
                rejects.append({"jar": name, "reason": "unsafe_jar_name"})
                continue
            try:
                found[name] = _unchanged_digest(Path(entry.path))
            except (OSError, ValueError) as e:
                rejects.append({"jar": name, "reason": "unreadable_or_unstable",
                                "detail": type(e).__name__})
    missing = sorted(set(expected) - set(found))
    unexpected = sorted(set(found) - set(expected))
    altered = sorted(
        name for name in set(found) & set(expected)
        if found[name] != expected[name]
    )
    good = sorted(set(found) & set(expected) - set(altered))
    status = "match" if not (missing or unexpected or altered or rejects) else "mismatch"
    return {
        "kind": "WRITER_JAR_ROSTER_POINT_IN_TIME_ONLY",
        "status": status,
        "reference_sha256": reference_digest,
        "expected_jar_count": len(expected),
        "observed_top_level_jar_count": len(seen),
        "matching": good,
        "missing": missing,
        "unexpected": unexpected,
        "altered": altered,
        "rejected": rejects,
        "other_top_level_entries_not_scanned": nonjar_entries,
        "caveat": (
            "SHA256/size roster equality only at this read interval. "
            "Does not prove which plugins Paper loads, whether arbitrary "
            "runtime code changes, which JVM writer APIs execute, quiescence "
            "between LIVE/saved observation and commit, or G0/G3/admission. "
            "Never use as a standalone permission to publish or acknowledge."
        ),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference-json", type=Path, required=True)
    parser.add_argument("--installed-plugins", type=Path, required=True)
    parser.add_argument("--report-json", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        result = verify_installed(args.reference_json, args.installed_plugins)
        args.report_json.parent.mkdir(parents=True, exist_ok=True)
        args.report_json.write_text(json.dumps(result, sort_keys=True,
                          indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print("WRITER_PLUGIN_ROSTER_DENIED " + type(error).__name__ + ": " +
              str(error), file=sys.stderr)
        return 2
    if result["status"] != "match":
        print("WRITER_PLUGIN_ROSTER_DENIED " + json.dumps({
            k: result[k] for k in ("missing", "unexpected", "altered", "rejected")
        }, ensure_ascii=False, sort_keys=True))
        return 2
    print(f"WRITER_PLUGIN_ROSTER_MATCHED jars={result['expected_jar_count']}"
          " — only a point-in-time JAR byte comparison")
    print("WRITER_EPOCH_AND_ADMISSION_NOT_PROVEN")
    return 0


if __name__ == "__main__":
    sys.exit(main())
