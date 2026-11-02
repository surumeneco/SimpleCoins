#!/usr/bin/env python3
"""Cross-language tests using the actual Java GrantLedger writer, not synthetic SCG2 bytes."""
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "issuance_verifier_mutf8", Path(__file__).with_name("verify-issuance-journal.py"))
verifier = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(verifier)


class ActualJavaWriterMutf8Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        sources = [
            ROOT / "src/main/java/co/surumene/simplecoins/domain/DurableFile.java",
            ROOT / "src/main/java/co/surumene/simplecoins/domain/CoinDenomination.java",
            ROOT / "src/main/java/co/surumene/simplecoins/domain/GrantLedger.java",
            ROOT / "docs/verification/IssuanceJournalMutf8WriterFixture.java",
        ]
        if not all(p.is_file() for p in sources) or not shutil.which("javac") or not shutil.which("java"):
            if os.environ.get("CI"):
                raise AssertionError("CI must run the actual Java writer fixture")
            raise unittest.SkipTest("Actual Java sources and JDK required")
        cls.classes = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.classes.cleanup)
        subprocess.run(
            ["javac", "-d", cls.classes.name, *map(str, sources)],
            check=True, capture_output=True, text=True)

    def run_writer(self, case):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "issuance"
            subprocess.run(
                ["java", "-cp", self.classes.name, "IssuanceJournalMutf8WriterFixture",
                 str(directory), case],
                check=True, capture_output=True, text=True)
            result = verifier.verify(directory)
            records = sorted(directory.glob("*.grant"))
            payloads = [p.read_bytes()[32:] for p in records]
            return result, payloads

    def test_actual_writer_null_issuer_modified_utf8(self):
        result, payloads = self.run_writer("nul")
        self.assertEqual((1, 1, 1, []),
                         (result["records"], result["pending"], result["head"], result["warnings"]))
        self.assertEqual(1, len(payloads))
        self.assertIn(bytes((0xC0, 0x80)), payloads[0])

    def test_actual_writer_supplementary_utf16_surrogates(self):
        result, payloads = self.run_writer("supplementary")
        self.assertEqual(1, result["records"])
        self.assertIn(bytes((0xED, 0xA0, 0xBD, 0xED, 0xB8, 0x80)), payloads[0])

    def test_actual_writer_accepts_java_utf16_length_1024(self):
        result, payloads = self.run_writer("max-utf16")
        self.assertEqual(1, result["records"])
        self.assertEqual(1, len(payloads))

    def test_actual_writer_rejects_java_utf16_length_1025(self):
        result, payloads = self.run_writer("too-long")
        self.assertEqual((0, 0, 0), (result["records"], result["pending"], result["head"]))
        self.assertEqual([], payloads)


if __name__ == "__main__":
    unittest.main()
