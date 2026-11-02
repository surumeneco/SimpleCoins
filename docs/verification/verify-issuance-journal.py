#!/usr/bin/env python3
"""Read-only structural check of SimpleCoins' stopped-server issuance journal.

This never reconciles playerdata, inventories or in-memory state, and never
repairs files. Run on a copy made while the server is stopped.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import struct
import sys
import uuid

DENOMINATIONS = frozenset((1, 5, 10, 50, 100, 500, 1000, 5000, 10000))
CANONICAL_RECORD = re.compile(r"[1-9][0-9]*\.grant\Z")


class InvalidJournal(ValueError):
    pass


def _regular_bytes(path: Path, max_payload: int) -> bytes:
    try:
        before = path.lstat()
        if not stat.S_ISREG(before.st_mode):
            raise InvalidJournal(f"non-regular or symbolic record: {path.name}")
        if not 32 <= before.st_size <= max_payload + 32:
            raise InvalidJournal(f"invalid record size: {path.name}")
        flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
        fd = os.open(path, flags)
        try:
            opened = os.fstat(fd)
            if not stat.S_ISREG(opened.st_mode) or (opened.st_dev, opened.st_ino) != (before.st_dev, before.st_ino):
                raise InvalidJournal(f"record changed during open: {path.name}")
            with os.fdopen(fd, "rb", closefd=False) as stream:
                data = stream.read(max_payload + 33)
            after = os.fstat(fd)
        finally:
            os.close(fd)
        if len(data) != before.st_size or (before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns, before.st_ctime_ns) != (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns, after.st_ctime_ns):
            raise InvalidJournal(f"record changed during read: {path.name}")
        digest, payload = data[:32], data[32:]
        if hashlib.sha256(payload).digest() != digest:
            raise InvalidJournal(f"checksum mismatch: {path.name}")
        return payload
    except OSError as error:
        raise InvalidJournal(f"cannot read record {path.name}: {error.strerror}") from error


class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def take(self, count: int) -> bytes:
        if self.pos + count > len(self.data):
            raise InvalidJournal("truncated grant record")
        part = self.data[self.pos:self.pos + count]
        self.pos += count
        return part

    def unpack(self, format_: str):
        return struct.unpack(format_, self.take(struct.calcsize(format_)))[0]

    def utf(self) -> str:
        """Decode Java DataInputStream modified UTF-8 into UTF-16 code units."""
        raw = self.take(self.unpack(">H"))
        units = []
        i = 0
        while i < len(raw):
            b = raw[i]
            if 0 < b < 0x80:
                units.append(b)
                i += 1
            elif 0xC0 <= b <= 0xDF and i + 1 < len(raw) and raw[i + 1] & 0xC0 == 0x80:
                u = ((b & 0x1F) << 6) | (raw[i + 1] & 0x3F)
                if u < 0x80 and u != 0:
                    raise InvalidJournal("noncanonical modified UTF-8")
                units.append(u)
                i += 2
            elif 0xE0 <= b <= 0xEF and i + 2 < len(raw) and all(x & 0xC0 == 0x80 for x in raw[i + 1:i + 3]):
                u = ((b & 0x0F) << 12) | ((raw[i + 1] & 0x3F) << 6) | (raw[i + 2] & 0x3F)
                if u < 0x800:
                    raise InvalidJournal("noncanonical modified UTF-8")
                units.append(u)
                i += 3
            else:
                raise InvalidJournal("invalid modified UTF-8")
        return "".join(chr(unit) for unit in units)


def _grant(data: bytes) -> tuple[int, str, bool]:
    r = Reader(data)
    schema = r.unpack(">I")
    if schema not in (0x53434731, 0x53434732):
        raise InvalidJournal("unsupported grant schema")
    sequence = r.unpack(">q")
    raw_id, raw_player = r.utf(), r.utf()
    for raw in (raw_id, raw_player):
        try:
            if str(uuid.UUID(raw)) != raw:
                raise InvalidJournal("noncanonical grant UUID")
        except (ValueError, AttributeError) as error:
            raise InvalidJournal("invalid grant UUID") from error
    amount, issuer, created = r.unpack(">q"), r.utf(), r.unpack(">q")
    delivered = r.unpack(">B")
    if schema == 0x53434732:
        denomination, count = r.unpack(">i"), r.unpack(">q")
    else:
        denomination, count = 0, 0
    if r.pos != len(data):
        raise InvalidJournal("trailing grant bytes")
    if sequence <= 0 or amount <= 0 or not issuer.strip() or len(issuer) > 1024 or delivered not in (0, 1):
        raise InvalidJournal("invalid grant fields")
    if denomination == 0:
        if count != 0:
            raise InvalidJournal("untyped grant has coin count")
    elif denomination not in DENOMINATIONS or count <= 0 or denomination * count != amount or denomination * count > (1 << 63) - 1:
        raise InvalidJournal("invalid denomination/count")
    return sequence, raw_id, bool(delivered)


def verify(directory: Path) -> dict:
    if not stat.S_ISDIR(directory.lstat().st_mode):
        raise InvalidJournal("journal directory must be a real directory")
    names = list(directory.iterdir())
    records = []
    temporary = 0
    for path in names:
        if path.name.endswith(".grant"):
            if not CANONICAL_RECORD.fullmatch(path.name):
                raise InvalidJournal("noncanonical grant filename")
            records.append(path)
        elif path.name.startswith(".pending-") and path.name.endswith(".tmp"):
            temporary += 1
    records.sort(key=lambda p: int(p.name[:-6]))
    ids = set()
    pending = 0
    for expected, path in enumerate(records, start=1):
        if path.name != f"{expected}.grant":
            raise InvalidJournal("missing or duplicated journal sequence")
        sequence, grant_id, delivered = _grant(_regular_bytes(path, 8192))
        if sequence != expected or grant_id in ids:
            raise InvalidJournal("misnamed/duplicate grant record")
        ids.add(grant_id)
        pending += not delivered
    head_payload = _regular_bytes(directory / "head", 8)
    if len(head_payload) != 8:
        raise InvalidJournal("invalid journal head payload")
    head = struct.unpack(">q", head_payload)[0]
    if head < 0 or head > len(records):
        raise InvalidJournal("journal tail missing or head invalid")
    warnings = []
    if head < len(records):
        warnings.append("head_behind_last_record_recovery_required")
    if temporary:
        warnings.append("pending_temporary_files_present")
    return {"records": len(records), "pending": pending, "head": head, "warnings": warnings,
            "scope": "journal-structure-only; no playerdata or live-delivery proof"}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("journal_dir", type=Path, help="Stopped-server copy of plugins/SimpleCoins/issuance")
    args = parser.parse_args(argv)
    try:
        result = verify(args.journal_dir)
    except (InvalidJournal, OSError) as error:
        print(json.dumps({"status": "invalid", "reason": str(error)}, ensure_ascii=False))
        return 2
    print(json.dumps({"status": "warning" if result["warnings"] else "ok", **result}, ensure_ascii=False))
    return 1 if result["warnings"] else 0


if __name__ == "__main__":
    sys.exit(main())
