"""Code-to-spec Red: metadata equality and ZIP listings do not prove JAR integrity."""
import hashlib
import io
import os
import struct
from pathlib import Path
from unittest.mock import patch
import tempfile
import unittest
import zipfile

import phase5_plugin_jar_inventory as subject


class PluginJarMetadataCoherenceTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.path = self.directory / "same.jar"
        self.original = self._jar_bytes("plugin.yml", b"x" * 24)
        self.replacement = self._jar_bytes("paper-plugin.yml", b"y" * 12)
        self.assertEqual(len(self.original), len(self.replacement))
        self.original_hash = hashlib.sha256(self.original).hexdigest()
        self.replacement_hash = hashlib.sha256(self.replacement).hexdigest()
        self.assertNotEqual(self.original_hash, self.replacement_hash)
        self.path.write_bytes(self.original)

    @staticmethod
    def _jar_bytes(descriptor, payload):
        output = io.BytesIO()
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr(descriptor, payload)
        return output.getvalue()

    def _assert_coherent(self, result):
        valid_pairs = {
            (self.original_hash, ("plugin.yml",)),
            (self.replacement_hash, ("paper-plugin.yml",)),
        }
        observed = [(entry["sha256"], tuple(entry["descriptors"])) for entry in result["entries"]]
        self.assertTrue(all(pair in valid_pairs for pair in observed),
                        "SHA-256 and descriptor must represent the same JAR byte version")
        if result["scan_complete"]:
            self.assertEqual(1, len(observed), "complete scan must contain the JAR")

    def test_unchanged_same_size_jar_is_complete(self):
        result = subject.inventory(self.directory)
        self.assertTrue(result["scan_complete"])
        self.assertEqual(self.original_hash, result["entries"][0]["sha256"])
        self._assert_coherent(result)

    def test_same_size_restored_mtime_rewrite_is_not_complete(self):
        before = os.stat(self.path)
        original_sha_stream = subject.sha256_stream
        mutated = False

        def rewrite_after_hash(stream):
            nonlocal mutated
            digest = original_sha_stream(stream)
            with self.path.open("r+b") as target:
                target.write(self.replacement)
                target.flush()
                os.fsync(target.fileno())
            os.utime(self.path, ns=(before.st_atime_ns, before.st_mtime_ns))
            mutated = True
            return digest

        with patch.object(subject, "sha256_stream", side_effect=rewrite_after_hash):
            try:
                result = subject.inventory(self.directory)
            except (OSError, ValueError):
                result = None  # An explicit incomplete/error verdict is valid.
        after = os.stat(self.path)
        self.assertTrue(mutated, "mutation hook must execute")
        self.assertEqual((before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns),
                         (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns))
        if result is not None:
            self._assert_coherent(result)

    def test_root_descriptor_crc_mismatch_is_incomplete(self):
        data = bytearray(self.original)
        central_offset = struct.unpack_from("<I", data, len(data) - 6)[0]
        data[central_offset + 16] ^= 1
        self.path.write_bytes(data)
        with zipfile.ZipFile(self.path) as archive:
            with self.assertRaises(zipfile.BadZipFile):
                archive.read("plugin.yml")
        result = subject.inventory(self.directory)
        self.assertFalse(result["scan_complete"], "CRC-invalid descriptor must not be attested")
        self.assertIn("unreadable_or_invalid_jar", [e["reason"] for e in result["errors"])


if __name__ == "__main__":
    unittest.main()
