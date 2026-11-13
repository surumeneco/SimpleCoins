"""Candidate Expected Red for decoded-equivalent duplicate JSON member names.

CAT Draft only: no normative decision or production change is implied.
"""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from test_audit_wcl_service_capture import valid

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools" / "audit_wcl_service_capture.py"


class EscapedDuplicateMemberDraftTest(unittest.TestCase):
    def assert_rejected(self, old, replacement):
        raw = json.dumps(valid())
        self.assertIn(old, raw)
        raw = raw.replace(old, replacement, 1)
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "capture.json"
            path.write_text(raw, encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(SCRIPT), str(path)],
                capture_output=True, text=True, check=False,
            )
        self.assertEqual(3, result.returncode, result.stdout + result.stderr)
        payload = json.loads(result.stdout)
        self.assertEqual("invalid", payload["status"])
        self.assertTrue(any("DUPLICATE" in code for code in payload["codes"]), payload)

    def test_schema_version_unicode_escape_alias(self):
        self.assert_rejected(
            '"schema_version": 1',
            '"schema_\\u0076ersion": 2, "schema_version": 1',
        )

    def test_capture_completeness_unicode_escape_alias(self):
        self.assert_rejected(
            '"registration_list_complete": true',
            '"registration_list_\\u0063omplete": false, "registration_list_complete": true',
        )

    def test_registration_owner_unicode_escape_alias(self):
        self.assert_rejected(
            '"owner_plugin": "WhatAWonderfulChicken"',
            '"owner_\\u0070lugin": "Untrusted", "owner_plugin": "WhatAWonderfulChicken"',
        )

    def test_plugin_enabled_unicode_escape_alias(self):
        self.assert_rejected(
            '"enabled": true',
            '"enabl\\u0065d": false, "enabled": true',
        )


if __name__ == "__main__":
    unittest.main()
