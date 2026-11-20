#!/usr/bin/env python3
"""Execute one generated A04 CAT JUnit case and capture its real Gradle XML."""
from __future__ import annotations

from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET


def main(args: list[str]) -> int:
    if len(args) != 2 or args[1] not in {'case.f0', 'case.f1', 'case.o0', 'case.h0'}:
        print('usage: run_cat_a04_junit.py <report.xml> case.{f0,f1,o0,h0}', file=sys.stderr)
        return 2
    target = Path(args[0]).resolve()
    case_id = args[1]
    root = Path(__file__).resolve().parents[1]
    reports = root / 'build/test-results/test'
    pattern = 'TEST-co.surumene.simplecoins.a04.generated.GeneratedA04SavedOwnerCase*Test.xml'
    for old in reports.glob(pattern):
        old.unlink()
    command = [
        'gradle', '--no-daemon', ':test',
        '--tests', 'co.surumene.simplecoins.a04.generated.GeneratedA04SavedOwnerCase*Test',
    ]
    try:
        result = subprocess.run(command, cwd=root, capture_output=True, text=True,
                                errors='replace', timeout=260, check=False)
    except (OSError, subprocess.TimeoutExpired) as exc:
        print(f'A04 JUnit runner invocation failed: {exc}', file=sys.stderr)
        return 3
    print(result.stdout, end='')
    print(result.stderr, end='', file=sys.stderr)
    reports_found = list(reports.glob(pattern))
    if len(reports_found) != 1:
        print(f'Expected exactly one A04 JUnit report; got: {reports_found}', file=sys.stderr)
        return 4
    try:
        cases = ET.parse(reports_found[0]).getroot().findall('.//testcase')
    except ET.ParseError as exc:
        print(f'Malformed real A04 JUnit report: {exc}', file=sys.stderr)
        return 5
    if len(cases) != 1 or cases[0].get('name') != case_id:
        print(f'A04 JUnit testcase identity mismatch: {case_id}; '
              f'observed {[x.get("name") for x in cases]}', file=sys.stderr)
        return 6
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(reports_found[0], target)
    return result.returncode


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
