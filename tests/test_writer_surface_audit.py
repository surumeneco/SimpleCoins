#!/usr/bin/env python3
"""Deterministic synthetic JVM constant-pool and ZIP inventory tests."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile

SCRIPT = (Path(__file__).resolve().parents[1] / "tools/audit_writer_surface.py")
spec = importlib.util.spec_from_file_location("writer_audit", SCRIPT)
assert spec and spec.loader
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


def u2(x: int) -> bytes:
    return struct.pack(">H", x)


def synthetic_class(owner: str, method: str, descriptor: str = "()V",
                    interface: bool = True) -> bytes:
    # Include UTF8, Class, NameAndType and a (Interface)Methodref.
    names = [owner, method, descriptor]
    cp = (
        b"\x01" + u2(len(names[0])) + names[0].encode() +
        b"\x07" + u2(1) +
        b"\x01" + u2(len(names[1])) + names[1].encode() +
        b"\x01" + u2(len(names[2])) + names[2].encode() +
        b"\x0c" + u2(3) + u2(4) +
        (b"\x0b" if interface else b"\x0a") + u2(2) + u2(5)
    )
    return b"\xca\xfe\xba\xbe" + u2(0) + u2(65) + u2(7) + cp


class AuditWriterSurfaceTest(unittest.TestCase):
    def test_method_reference_and_expected_categorization(self) -> None:
        matrix = [
            ("org/bukkit/inventory/Inventory", "setItem", "INVENTORY_WRITE"),
            ("org/bukkit/persistence/PersistentDataContainer", "set", "PDC_WRITE"),
            ("org/bukkit/block/BlockState", "update", "WORLD_WRITE"),
            ("java/nio/file/Files", "move", "STORED_FILE_WRITE"),
            ("net/minecraft/world/level/storage/PlayerDataStorage", "save",
             "PLAYERDATA_SAVE"),
            ("java/lang/reflect/Method", "invoke", "DYNAMIC_DISPATCH"),
            ("java/util/concurrent/CompletableFuture", "runAsync", "ASYNC_ENTRY"),
        ]
        for owner, method, expected in matrix:
            with self.subTest(owner=owner, method=method):
                refs = audit.class_callrefs(synthetic_class(owner, method))
                self.assertEqual({(owner, method, "()V")}, refs)
                self.assertIn(expected, audit.classify(owner, method))

    def test_invalid_or_truncated_classfile_is_not_silent_zero(self) -> None:
        good = synthetic_class("java/nio/file/Files", "write")
        for malformed in (b"", good[:2], good[:-1],
                          good[:8] + u2(255) + good[10:]):
            with self.subTest(size=len(malformed)):
                with self.assertRaises(ValueError):
                    audit.class_callrefs(malformed)
        good_ref = bytearray(good)
        good_ref[-4:-2] = u2(123)  # invalid owner constant ref
        with self.assertRaises(ValueError):
            audit.class_callrefs(bytes(good_ref))

    def test_jar_audit_measures_real_zip_bytes_without_executing_methods(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            jar = root / "PluginA.jar"
            with zipfile.ZipFile(jar, "w") as out:
                out.writestr("example/Test.class",
                             synthetic_class("org/bukkit/inventory/Inventory", "setItem"))
                out.writestr("example/Next.class",
                             synthetic_class("java/lang/reflect/Method", "invoke"))
            report = audit.audit_jar(jar, sample_limit=1)
            self.assertEqual("PluginA.jar", report["jar"])
            self.assertEqual(2, report["classes"])
            self.assertEqual(1, report["categories"]["INVENTORY_WRITE"])
            self.assertEqual(1, report["categories"]["DYNAMIC_DISPATCH"])
            self.assertEqual(0, report["categories"]["PLAYERDATA_SAVE"])
            self.assertEqual(64, len(report["sha256"]))
            self.assertEqual(1, len(report["examples"]["INVENTORY_WRITE"]))
            self.assertEqual(2, report["method_references"])

    def test_zip_duplicate_class_names_are_not_ignored(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            jar = Path(td) / "dupe.jar"
            import warnings
            with zipfile.ZipFile(jar, "w") as out:
                out.writestr("a/A.class", synthetic_class("java/nio/file/Files", "write"))
                with warnings.catch_warnings():
                    warnings.simplefilter("ignore", UserWarning)
                    out.writestr("a/A.class", synthetic_class("java/nio/file/Files", "write"))
            with self.assertRaisesRegex(ValueError, "duplicate class"):
                audit.audit_jar(jar)

    def test_empty_and_missing_required_distribution_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            out = root / "out.json"
            with self.assertRaisesRegex(ValueError, "No packaged"):
                audit.main(["--plugins", td, "--output", str(out)])
            with zipfile.ZipFile(root / "one.jar", "w") as z:
                z.writestr("example/Test.class",
                           synthetic_class("java/nio/file/Files", "write"))
            with self.assertRaisesRegex(ValueError, "Missing mandatory"):
                audit.main(["--plugins", td, "--output", str(out),
                            "--require", "needed.jar"])
            self.assertEqual(0, audit.main(["--plugins", td, "--output", str(out),
                                            "--require", "one.jar"]))
            self.assertIn("STATIC_CANDIDATE_CALL_REFERENCES_ONLY",
                          out.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
