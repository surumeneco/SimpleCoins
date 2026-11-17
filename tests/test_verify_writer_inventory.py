#!/usr/bin/env python3
"""Fail-closed, standard-library tests of point-in-time installed JAR roster."""
from __future__ import annotations

import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "tools/verify_writer_inventory.py"
spec = importlib.util.spec_from_file_location("writer_roster", SCRIPT)
assert spec and spec.loader
roster = importlib.util.module_from_spec(spec)
spec.loader.exec_module(roster)


def expected_report(root: Path, jars: dict[str, bytes]) -> Path:
    path = root / "reference.json"
    path.write_text(json.dumps({
        "kind": "STATIC_CANDIDATE_CALL_REFERENCES_ONLY",
        "jars": [{"jar": name, "sha256": hashlib.sha256(data).hexdigest(),
                  "size": len(data)}
                 for name, data in jars.items()]
    }), encoding="utf-8")
    return path


class InstalledWriterRosterTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.installed = self.root / "plugins"
        self.installed.mkdir()

    def install(self, jars: dict[str, bytes]) -> None:
        for name, data in jars.items():
            (self.installed / name).write_bytes(data)

    def test_identical_sha256_roster_passes_without_claiming_writer_epoch(self) -> None:
        jars = {"InvSee++.jar": b"real-test-content-A",
                "WhatAWonderfulWolf-1.1.1.jar": b"real-test-content-B"}
        self.install(jars)
        expected = expected_report(self.root, jars)
        (self.installed / "InvSeePlusPlus").mkdir()  # ordinary plugin data folder
        result = roster.verify_installed(expected, self.installed)
        self.assertEqual("match", result["status"])
        self.assertEqual(2, result["expected_jar_count"])
        self.assertEqual(1, result["other_top_level_entries_not_scanned"])
        self.assertEqual(sorted(jars), result["matching"])
        self.assertIn("Does not prove which plugins Paper loads", result["caveat"])
        output = self.root / "checked" / "roster.json"
        self.assertEqual(0, roster.main([
            "--reference-json", str(expected),
            "--installed-plugins", str(self.installed),
            "--report-json", str(output)]))
        self.assertEqual("match", json.loads(output.read_text())["status"])

    def test_unknown_missing_or_equal_size_modified_jar_is_denied(self) -> None:
        jars = {"A.jar": b"1234", "B.jar": b"zz"}
        self.install(jars)
        ref = expected_report(self.root, jars)
        (self.installed / "A.jar").write_bytes(b"1235")
        (self.installed / "B.jar").unlink()
        (self.installed / "Extra.jar").write_bytes(b"new")
        observed = roster.verify_installed(ref, self.installed)
        self.assertEqual("mismatch", observed["status"])
        self.assertEqual(["B.jar"], observed["missing"])
        self.assertEqual(["A.jar"], observed["altered"])
        self.assertEqual(["Extra.jar"], observed["unexpected"])
        out = self.root / "failed.json"
        self.assertEqual(2, roster.main([
            "--reference-json", str(ref),
            "--installed-plugins", str(self.installed),
            "--report-json", str(out)]))
        self.assertEqual("mismatch", json.loads(out.read_text())["status"])

    def test_symlinked_jar_and_linked_root_are_rejected(self) -> None:
        installed_file = self.installed / "A.jar"
        target = self.root / "payload.bin"
        target.write_bytes(b"real")
        installed_file.symlink_to(target)
        ref = expected_report(self.root, {"A.jar": b"real"})
        observed = roster.verify_installed(ref, self.installed)
        self.assertEqual("mismatch", observed["status"])
        self.assertEqual(["A.jar"], observed["missing"])
        self.assertEqual("unreadable_or_unstable", observed["rejected"][0]["reason"])
        directory_link = self.root / "linked-plugins"
        directory_link.symlink_to(self.installed, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "linked"):
            roster.verify_installed(ref, directory_link)

    def test_case_alias_and_unsafe_extensions_fail(self) -> None:
        self.install({"A.jar": b"valid", "a.jar": b"shadow",
                      "Other.JAR": b"upper-case"})
        ref = expected_report(self.root, {"A.jar": b"valid"})
        obs = roster.verify_installed(ref, self.installed)
        self.assertEqual("mismatch", obs["status"])
        self.assertIn("casefold_duplicate",
                      {v["reason"] for v in obs["rejected"]})
        self.assertIn("unsafe_jar_name",
                      {v["reason"] for v in obs["rejected"]})

    def test_empty_duplicate_and_forged_baselines_fail(self) -> None:
        ref = self.root / "ref.json"
        ref.write_text(json.dumps({
            "kind": "STATIC_CANDIDATE_CALL_REFERENCES_ONLY", "jars": []
        }), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "absent"):
            roster.reference_roster(ref)
        bad = {"kind": "STATIC_CANDIDATE_CALL_REFERENCES_ONLY",
               "jars": [{"jar": "A.jar", "sha256": "f" * 64, "size": 2},
                        {"jar": "a.jar", "sha256": "f" * 64, "size": 2}]}
        ref.write_text(json.dumps(bad), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "case alias"):
            roster.reference_roster(ref)
        bad["jars"][1]["jar"] = "../../outside.jar"
        ref.write_text(json.dumps(bad), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "basename"):
            roster.reference_roster(ref)
        bad["jars"] = [{"jar": "A.jar", "sha256": "not-sha", "size": True}]
        ref.write_text(json.dumps(bad), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "SHA256"):
            roster.reference_roster(ref)

    def test_missing_installation_root_fails_closed(self) -> None:
        ref = expected_report(self.root, {"A.jar": b"safe"})
        with self.assertRaises((ValueError, FileNotFoundError)):
            roster.verify_installed(ref, self.root / "missing")


if __name__ == "__main__":
    unittest.main(verbosity=2)
