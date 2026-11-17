#!/usr/bin/env python3
"""Deterministic synthetic JVM constant-pool and ZIP inventory tests."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import struct
import subprocess
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

    def test_real_javac_file_writer_entrypoints_are_not_silent(self) -> None:
        """Actual compiled JVM bytecode, not an invented symbolic method ref."""
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            source = root / "ActualWritableSources.java"
            source.write_text("""
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
final class ActualWritableSources {
    static void writerReferences(Path path) throws Exception {
        try (var text = Files.newBufferedWriter(path)) { text.write("one"); }
        try (var text = new FileWriter(path.toFile())) { text.write("two"); }
        try (var channel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE)) { }
        try (var channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            channel.map(FileChannel.MapMode.READ_WRITE, 0, 4096);
            channel.transferFrom(Channels.newChannel(
                    new ByteArrayInputStream(new byte[0])), 0, 0);
        }
        try (var read = Files.newBufferedReader(path)) { }
    }
}
""", encoding="utf-8")
            subprocess.run(["javac", str(source)], cwd=root,
                           check=True, timeout=30, capture_output=True, text=True)
            actual = audit.class_callrefs(
                (root / "ActualWritableSources.class").read_bytes())
            expected_writer_refs = {
                ("java/nio/file/Files", "newBufferedWriter"),
                ("java/io/FileWriter", "<init>"),
                ("java/nio/channels/AsynchronousFileChannel", "open"),
                ("java/nio/channels/FileChannel", "map"),
                ("java/nio/channels/FileChannel", "transferFrom"),
            }
            for owner, name in expected_writer_refs:
                with self.subTest(owner=owner, method=name):
                    self.assertTrue(
                        any(o == owner and n == name for o, n, _ in actual),
                        "The real javac output lacks the expected call ref")
                    self.assertIn(
                        "STORED_FILE_WRITE", audit.classify(owner, name),
                        "A real Java file-writing entrypoint escaped classification")
            self.assertTrue(any(
                o == "java/nio/file/Files" and n == "newBufferedReader"
                for o, n, _ in actual), "Control read-only call was not emitted")
            self.assertNotIn(
                "STORED_FILE_WRITE",
                audit.classify("java/nio/file/Files", "newBufferedReader"),
                "Read-only NIO operation must not be called a file write")
            print("A01_REAL_JAVAC_FILE_WRITER_API_GAPS_CLASSIFIED_NOT_RUNTIME_QUISCENCE")

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
