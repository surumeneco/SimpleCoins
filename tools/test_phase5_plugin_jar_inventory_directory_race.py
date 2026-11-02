"""Code-to-spec Red regression: ancestor directory swap must not attest external JARs."""
import hashlib
from pathlib import Path
from unittest.mock import patch
import tempfile
import unittest
import zipfile

import phase5_plugin_jar_inventory as subject


class PluginDirectoryRaceTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.plugins = self.root / "plugins"
        self.plugins.mkdir()
        self.outside = self.root / "outside"
        self.outside.mkdir()
        self._jar(self.plugins / "same.jar", "expected")
        self.outside_jar = self._jar(self.outside / "same.jar", "outside")

    @staticmethod
    def _jar(path, identity):
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("plugin.yml", "name: " + identity + "\nversion: 1.0\n")
        return path

    def test_control_static_directory_scans_only_expected_jar(self):
        result = subject.inventory(self.plugins)
        expected = hashlib.sha256((self.plugins / "same.jar").read_bytes()).hexdigest()
        outside = hashlib.sha256(self.outside_jar.read_bytes()).hexdigest()
        self.assertTrue(result["scan_complete"])
        self.assertEqual([expected], [entry["sha256"] for entry in result["entries"]])
        self.assertNotEqual(expected, outside)

    def test_parent_directory_swap_before_open_must_not_attest_outside_jar(self):
        real_open = subject.os.open
        swapped = False

        def swap_parent(path, flags):
            nonlocal swapped
            if not swapped and Path(path).name == "same.jar":
                swapped = True
                self.plugins.rename(self.root / "plugins-original")
                self.plugins.symlink_to(self.outside, target_is_directory=True)
            return real_open(path, flags)

        with patch.object(subject.os, "open", side_effect=swap_parent):
            result = subject.inventory(self.plugins)
        outside_hash = hashlib.sha256(self.outside_jar.read_bytes()).hexdigest()
        self.assertFalse(result["scan_complete"], "ancestor symlink swap must fail closed")
        self.assertFalse(any(e["sha256"] == outside_hash for e in result["entries"]))

    def test_parent_directory_swap_at_enumeration_must_not_attest_outside_jar(self):
        real_iterdir = Path.iterdir
        swapped = False

        def swap_at_iterdir(path):
            nonlocal swapped
            if not swapped and path == self.plugins:
                swapped = True
                self.plugins.rename(self.root / "plugins-original")
                self.plugins.symlink_to(self.outside, target_is_directory=True)
            return real_iterdir(path)

        with patch.object(Path, "iterdir", swap_at_iterdir):
            result = subject.inventory(self.plugins)
        outside_hash = hashlib.sha256(self.outside_jar.read_bytes()).hexdigest()
        self.assertFalse(result["scan_complete"], "ancestor swap before listing must fail closed")
        self.assertFalse(any(e["sha256"] == outside_hash for e in result["entries"]))


if __name__ == "__main__":
    unittest.main()
