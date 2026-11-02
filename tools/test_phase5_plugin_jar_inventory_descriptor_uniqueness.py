"""CAT Draft JAR-DESCRIPTOR-01..03: root ZIP descriptor multiplicity.

Expected Red: current inventory collapses duplicate member names into a set.
This test does not claim Paper's actual plugin descriptor precedence.
"""

from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile

from phase5_plugin_jar_inventory import inventory


class PluginJarDescriptorUniquenessTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)

    def make_jar(self, descriptors):
        path = self.directory / "descriptor.jar"
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(path, "w") as archive:
                for index, name in enumerate(descriptors):
                    archive.writestr(
                        name, f"name: Plugin{index}\\nversion: '1.0'\\n"
                    )
        return path

    def assert_duplicate_rejected(self, name):
        self.make_jar([name, name])
        result = inventory(self.directory)
        self.assertFalse(
            result["scan_complete"],
            f"two ZIP members named {name!r} must not be attested as unambiguous",
        )
        self.assertIn(
            {"jar": "descriptor.jar", "reason": "ambiguous_root_plugin_descriptor"},
            result["errors"],
        )

    def test_duplicate_legacy_root_descriptor_is_incomplete(self):
        self.assert_duplicate_rejected("plugin.yml")

    def test_duplicate_paper_root_descriptor_is_incomplete(self):
        self.assert_duplicate_rejected("paper-plugin.yml")

    def test_distinct_legacy_and_paper_descriptors_are_not_duplicate_paths(self):
        self.make_jar(["plugin.yml", "paper-plugin.yml"])
        result = inventory(self.directory)
        self.assertTrue(result["scan_complete"])
        self.assertEqual([], result["errors"])
        self.assertEqual(
            ["paper-plugin.yml", "plugin.yml"],
            result["entries"][0]["descriptors"],
        )


if __name__ == "__main__":
    unittest.main()
