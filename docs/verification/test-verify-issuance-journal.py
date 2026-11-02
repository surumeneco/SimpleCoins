#!/usr/bin/env python3
"""Offline tests for SimpleCoins' read-only issuance journal verifier."""
import hashlib
import importlib.util
import shutil
import subprocess
from pathlib import Path
import struct
import tempfile
import unittest
import uuid

spec = importlib.util.spec_from_file_location("issuance_verifier", Path(__file__).with_name("verify-issuance-journal.py"))
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


def utf(s):
    raw = s.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def durable(path, payload):
    path.write_bytes(hashlib.sha256(payload).digest() + payload)


def grant(seq, grant_id=None, player_id=None, schema=2, amount=100, denom=0, count=0, delivered=False, issuer="console"):
    raw = struct.pack(">Iq", 0x53434730 + schema, seq)
    raw += utf(str(grant_id or uuid.uuid4())) + utf(str(player_id or uuid.uuid4()))
    raw += struct.pack(">q", amount) + utf(issuer) + struct.pack(">qB", 12345, int(delivered))
    if schema == 2:
        raw += struct.pack(">iq", denom, count)
    return raw


class VerifyIssuanceJournalTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name) / "issuance"
        self.path.mkdir()

    def head(self, n):
        durable(self.path / "head", struct.pack(">q", n))

    def record(self, seq, **kwargs):
        durable(self.path / f"{seq}.grant", grant(seq, **kwargs))

    def test_empty_journal(self):
        self.head(0)
        self.assertEqual({"records": 0, "pending": 0, "head": 0}, {k: verifier.verify(self.path)[k] for k in ("records", "pending", "head")})

    def test_mixed_schemas_and_delivered(self):
        self.record(1, schema=1, delivered=True)
        self.record(2, schema=2, denom=5, count=3, amount=15)
        self.head(2)
        result = verifier.verify(self.path)
        self.assertEqual((2, 1, []), (result["records"], result["pending"], result["warnings"]))

    def test_checksum_tamper_rejected(self):
        self.record(1)
        self.head(1)
        p = self.path / "1.grant"
        data = bytearray(p.read_bytes())
        data[-1] ^= 1
        p.write_bytes(data)
        with self.assertRaisesRegex(verifier.InvalidJournal, "checksum"):
            verifier.verify(self.path)

    def test_missing_sequence_rejected(self):
        self.record(2)
        self.head(2)
        with self.assertRaisesRegex(verifier.InvalidJournal, "sequence"):
            verifier.verify(self.path)

    def test_noncanonical_filename_rejected(self):
        durable(self.path / "01.grant", grant(1))
        self.head(1)
        with self.assertRaisesRegex(verifier.InvalidJournal, "filename"):
            verifier.verify(self.path)

    def test_duplicate_grant_id_rejected(self):
        duplicate = uuid.uuid4()
        self.record(1, grant_id=duplicate)
        self.record(2, grant_id=duplicate)
        self.head(2)
        with self.assertRaisesRegex(verifier.InvalidJournal, "duplicate"):
            verifier.verify(self.path)

    def test_head_ahead_rejected(self):
        self.record(1)
        self.head(2)
        with self.assertRaisesRegex(verifier.InvalidJournal, "tail"):
            verifier.verify(self.path)

    def test_head_behind_is_warning(self):
        self.record(1)
        self.head(0)
        self.assertIn("head_behind_last_record_recovery_required", verifier.verify(self.path)["warnings"])

    def test_pending_temp_is_warning(self):
        self.head(0)
        (self.path / ".pending-test.tmp").write_bytes(b"partial")
        self.assertIn("pending_temporary_files_present", verifier.verify(self.path)["warnings"])

    def test_bad_denomination_rejected(self):
        self.record(1, denom=7, count=1, amount=7)
        self.head(1)
        with self.assertRaisesRegex(verifier.InvalidJournal, "denomination"):
            verifier.verify(self.path)

    def test_trailing_bytes_rejected(self):
        durable(self.path / "1.grant", grant(1) + b"extra")
        self.head(1)
        with self.assertRaisesRegex(verifier.InvalidJournal, "trailing"):
            verifier.verify(self.path)

    def test_symlinked_record_rejected(self):
        outside = Path(self.tmp.name) / "outside"
        durable(outside, grant(1))
        (self.path / "1.grant").symlink_to(outside)
        self.head(1)
        with self.assertRaisesRegex(verifier.InvalidJournal, "symbolic"):
            verifier.verify(self.path)

    def test_symlinked_directory_rejected(self):
        self.head(0)
        link = Path(self.tmp.name) / "alias"
        link.symlink_to(self.path, target_is_directory=True)
        with self.assertRaisesRegex(verifier.InvalidJournal, "real directory"):
            verifier.verify(link)

    def test_missing_head_rejected(self):
        self.record(1)
        with self.assertRaises(verifier.InvalidJournal):
            verifier.verify(self.path)

    def test_real_java_grant_ledger_writer_compatibility(self):
        root = Path(__file__).resolve().parents[2]
        sources = [
            root / "src/main/java/co/surumene/simplecoins/domain/DurableFile.java",
            root / "src/main/java/co/surumene/simplecoins/domain/CoinDenomination.java",
            root / "src/main/java/co/surumene/simplecoins/domain/GrantLedger.java",
            root / "docs/verification/IssuanceJournalVerifierFixture.java",
        ]
        if not all(path.is_file() for path in sources) or not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("actual Java sources and JDK required")
        classes = Path(self.tmp.name) / "classes"
        classes.mkdir()
        subprocess.run(["javac", "-d", str(classes), *map(str, sources)],
                       check=True, capture_output=True, text=True)
        subprocess.run(["java", "-cp", str(classes), "IssuanceJournalVerifierFixture",
                        str(self.path)], check=True, capture_output=True, text=True)
        result = verifier.verify(self.path)
        self.assertEqual((2, 1, 2, []),
                         (result["records"], result["pending"], result["head"], result["warnings"]))

    def test_java_nonblank_unicode_issuer_is_accepted(self):
        # Java String.isBlank() returns false for these four code points.
        for cp in ("\u00a0", "\u0085", "\u2007", "\u202f"):
            with self.subTest(codepoint=f"U+{ord(cp):04X}"):
                self.record(1, issuer=cp)
                self.head(1)
                self.assertEqual(1, verifier.verify(self.path)["records"])
                (self.path / "1.grant").unlink()

    def test_real_java_writer_accepts_nonbreaking_issuer(self):
        # Compile the actual GrantLedger writer, not just a Python SCG2 fixture.
        root = Path(__file__).resolve().parents[2]
        sources = [
            root / "src/main/java/co/surumene/simplecoins/domain/DurableFile.java",
            root / "src/main/java/co/surumene/simplecoins/domain/CoinDenomination.java",
            root / "src/main/java/co/surumene/simplecoins/domain/GrantLedger.java",
            root / "docs/verification/IssuanceJournalBlankIssuerFixture.java",
        ]
        if not all(path.is_file() for path in sources) or not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("actual Java sources and JDK required")
        classes = Path(self.tmp.name) / "blank-issuer-classes"
        classes.mkdir()
        subprocess.run(["javac", "-d", str(classes), *map(str, sources)],
                       check=True, capture_output=True, text=True)
        subprocess.run(["java", "-cp", str(classes), "IssuanceJournalBlankIssuerFixture",
                        str(self.path)], check=True, capture_output=True, text=True)
        self.assertEqual(1, verifier.verify(self.path)["records"])

    def test_java_blank_unicode_issuer_is_rejected(self):
        # Java String.isBlank() returns true for these inputs.
        for issuer in ("\u2003", " ", "\t", "\u001c"):
            with self.subTest(issuer=repr(issuer)):
                self.record(1, issuer=issuer)
                self.head(1)
                with self.assertRaises(verifier.InvalidJournal):
                    verifier.verify(self.path)
                (self.path / "1.grant").unlink()

    def test_no_identity_leak(self):
        private_grant_id, private_player_id = uuid.uuid4(), uuid.uuid4()
        self.record(1, grant_id=private_grant_id, player_id=private_player_id)
        self.head(1)
        result = verifier.verify(self.path)
        self.assertNotIn(str(private_grant_id), str(result))
        self.assertNotIn(str(private_player_id), str(result))
        self.assertNotIn("console", str(result))


if __name__ == "__main__":
    unittest.main()
