#!/usr/bin/env python3
"""Conservative static writer-ENTRYPOINT inventory of packaged Minecraft plugins.

This is a Java class-file constant-pool call-reference audit. A reference is NOT
evidence that the call executes. Absence of a reference is NOT evidence of no
writes: reflection, dynamic linkage, inherited calls, native code and other
JARs can bypass the scan. Never use its report to unlock admissions or ACK.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import struct
import sys
import zipfile

# These are representative call-sites, not an exhaustive definition of a writer.
CATEGORIES = (
    "PLAYERDATA_SAVE",
    "STORED_FILE_WRITE",
    "INVENTORY_WRITE",
    "PDC_WRITE",
    "WORLD_WRITE",
    "NMS_MUTATION",
    "ASYNC_ENTRY",
    "DYNAMIC_DISPATCH",
)


def _u2(data: bytes, at: int) -> int:
    if at + 2 > len(data):
        raise ValueError("Truncated classfile u2")
    return struct.unpack_from(">H", data, at)[0]


def class_callrefs(data: bytes) -> set[tuple[str, str, str]]:
    """Read constant-pool Methodref/InterfaceMethodref, with strict bounds.

    Does not disassemble instructions. Some refs may be dead or symbolic.
    """
    if len(data) < 10 or data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("Not a JVM classfile")
    count = _u2(data, 8)
    cp: list[object | None] = [None] * count
    at = 10
    index = 1
    while index < count:
        if at >= len(data):
            raise ValueError("Truncated classfile constant pool")
        tag = data[at]
        at += 1
        if tag == 1:
            size = _u2(data, at)
            at += 2
            if at + size > len(data):
                raise ValueError("Truncated UTF8 constant")
            cp[index] = (tag, data[at:at + size].decode("utf-8", "replace"))
            at += size
        elif tag in (3, 4):
            if at + 4 > len(data):
                raise ValueError("Truncated number constant")
            at += 4
            cp[index] = (tag,)
        elif tag in (5, 6):
            if at + 8 > len(data) or index + 1 >= count:
                raise ValueError("Truncated wide number constant")
            at += 8
            cp[index] = (tag,)
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[index] = (tag, _u2(data, at))
            at += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[index] = (tag, _u2(data, at), _u2(data, at + 2))
            at += 4
        elif tag == 15:
            if at + 3 > len(data):
                raise ValueError("Truncated method handle")
            cp[index] = (tag, data[at], _u2(data, at + 1))
            at += 3
        else:
            raise ValueError(f"Unrecognized Java constant tag {tag}")
        index += 1

    def entry(i: int, tag: int) -> tuple:
        if i < 1 or i >= count or cp[i] is None or cp[i][0] != tag:
            raise ValueError(f"Invalid Java constant-pool reference {i}, expected tag {tag}")
        return cp[i]  # type: ignore[return-value]

    refs: set[tuple[str, str, str]] = set()
    for value in cp[1:]:
        if value is None or value[0] not in (10, 11):
            continue
        class_name = entry(value[1], 7)[1]
        owner = entry(class_name, 1)[1]
        pair = entry(value[2], 12)
        name = entry(pair[1], 1)[1]
        descriptor = entry(pair[2], 1)[1]
        refs.add((owner, name, descriptor))
    return refs


def classify(owner: str, name: str) -> set[str]:
    result: set[str] = set()
    if name == "saveData" and (
            "CraftPlayer" in owner or owner.endswith("/Player")):
        result.add("PLAYERDATA_SAVE")
    if owner.endswith("/PlayerDataStorage") and name.startswith(("save", "write")):
        result.add("PLAYERDATA_SAVE")
    if (owner == "java/nio/file/Files"
            and name.startswith(("write", "move", "copy", "delete", "create",
                                 "newOutputStream", "newByteChannel",
                                 "setAttribute"))):
        result.add("STORED_FILE_WRITE")
    if owner in ("java/io/FileOutputStream", "java/io/RandomAccessFile") \
            and name == "<init>":
        result.add("STORED_FILE_WRITE")
    if owner == "java/nio/channels/FileChannel" and name in ("open", "write", "truncate"):
        result.add("STORED_FILE_WRITE")
    if owner == "java/io/File" and name in ("renameTo", "delete", "createNewFile"):
        result.add("STORED_FILE_WRITE")
    if owner.startswith("org/bukkit/inventory/") and name.startswith((
            "setItem", "setContents", "setStorageContents", "setAmount",
            "addItem", "removeItem", "remove", "clear", "setMaxStackSize")):
        result.add("INVENTORY_WRITE")
    if owner.startswith("org/bukkit/persistence/") \
            and name in ("set", "remove"):
        result.add("PDC_WRITE")
    if owner.startswith("org/bukkit/inventory/meta/") \
            and name in ("setCustomModelData", "setItemModel", "setLore", "setDisplayName"):
        result.add("PDC_WRITE")
    if (owner.startswith(("org/bukkit/block/", "org/bukkit/World",
                          "org/bukkit/entity/")) and name in (
            "setType", "setBlockData", "setItemStack", "update",
            "dropItem", "dropItemNaturally", "spawn", "remove",
            "setMetadata", "setPersistent", "setVelocity")):
        result.add("WORLD_WRITE")
    if owner.startswith("net/minecraft/") and name.startswith((
            "setItem", "setBlock", "setChanged", "save", "removeItem", "addFreshEntity")):
        result.add("NMS_MUTATION")
    if ("Scheduler" in owner or "BukkitScheduler" in owner) \
            and ("Async" in name or "async" in name):
        result.add("ASYNC_ENTRY")
    if owner.startswith("java/util/concurrent/") and name in (
            "execute", "submit", "runAsync", "supplyAsync", "schedule"):
        result.add("ASYNC_ENTRY")
    if owner in ("java/lang/reflect/Method", "java/lang/reflect/Constructor",
                 "java/lang/invoke/MethodHandle") and name.startswith((
            "invoke", "newInstance")):
        result.add("DYNAMIC_DISPATCH")
    return result


def audit_jar(path: Path, sample_limit: int = 12) -> dict:
    jar_bytes = path.read_bytes()
    counts: dict[str, int] = {key: 0 for key in CATEGORIES}
    examples: dict[str, list[dict[str, str]]] = {key: [] for key in CATEGORIES}
    inspected = 0
    all_ref_count = 0
    with zipfile.ZipFile(path) as jar:
        members = [entry for entry in jar.infolist() if entry.filename.endswith(".class")]
        seen = set()
        for member in members:
            if member.filename in seen:
                raise ValueError(f"{path}: duplicate class entry {member.filename}")
            seen.add(member.filename)
            inspected += 1
            try:
                calls = class_callrefs(jar.read(member))
            except ValueError as exc:
                raise ValueError(f"{path}!{member.filename}: {exc}") from exc
            all_ref_count += len(calls)
            for owner, name, descriptor in sorted(calls):
                for category in classify(owner, name):
                    counts[category] += 1
                    if len(examples[category]) < sample_limit:
                        examples[category].append({
                            "class": member.filename,
                            "owner": owner,
                            "method": name,
                            "descriptor": descriptor,
                        })
    return {
        "jar": path.name,
        "sha256": hashlib.sha256(jar_bytes).hexdigest(),
        "size": len(jar_bytes),
        "classes": inspected,
        "method_references": all_ref_count,
        "categories": counts,
        "examples": {k: v for k, v in examples.items() if v},
    }


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--plugins", type=Path, required=True,
                   help="Directory containing actual packaged plugin JARs")
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--max-examples", type=int, default=12)
    p.add_argument("--require", action="append", default=[],
                   help="JAR basename that must be present in the distribution")
    args = p.parse_args(argv)
    if args.max_examples < 0 or args.max_examples > 100:
        p.error("--max-examples must be in 0..100")
    jars = sorted(args.plugins.glob("*.jar"))
    if not jars:
        raise ValueError("No packaged plugin JARs; cannot claim source enumeration")
    found = {jar.name for jar in jars}
    missing = sorted(set(args.require) - found)
    if missing:
        raise ValueError(f"Missing mandatory deployed plugin JARs: {missing}")
    report = {
        "kind": "STATIC_CANDIDATE_CALL_REFERENCES_ONLY",
        "caveat": (
            "Not reachable-code analysis; not full owner/writer discovery; "
            "not frozen LIVE/saved generation, G0, G3 or CAT approval. "
            "Reflection, dynamic/native calls, runtime plugins and Paper itself "
            "may bypass this inventory."
        ),
        "jars": [audit_jar(jar, args.max_examples) for jar in jars],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False,
                                      sort_keys=True, indent=2) + "\n",
                           encoding="utf-8")
    print("WRITER_SURFACE_REPORT jars=" + str(len(report["jars"]))
          + " classes=" + str(sum(j["classes"] for j in report["jars"])))
    for jar in report["jars"]:
        groups = ",".join(f"{k}:{v}" for k, v in jar["categories"].items() if v)
        print(f"JAR {jar['jar']} classes={jar['classes']} refs={jar['method_references']} "
              f"candidate_calls={groups or 'none'}")
    print("STATIC_CALL_REFERENCES_ARE_NOT_WRITER_COMPLETENESS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
