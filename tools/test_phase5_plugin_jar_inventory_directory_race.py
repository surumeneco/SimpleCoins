"""Code-to-spec Red regression: ancestor directory swaps cannot attest external JARs."""
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
        original = self._jar(self.plugins / "same.jar", "expected")
        outside = self._jar(self.outside / "same.jar", "outside")
        self.expected_hash = hashlib.sha256(original.read_bytes()).hexdigest()
        self.outside_hash = hashlib.sha256(outside.read_bytes()).hexdigest()
        self.assertNotEqual(self.expected_hash, self.outside_hash)

    @staticmethod
    def _jar(path, identity):
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("plugin.yml", "name: " + identity + "\nversion: 1.0\n")
        return path

    def _swap(self):
        self.plugins.rename(self.root / "plugins-original")
        self.plugins.symlink_to(self.outside, target_is_directory=True)

    def _assert_safe_observation(self, result):
        hashes = [entry["sha256"] for entry in result["entries"]]
        self.assertNotIn(self.outside_hash, hashes, "outside JAR must never be attested")
        if result["scan_complete"]:
            # A stable directory FD may legitimately yield a complete snapshot
            # of the ORIGINAL directory even after its pathname is replaced.
            self.assertEqual([self.expected_hash], hashes)

    def test_control_static_directory_scans_only_expected_jar(self):
        result = subject.inventory(self.plugins)
        self.assertTrue(result["scan_complete"])
        self._assert_safe_observation(result)

    def test_parent_directory_swap_before_open_must_not_attest_outside_jar(self):
        real_open = subject.os.open
        swapped = False

        def swap_parent(path, flags, *args, **kwargs):
            nonlocal swapped
            if not swapped and Path(path).name == "same.jar":
                swapped = True
                self._swap()
            return real_open(path, flags, *args, **kwargs)

        with patch.object(subject.os, "open", side_effect=swap_parent):
            try:
                result = subject.inventory(self.plugins)
            except (OSError, ValueError):
                result = None  # Explicit fail-closed rejection is permitted.
        self.assertTrue(swapped, "JAR-open race injection must actually run")
        if result is not None:
            self._assert_safe_observation(result)

    def test_parent_directory_swap_at_enumeration_must_not_attest_outside_jar(self):
        real_scandir = subject.os.scandir
        swapped = False

        def swap_at_scandir(path):
            nonlocal swapped
            if not swapped:
                swapped = True
                self._swap()
            return real_scandir(path)

        # pathlib.Path.iterdir uses os.scandir; FD-anchored scanners can also
        # use os.scandir(directory_fd), so this hook exercises either design.
        with patch.object(subject.os, "scandir", side_effect=swap_at_scandir):
            try:
                result = subject.inventory(self.plugins)
            except (OSError, ValueError):
                result = None
        self.assertTrue(swapped, "directory-enumeration race injection must actually run")
        if result is not None:
            self._assert_safe_observation(result)


if __name__ == "__main__":
    unittest.main()
