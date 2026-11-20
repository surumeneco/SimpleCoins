"""Draft code-to-spec tests for malformed WCL capture records.

These tests only characterize the read-only audit; they do not verify live
Paper provenance or constitute a Confirmed CAT contract.
"""
import copy
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from audit_wcl_service_capture import audit
from test_audit_wcl_service_capture import valid


class MalformedRecordCharacterizationTests(unittest.TestCase):
    def check(self, change, *codes):
        capture = valid()
        change(capture)
        result = audit(capture)
        self.assertEqual("invalid", result["status"])
        for code in codes:
            self.assertIn(code, result["codes"])
        self.assertEqual(sorted(set(result["codes"])), result["codes"])

    def test_non_object_plugin_is_invalid(self):
        self.check(lambda c: c["plugins"].append(None), "INVALID_PLUGIN_RECORD")

    def test_non_object_registration_is_invalid(self):
        self.check(lambda c: c["registrations"].append([]), "INVALID_REGISTRATION")

    def test_required_plugin_enabled_must_be_boolean(self):
        self.check(lambda c: c["plugins"][0].update(enabled=1), "INVALID_PLUGIN_ENABLED")

    def test_provider_enabled_must_be_boolean(self):
        self.check(lambda c: c["registrations"][0].update(owner_enabled="true"),
                   "INVALID_PROVIDER_ENABLED")

    def test_invalid_record_does_not_hide_missing_provider(self):
        def change(c):
            c["registrations"].pop()
            c["registrations"].append(None)
        self.check(change, "INVALID_REGISTRATION", "REQUIRED_PROVIDER_MISSING")


if __name__ == "__main__":
    unittest.main()
