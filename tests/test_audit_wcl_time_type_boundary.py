"""Code-to-spec Red characterization for malformed WCL capture timestamp types.

This does not attest to Paper runtime data or confirm CAT semantics.
"""
import copy
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
sys.path.insert(0, str(ROOT / "tests"))
from audit_wcl_service_capture import audit
from test_audit_wcl_service_capture import valid


class MalformedCaptureTimeTypesTest(unittest.TestCase):
    def test_complete_capture_control_remains_structurally_complete(self):
        self.assertEqual({"status": "structurally_complete", "codes": []}, audit(valid()))

    def test_null_server_start_is_invalid_not_an_uncaught_exception(self):
        capture = valid()
        capture["capture"]["server_start"] = None
        result = audit(capture)
        self.assertEqual("invalid", result["status"])
        self.assertIn("MISSING_CAPTURE_METADATA", result["codes"])

    def test_numeric_captured_at_is_invalid_not_an_uncaught_exception(self):
        capture = valid()
        capture["capture"]["captured_at"] = 0
        result = audit(capture)
        self.assertEqual("invalid", result["status"])
        self.assertIn("MISSING_CAPTURE_METADATA", result["codes"])

    def test_cli_null_timestamp_returns_machine_readable_invalid(self):
        capture = valid()
        capture["capture"]["server_start"] = None
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "capture.json"
            path.write_text(json.dumps(capture), encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(ROOT / "tools" / "audit_wcl_service_capture.py"), str(path)],
                capture_output=True, text=True, check=False)
            self.assertEqual(3, result.returncode)
            self.assertEqual("invalid", json.loads(result.stdout)["status"])
            self.assertEqual("", result.stderr)


if __name__ == "__main__":
    unittest.main()
