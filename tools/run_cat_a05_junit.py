#!/usr/bin/env python3
"""Run one script-generated CAT A05 JUnit case, preserving actual Gradle XML.

This is a technology test runner, NOT a CAT Gate or observation-capability
provider. It copies a real JUnit report from the invocation, never creates
a synthetic testcase or expected output.
"""
from __future__ import annotations

from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET


def main(args: list[str]) -> int:
    if len(args) != 2 or not re.fullmatch(r"case\.s[0-7]", args[1]):
        print("usage: run_cat_a05_junit.py <report.xml> case.s0..case.s7", file=sys.stderr)
        return 2
    target = Path(args[0]).resolve()
    case_id = args[1]
    root = Path(__file__).resolve().parents[1]
    reports = root / "build/test-results/test"
    pattern = "TEST-co.surumene.simplecoins.domain.generated.GeneratedA05WclEvidenceCase*Test.xml"
    # Removing old generated test reports prevents stale evidence being reused.
    for old in reports.glob(pattern):
        old.unlink()
    command = [
        "gradle", "--no-daemon", ":test",
        "--tests", "co.surumene.simplecoins.domain.generated.GeneratedA05WclEvidenceCase*Test",
    ]
    try:
        outcome = subprocess.run(command, cwd=root, capture_output=True, text=True,
                                 errors="replace", timeout=260, check=False)
    except (OSError, subprocess.TimeoutExpired) as error:
        print(f"CAT A05 JUnit runner failed to invoke Gradle: {error}", file=sys.stderr)
        return 3
    print(outcome.stdout, end="")
    print(outcome.stderr, end="", file=sys.stderr)
    files = list(reports.glob(pattern))
    if len(files) != 1:
        print(f"Expected exactly one real Gradle CAT JUnit report, got: {files}", file=sys.stderr)
        return 4
    try:
        cases = ET.parse(files[0]).getroot().findall(".//testcase")
    except ET.ParseError as error:
        print(f"Malformed real Gradle CAT JUnit report: {error}", file=sys.stderr)
        return 5
    if len(cases) != 1 or cases[0].get("name") != case_id:
        print(f"CAT JUnit actual testcase identity mismatch: expected {case_id}; "
              f"observed {[x.get('name') for x in cases]}", file=sys.stderr)
        return 6
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(files[0], target)
    return outcome.returncode


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
