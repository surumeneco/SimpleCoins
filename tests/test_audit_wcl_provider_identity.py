"""Code-to-spec characterization of WCL service identity boundaries.

These tests exercise the existing read-only structural audit, not live Paper
provenance or a Confirmed CAT acceptance contract.
"""
import copy
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from audit_wcl_service_capture import audit
from test_audit_wcl_service_capture import valid


class ProviderIdentityCharacterizationTests(unittest.TestCase):
    def assert_status_codes(self, capture, expected_status, *expected_codes):
        result = audit(capture)
        self.assertEqual(expected_status, result["status"])
        for code in expected_codes:
            self.assertIn(code, result["codes"])
        self.assertEqual(sorted(set(result["codes"])), result["codes"])

    def test_duplicate_plugin_with_conflicting_jar_identity_is_invalid(self):
        capture = valid()
        duplicate = copy.deepcopy(capture["plugins"][1])
        duplicate["jar_sha256"] = "b" * 64
        capture["plugins"].append(duplicate)
        self.assert_status_codes(capture, "invalid", "DUPLICATE_PLUGIN")

    def test_unexpected_provider_does_not_replace_required_providers(self):
        capture = valid()
        extra = copy.deepcopy(capture["registrations"][0])
        extra["provider_id"] = "third-party"
        capture["registrations"].append(extra)
        self.assert_status_codes(capture, "incomplete", "UNEXPECTED_PROVIDER")
        self.assertNotIn("REQUIRED_PROVIDER_MISSING", audit(capture)["codes"])

    def test_case_alias_is_not_equivalent_to_required_provider(self):
        capture = valid()
        capture["registrations"][0]["provider_id"] = "WWC"
        self.assert_status_codes(
            capture, "incomplete", "REQUIRED_PROVIDER_MISSING", "UNEXPECTED_PROVIDER"
        )

    def test_whitespace_alias_is_not_equivalent_to_required_plugin(self):
        capture = valid()
        capture["plugins"][0]["name"] += " "
        self.assert_status_codes(capture, "incomplete", "REQUIRED_PLUGIN_MISSING")

    def test_invalid_duplicate_does_not_hide_missing_provider(self):
        capture = valid()
        capture["registrations"].pop()
        capture["registrations"].append(copy.deepcopy(capture["registrations"][0]))
        self.assert_status_codes(
            capture, "invalid", "DUPLICATE_PROVIDER", "REQUIRED_PROVIDER_MISSING"
        )


if __name__ == "__main__":
    unittest.main()
