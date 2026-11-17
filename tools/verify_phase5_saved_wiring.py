#!/usr/bin/env python3
"""Conservative negative-evidence inventory of saved-asset read paths in a SimpleCoins JAR.

A candidate-only result proves neither runtime invocation nor complete saved/live
coverage or an authoritative accounting snapshot. Do not use this tool as an
acceptance or CAT Gate.
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
    """The inspected JVM constant pool is truncated or structurally invalid."""


def utf8_pool(payload: bytes) -> frozenset[str]:
    if len(payload) < 10 or payload[:4] != b'\xca\xfe\xba\xbe':
        raise InvalidClassFile('Invalid class-file header')
    count = struct.unpack_from('>H', payload, 8)[0]
    if count < 1:
        raise InvalidClassFile('Invalid zero constant-pool count')
    offset = 10
    entries: set[str] = set()
    index = 1

    def take(size: int) -> bytes:
        nonlocal offset
        if offset + size > len(payload):
            raise InvalidClassFile('Truncated class-file constant pool')
        value = payload[offset:offset + size]
        offset += size
        return value

    while index < count:
        tag = take(1)[0]
        if tag == 1:
            size = struct.unpack('>H', take(2))[0]
            raw = take(size)
            if raw.isascii():
                entries.add(raw.decode('ascii'))
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            take(4)
        elif tag in (5, 6):
            take(8)
            # JVMS §4.4.5: long/double also occupies the following CP slot.
            if index + 1 >= count:
                raise InvalidClassFile('Wide constant exceeds constant-pool range')
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            take(2)
        elif tag == 15:
            take(3)
        else:
            raise InvalidClassFile(f'Unsupported constant-pool tag: {tag}')
        index += 1
    return frozenset(entries)



def class_references(payload: bytes) -> frozenset[str]:
    """Resolve only tagged JVM CONSTANT_Class entries, never arbitrary UTF8 hints.

    A string literal or debug log can contain a class path but does not establish
    a bytecode class reference. Reference names are exact JVM internal names;
    descriptors and similarly prefixed names are not counted as references.
    This is still a static hint, not proof of invocation.
    """
    if len(payload) < 10 or payload[:4] != b'\xca\xfe\xba\xbe':
        raise InvalidClassFile('Invalid class-file header')
    count = struct.unpack_from('>H', payload, 8)[0]
    if count < 1:
        raise InvalidClassFile('Invalid zero constant-pool count')
    offset = 10
    index = 1
    strings: dict[int, str | None] = {}
    classes: list[int] = []

    def take(size: int) -> bytes:
        nonlocal offset
        if offset + size > len(payload):
            raise InvalidClassFile('Truncated class-file constant pool')
        chunk = payload[offset:offset + size]
        offset += size
        return chunk

    while index < count:
        tag = take(1)[0]
        if tag == 1:
            length = struct.unpack('>H', take(2))[0]
            raw = take(length)
            strings[index] = raw.decode('ascii') if raw.isascii() else None
        elif tag == 7:
            classes.append(struct.unpack('>H', take(2))[0])
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            take(4)
        elif tag in (5, 6):
            take(8)
            # JVMS §4.4.5: long/double also occupies the following CP slot.
            if index + 1 >= count:
                raise InvalidClassFile('Wide constant exceeds constant-pool range')
            index += 1
        elif tag in (8, 16, 19, 20):
            take(2)
        elif tag == 15:
            take(3)
        else:
            raise InvalidClassFile(f'Unsupported constant-pool tag: {tag}')
        index += 1

    result: set[str] = set()
    for class_index in classes:
        if class_index not in strings:
            raise InvalidClassFile('CONSTANT_Class has invalid Utf8 name index')
        name = strings[class_index]
        if name is not None:
            result.add(name)
    return frozenset(result)


DOMAIN = 'co/surumene/simplecoins/domain/'
REQUIRED_CLASSES = (
    'WorldStorageDiscovery', 'WorldSnapshotTraversal', 'AnvilRegionReader',
    'PlayerDataReader', 'SettlementScanRunner', 'SettlementAccumulator',
    'PlayerDataSettlementImport', 'BlockEntitySettlementImport',
    'FreeEntitySettlementImport', 'ResolvedEntitySettlementImport',
    'SavedEntityUuidIntegrity',
)
SCAN_REFERENCES = (
    'WorldSnapshotTraversal', 'SettlementAccumulator', 'PlayerDataSettlementImport',
    'BlockEntitySettlementImport', 'FreeEntitySettlementImport',
    'ResolvedEntitySettlementImport',
)
TRAVERSAL_REFERENCES = (
    'AnvilRegionReader', 'PlayerDataReader', 'SavedEntityUuidIntegrity',
)


def inspect_jar(path: Path) -> dict:
    with ZipFile(path) as jar:
        entries = jar.namelist()
        names = set(entries)
        if len(entries) != len(names):
            raise ValueError('Duplicate ZIP member path')
        if 'plugin.yml' not in names:
            raise ValueError('Missing plugin.yml')
        descriptor = jar.read('plugin.yml').decode('utf-8')
        main = re.search(r'(?m)^main:\s*([a-zA-Z0-9_.]+)\s*$', descriptor)
        if main is None:
            raise ValueError('Missing or invalid main plugin class')
        main_class = main.group(1).replace('.', '/') + '.class'
        if main_class not in names:
            raise ValueError('Declared main plugin class absent')
        main_class_refs = class_references(jar.read(main_class))
        pools: dict[str, frozenset[str]] = {}
        references: dict[str, frozenset[str]] = {}
        for name in REQUIRED_CLASSES:
            class_path = DOMAIN + name + '.class'
            if class_path in names:
                class_bytes = jar.read(class_path)
                pools[name] = utf8_pool(class_bytes)
                references[name] = class_references(class_bytes)

        # Javac may place visitor calls in any numbered anonymous inner class.
        # Looking only at $1 silently misses a scanner implemented in $2 or later.
        scan_classes = references.get('SettlementScanRunner', frozenset())
        inner_prefix = DOMAIN + 'SettlementScanRunner$'
        for inner_path in sorted(names):
            if (inner_path.startswith(inner_prefix)
                    and inner_path.endswith('.class')
                    and inner_path[len(inner_prefix):-len('.class')].isdigit()):
                scan_classes = scan_classes | class_references(jar.read(inner_path))

        checks: dict[str, bool] = {
            'main:SettlementScanRunner': DOMAIN + 'SettlementScanRunner' in main_class_refs,
        }
        checks.update({f'class:{name}': name in pools for name in REQUIRED_CLASSES})
        checks.update({f'scanner:{name}': DOMAIN + name in scan_classes
                       for name in SCAN_REFERENCES})
        traversal_classes = references.get('WorldSnapshotTraversal', frozenset())
        checks.update({f'traversal:{name}': DOMAIN + name in traversal_classes
                       for name in TRAVERSAL_REFERENCES})
        discovery_pool = pools.get('WorldStorageDiscovery', frozenset())
        checks['discovery:requiredSourceIds'] = 'requiredSourceIds' in discovery_pool
        checks['discovery:discover'] = 'discover' in discovery_pool
        missing = sorted(name for name, found in checks.items() if not found)
        return {
            'kind': 'phase5-saved-wiring-static-inventory',
            'main_class': main.group(1),
            'status': 'missing' if missing else 'candidate-only',
            'missing': missing,
            'checks': checks,
            'limitation': (
                'Constant-pool hints do not prove invocation, correct source roots, '
                'NBT decoder completeness, saved/live deduplication, ownership, '
                'consistent snapshot, CAT Gates, or Paper/JE/BE acceptance.'
            ),
        }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        report = inspect_jar(args.jar)
    except (OSError, BadZipFile, ValueError, UnicodeError, struct.error) as exc:
        print(json.dumps({'kind': 'phase5-saved-wiring-static-inventory',
                          'status': 'invalid', 'error': str(exc)}, ensure_ascii=False))
        return 3
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    return 2 if report['status'] == 'missing' else 0


if __name__ == '__main__':
    sys.exit(main())
