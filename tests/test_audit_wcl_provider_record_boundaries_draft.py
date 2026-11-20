"""Candidate characterization for WCL provider registration boundaries.

The CAT Draft is not a Confirmed contract; these tests assert current behavior.
"""
import copy
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from audit_wcl_service_capture import audit
from test_audit_wcl_service_capture import valid


class ProviderRecordBoundaryDraftTests(unittest.TestCase):
    def check(self, change, expected_status, *codes):
        capture = valid()
        change(capture)
        result = audit(capture)
        self.assertEqual(expected_status, result["status"])
        for code in codes:
            self.assertIn(code, result["codes"])

    def test_non_object_registration_preserves_missing_provider_evidence(self):
        self.check(lambda v: v["registrations"].__setitem__(0, None), "invalid",
                   "INVALID_REGISTRATION", "REQUIRED_PROVIDER_MISSING")

    def test_blank_provider_class_is_invalid(self):
        self.check(lambda v: v["registrations"][0].update(provider_class=""), "invalid",
                   "INVALID_PROVIDER_CLASS")

    def test_non_boolean_owner_enabled_is_invalid(self):
        self.check(lambda v: v["registrations"][0].update(owner_enabled=1), "invalid",
                   "INVALID_PROVIDER_ENABLED")

    def test_unexpected_provider_does_not_satisfy_required_provider(self):
        self.check(lambda v: v["registrations"][0].update(provider_id="unknown"), "incomplete",
                   "UNEXPECTED_PROVIDER", "REQUIRED_PROVIDER_MISSING")

    def test_duplicate_provider_with_owner_mismatch_preserves_both_codes(self):
        def mutate(capture):
            duplicate = copy.deepcopy(capture["registrations"][0])
            duplicate["owner_plugin"] = "Unknown"
            capture["registrations"].append(duplicate)
        self.check(mutate, "invalid", "DUPLICATE_PROVIDER", "PROVIDER_OWNER_MISMATCH")


if __name__ == "__main__":
    unittest.main()
