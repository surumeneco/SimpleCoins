"""Candidate Red: deeply nested WCL JSON must not escape the audit CLI.

The paired CAT document is a Candidate, not a Confirmed contract.
"""
import contextlib
import io
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from audit_wcl_service_capture import main


class WclJsonDepthCandidateTests(unittest.TestCase):
    def test_deep_nesting_is_machine_readable_invalid(self):
        # Well below the 1 MiB file bound; beyond CPython's JSON recursion limit.
        payload = "[" * 20000 + "0" + "]" * 20000
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "nested.json"
            path.write_text(payload, encoding="utf-8")
            stdout, stderr = io.StringIO(), io.StringIO()
            with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
                code = main([str(path)])
        self.assertEqual(3, code)
        self.assertEqual({"status": "invalid", "codes": ["UNREADABLE_CAPTURE"]},
                         json.loads(stdout.getvalue()))
        self.assertEqual("", stderr.getvalue())


    def test_deep_nesting_subprocess_cli_is_structured_invalid(self):
        # Exercise the real CLI process boundary, including exit status and stderr.
        payload = "[" * 20000 + "0" + "]" * 20000
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "nested.json"
            path.write_text(payload, encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(ROOT / "tools" / "audit_wcl_service_capture.py"), str(path)],
                capture_output=True, text=True, check=False, timeout=15,
            )
        self.assertEqual(3, result.returncode)
        self.assertEqual({"status": "invalid", "codes": ["UNREADABLE_CAPTURE"]},
                         json.loads(result.stdout))
        self.assertEqual("", result.stderr)


if __name__ == "__main__":
    unittest.main()
