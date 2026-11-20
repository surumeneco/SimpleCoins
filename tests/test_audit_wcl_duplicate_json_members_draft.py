"""Expected Red: duplicate JSON members in untrusted WCL capture.

CAT Draft only. No product behavior is authorized by this test.
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


class DuplicateMemberDraftTest(unittest.TestCase):
    def assert_duplicate_rejected(self, raw):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "capture.json"
            path.write_text(raw, encoding="utf-8")
            process = subprocess.run(
                [sys.executable, str(SCRIPT), str(path)],
                text=True, capture_output=True, check=False,
            )
            self.assertEqual(3, process.returncode, process.stdout + process.stderr)
            result = json.loads(process.stdout)
            self.assertEqual("invalid", result["status"])
            self.assertTrue(any("DUPLICATE" in code for code in result["codes"]), result)

    def test_duplicate_schema_version(self):
        raw = json.dumps(valid())
        self.assert_duplicate_rejected(raw.replace(
            '"schema_version": 1', '"schema_version": 2, "schema_version": 1', 1
        ))

    def test_duplicate_capture_completeness_flag(self):
        raw = json.dumps(valid())
        self.assert_duplicate_rejected(raw.replace(
            '"registration_list_complete": true',
            '"registration_list_complete": false, "registration_list_complete": true', 1
        ))

    def test_duplicate_provider_owner(self):
        raw = json.dumps(valid())
        self.assert_duplicate_rejected(raw.replace(
            '"owner_plugin": "WhatAWonderfulChicken"',
            '"owner_plugin": "Untrusted", "owner_plugin": "WhatAWonderfulChicken"', 1
        ))

    def test_duplicate_required_plugin_enabled(self):
        raw = json.dumps(valid())
        self.assert_duplicate_rejected(raw.replace(
            '"enabled": true', '"enabled": false, "enabled": true', 1
        ))


if __name__ == "__main__":
    unittest.main()
