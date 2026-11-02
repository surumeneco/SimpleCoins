"""Deterministic offline verification for the Phase 5 plugin JAR inventory."""

import hashlib
from pathlib import Path
from unittest.mock import patch
import tempfile
import unittest
import zipfile

from phase5_plugin_jar_inventory import inventory, main, sha256_stream


class PluginJarInventoryTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)

    def jar(self, name, descriptor=None):
        path = self.directory / name
        with zipfile.ZipFile(path, "w") as archive:
            if descriptor:
                archive.writestr(descriptor, "name: Example\nversion: '1.0'\n")
        return path

    def test_sorted_inventory_and_exact_bytes_sha256(self):
        later = self.jar("z.jar", "paper-plugin.yml")
        earlier = self.jar("A.JAR", "plugin.yml")
        (self.directory / "notes.txt").write_text("ignored", encoding="utf-8")
        result = inventory(self.directory)
        self.assertTrue(result["scan_complete"])
        self.assertFalse(result["loaded_plugins_verified"])
        self.assertEqual(["A.JAR", "z.jar"], [e["jar"] for e in result["entries"]])
        self.assertEqual(hashlib.sha256(earlier.read_bytes()).hexdigest(), result["entries"][0]["sha256"])
        self.assertEqual(hashlib.sha256(later.read_bytes()).hexdigest(), result["entries"][1]["sha256"])
        self.assertEqual(["plugin.yml"], result["entries"][0]["descriptors"])
        self.assertEqual(["paper-plugin.yml"], result["entries"][1]["descriptors"])
        self.assertEqual(result, inventory(self.directory))

    def test_no_descriptor_is_explicitly_incomplete(self):
        self.jar("library.jar")
        result = inventory(self.directory)
        self.assertFalse(result["scan_complete"])
        self.assertEqual("no_root_plugin_descriptor", result["errors"][0]["reason"])
        self.assertEqual(1, len(result["entries"]))
        self.assertEqual(2, main(["--plugins-dir", str(self.directory)]))

    def test_corrupt_jar_is_not_silently_ignored(self):
        (self.directory / "bad.jar").write_bytes(b"not a zip")
        result = inventory(self.directory)
        self.assertFalse(result["scan_complete"])
        self.assertEqual([], result["entries"])
        self.assertEqual("unreadable_or_invalid_jar", result["errors"][0]["reason"])

    def test_symlink_jar_is_not_followed(self):
        actual = self.jar("real.jar", "plugin.yml")
        (self.directory / "link.jar").symlink_to(actual)
        result = inventory(self.directory)
        self.assertFalse(result["scan_complete"])
        self.assertEqual(["real.jar"], [e["jar"] for e in result["entries"]])
        self.assertEqual("symlink_not_followed", result["errors"][0]["reason"])

    def test_modified_jar_during_scan_is_rejected(self):
        path = self.jar("changing.jar", "plugin.yml")

        def mutate_after_hash(stream):
            digest = sha256_stream(stream)
            with zipfile.ZipFile(path, "a") as archive:
                archive.writestr("changed.txt", "new bytes")
            return digest

        with patch("phase5_plugin_jar_inventory.sha256_stream", side_effect=mutate_after_hash):
            result = inventory(self.directory)
        self.assertFalse(result["scan_complete"])
        self.assertEqual([], result["entries"])
        self.assertEqual("changed_during_scan", result["errors"][0]["reason"])

    @unittest.skipUnless(getattr(__import__("os"), "O_NOFOLLOW", 0), "Linux O_NOFOLLOW required")
    def test_symlink_swap_at_open_is_rejected(self):
        import os
        original = self.jar("swap.jar", "plugin.yml")
        outside = self.directory / "other.bin"
        outside.write_bytes(b"not a plugin")
        real_open = os.open

        swapped = False

        def replace_with_link(path, flags, *args, **kwargs):
            nonlocal swapped
            if Path(path).name == original.name and not swapped:
                swapped = True
                original.unlink()
                original.symlink_to(outside)
            return real_open(path, flags, *args, **kwargs)

        with patch("phase5_plugin_jar_inventory.os.open", side_effect=replace_with_link):
            result = inventory(self.directory)
        self.assertTrue(swapped, "JAR-open race injection must actually run")
        self.assertFalse(result["scan_complete"])
        self.assertEqual([], result["entries"])
        self.assertEqual("unreadable_or_invalid_jar", result["errors"][0]["reason"])

    def test_missing_or_symlink_directory_is_rejected(self):
        with self.assertRaises(ValueError):
            inventory(self.directory / "missing")
        link = self.directory / "alias"
        link.symlink_to(self.directory, target_is_directory=True)
        with self.assertRaises(ValueError):
            inventory(link)
        self.assertEqual(2, main(["--plugins-dir", str(link)]))


    def test_large_jar_spools_without_changing_digest_or_descriptor(self):
        import hashlib
        path = self.directory / "large.jar"
        with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr("plugin.yml", "name: Large\\n")
            archive.writestr("data.bin", b"X" * (9 * 1024 * 1024))
        result = inventory(self.directory)
        self.assertTrue(result["scan_complete"])
        self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), result["entries"][0]["sha256"])
        self.assertEqual(["plugin.yml"], result["entries"][0]["descriptors"])

    def test_symlink_in_ancestor_directory_is_rejected(self):
        nested = self.directory / "real"
        nested.mkdir()
        plugins = nested / "plugins"
        plugins.mkdir()
        with zipfile.ZipFile(plugins / "valid.jar", "w") as archive:
            archive.writestr("plugin.yml", "name: valid")
        alias = self.directory / "alias"
        alias.symlink_to(nested, target_is_directory=True)
        with self.assertRaises(ValueError):
            inventory(alias / "plugins")


if __name__ == "__main__":
    unittest.main()
