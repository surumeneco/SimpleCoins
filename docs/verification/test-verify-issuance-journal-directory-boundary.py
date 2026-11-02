"""CAT Draft ISS-PATH-01..03: offline issuance journal path provenance.

Expected Red: a symlink in an ancestor component currently bypasses the
final-component lstat check. This is read-only test support, not a grant fix.
"""
import hashlib
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    "issuance_verifier",
    Path(__file__).with_name("verify-issuance-journal.py"),
)
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class JournalDirectoryBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.real = self.root / "real"
        self.real.mkdir()
        self.journal = self.real / "issuance"
        self.journal.mkdir()
        head = struct.pack(">q", 0)
        (self.journal / "head").write_bytes(hashlib.sha256(head).digest() + head)

    def test_real_nested_directory_is_valid(self):
        result = verifier.verify(self.journal)
        self.assertEqual((0, 0, 0, []), (
            result["records"], result["pending"], result["head"], result["warnings"]
        ))

    def test_final_directory_symlink_is_rejected(self):
        alias = self.root / "journal-alias"
        alias.symlink_to(self.journal, target_is_directory=True)
        with self.assertRaisesRegex(verifier.InvalidJournal, "real directory"):
            verifier.verify(alias)

    def test_ancestor_directory_symlink_is_rejected(self):
        alias = self.root / "parent-alias"
        alias.symlink_to(self.real, target_is_directory=True)
        with self.assertRaisesRegex(verifier.InvalidJournal, "real directory"):
            verifier.verify(alias / "issuance")


if __name__ == "__main__":
    unittest.main()
