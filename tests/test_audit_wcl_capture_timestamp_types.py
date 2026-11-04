"""Draft Red characterization for untrusted WCL capture timestamp field types.

This is a proposed code-to-spec boundary, not a Confirmed CAT contract.
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
from audit_wcl_service_capture import audit
from test_audit_wcl_service_capture import valid


class CaptureTimestampTypeSafetyDraftTests(unittest.TestCase):
    def test_non_string_timestamp_fields_return_structured_invalid(self):
        values = (None, 123, True, [], {})
        for field in ("server_start", "captured_at"):
            for value in values:
                with self.subTest(field=field, value=repr(value)):
                    capture = valid()
                    capture["capture"][field] = copy.deepcopy(value)
                    result = audit(capture)
                    self.assertEqual("invalid", result["status"])
                    self.assertIn("MISSING_CAPTURE_METADATA", result["codes"])
                    self.assertIn("INVALID_CAPTURE_TIME", result["codes"])

    def test_cli_non_string_timestamp_is_json_invalid_not_traceback(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "capture.json"
            for field in ("server_start", "captured_at"):
                for value in (None, 123, True, [], {}):
                    with self.subTest(field=field, value=repr(value)):
                        capture = valid()
                        capture["capture"][field] = copy.deepcopy(value)
                        path.write_text(json.dumps(capture), encoding="utf-8")
                        proc = subprocess.run(
                            [sys.executable, str(ROOT / "tools" / "audit_wcl_service_capture.py"), str(path)],
                            capture_output=True, text=True, check=False,
                        )
                        self.assertEqual(3, proc.returncode, proc.stderr)
                        self.assertEqual("", proc.stderr, "CLI must not emit a traceback")
                        result = json.loads(proc.stdout)
                        self.assertEqual("invalid", result["status"])
                        self.assertIn("MISSING_CAPTURE_METADATA", result["codes"])
                        self.assertIn("INVALID_CAPTURE_TIME", result["codes"])


if __name__ == "__main__":
    unittest.main()
