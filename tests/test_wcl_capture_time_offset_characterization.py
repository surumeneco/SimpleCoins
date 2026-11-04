"""Draft characterization: WCL capture timestamp normalization and evidence precedence.

The read-only audit is not proof that a capture came from Paper.
No production behavior is modified by this test module.
"""
import copy
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from audit_wcl_service_capture import audit
from test_audit_wcl_service_capture import valid


class CaptureTimeOffsetCharacterizationTests(unittest.TestCase):
    def test_equivalent_instant_across_offsets_is_complete(self):
        capture = valid()
        capture["capture"]["server_start"] = "2026-11-03T09:00:00+09:00"
        capture["capture"]["captured_at"] = "2026-11-02T19:01:00-05:00"
        self.assertEqual({"status": "structurally_complete", "codes": []}, audit(capture))

    def test_local_clock_order_does_not_override_instant_order(self):
        capture = valid()
        capture["capture"]["server_start"] = "2026-11-03T01:00:00+00:00"
        capture["capture"]["captured_at"] = "2026-11-03T09:59:00+09:00"
        result = audit(capture)
        self.assertEqual("invalid", result["status"])
        self.assertIn("INVALID_CAPTURE_TIME", result["codes"])

    def test_equal_instant_is_accepted_by_current_contract(self):
        capture = valid()
        capture["capture"]["server_start"] = "2026-11-03T00:00:00Z"
        capture["capture"]["captured_at"] = "2026-11-03T09:00:00+09:00"
        self.assertEqual({"status": "structurally_complete", "codes": []}, audit(capture))

    def test_invalid_timestamp_does_not_hide_missing_provider(self):
        capture = valid()
        capture["capture"]["captured_at"] = "not-a-timestamp"
        capture["registrations"].pop()
        result = audit(capture)
        self.assertEqual("invalid", result["status"])
        self.assertIn("INVALID_CAPTURE_TIME", result["codes"])
        self.assertIn("REQUIRED_PROVIDER_MISSING", result["codes"])
        self.assertEqual(sorted(set(result["codes"])), result["codes"])

    def test_unverified_capture_flag_is_distinct_from_invalid_time(self):
        capture = valid()
        capture["capture"]["registration_list_complete"] = False
        capture["capture"]["captured_at"] = "2026-11-02T23:59:59Z"
        result = audit(capture)
        self.assertEqual("invalid", result["status"])
        self.assertIn("UNVERIFIED_REGISTRATION_LIST_COMPLETE", result["codes"])
        self.assertIn("INVALID_CAPTURE_TIME", result["codes"])


if __name__ == "__main__":
    unittest.main()
