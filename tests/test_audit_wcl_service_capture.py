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


def valid():
    return {
        "schema_version": 1,
        "capture": {"server_start": "2026-11-03T00:00:00Z", "captured_at": "2026-11-03T00:01:00Z",
                    "paper_version": "26.2", "primary_thread": True, "registration_list_complete": True},
        "plugins": [
            {"name": name, "version": "source-verified-candidate", "enabled": True,
             "jar_sha256": "a" * 64}
            for name in ("WonderfulCoreLibrary", "WhatAWonderfulChicken", "WhatAWonderfulWolf")
        ],
        "registrations": [
            {"provider_id": pid, "owner_plugin": owner, "provider_class": "example.Reader",
             "owner_enabled": True}
            for pid, owner in (("wwc", "WhatAWonderfulChicken"), ("www", "WhatAWonderfulWolf"))
        ],
    }


class AuditTests(unittest.TestCase):
    def check(self, change, status, code=None):
        value = valid()
        change(value)
        result = audit(value)
        self.assertEqual(status, result["status"])
        if code:
            self.assertIn(code, result["codes"])

    def test_complete_structure_is_not_runtime_attestation(self):
        self.assertEqual({"status": "structurally_complete", "codes": []}, audit(valid()))

    def test_missing_provider(self):
        self.check(lambda v: v["registrations"].pop(), "incomplete", "REQUIRED_PROVIDER_MISSING")

    def test_missing_plugin(self):
        self.check(lambda v: v["plugins"].pop(), "incomplete", "REQUIRED_PLUGIN_MISSING")

    def test_disabled_plugin(self):
        self.check(lambda v: v["plugins"][0].update(enabled=False), "incomplete", "REQUIRED_PLUGIN_DISABLED")

    def test_wrong_owner(self):
        self.check(lambda v: v["registrations"][0].update(owner_plugin="Unknown"),
                   "incomplete", "PROVIDER_OWNER_MISMATCH")

    def test_duplicate_provider(self):
        self.check(lambda v: v["registrations"].append(copy.deepcopy(v["registrations"][0])),
                   "invalid", "DUPLICATE_PROVIDER")

    def test_unverified_thread(self):
        self.check(lambda v: v["capture"].update(primary_thread=False),
                   "incomplete", "UNVERIFIED_PRIMARY_THREAD")

    def test_unverified_registration_completeness(self):
        self.check(lambda v: v["capture"].update(registration_list_complete=False),
                   "incomplete", "UNVERIFIED_REGISTRATION_LIST_COMPLETE")

    def test_missing_jar_hash(self):
        self.check(lambda v: v["plugins"][1].update(jar_sha256=""),
                   "incomplete", "MISSING_JAR_IDENTITY")

    def test_invalid_flag_type(self):
        self.check(lambda v: v["capture"].update(primary_thread="true"),
                   "invalid", "INVALID_CAPTURE_FLAG")

    def test_capture_before_start_is_invalid(self):
        self.check(lambda v: v["capture"].update(captured_at="2026-11-02T23:59:00Z"),
                   "invalid", "INVALID_CAPTURE_TIME")

    def test_naive_timestamp_is_invalid(self):
        self.check(lambda v: v["capture"].update(server_start="2026-11-03T00:00:00"),
                   "invalid", "INVALID_CAPTURE_TIME")

    def test_boolean_schema_version_is_invalid(self):
        self.check(lambda v: v.update(schema_version=True), "invalid", "INVALID_SCHEMA")

    def test_cli_exit_codes(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "capture.json"
            incomplete = valid()
            incomplete["registrations"].pop()
            for data, expected in ((valid(), 0), (incomplete, 2), ({"schema_version": 1}, 3)):
                path.write_text(json.dumps(data), encoding="utf-8")
                result = subprocess.run([sys.executable, str(ROOT / "tools" / "audit_wcl_service_capture.py"), str(path)],
                                        capture_output=True, text=True, check=False)
                self.assertEqual(expected, result.returncode)
                self.assertIn("status", json.loads(result.stdout))


if __name__ == "__main__":
    unittest.main()
