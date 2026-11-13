# WCL capture JSON depth boundary — CAT Candidate (not Confirmed)

- Work: TASK-20261101-004
- Target: `tools/audit_wcl_service_capture.py` CLI, `main()`
- Source: current `develop` audit reader (review before implementation)
- Status: Candidate / unapproved; no production behavior change authorized
- Flow: issue-work; expected Red characterization, not a formal CAT Gate

## Observed boundary

The capture reader limits file size to 1 MiB and maps `OSError`, `ValueError`, and `UnicodeError` to `UNREADABLE_CAPTURE`. The standard JSON decoder can instead raise `RecursionError` on a deeply nested but small JSON input. This exception is not currently caught by the CLI.

## Proposed behavior requiring decision

A capture with excessive nesting should be rejected as invalid without a Python traceback, using the existing `UNREADABLE_CAPTURE` machine-readable code and exit status 3. This must not imply the capture is structurally complete, nor expose player data. The exact rejection code is a proposal, not an approved contract.

## Reproduction and evidence boundary

Create an input under 1 MiB with 20,000 nested arrays, then call the actual `main([path])`. Candidate test expects status invalid, code UNREADABLE_CAPTURE, exit 3 and no stderr traceback. Current implementation raises RecursionError, so the test is expected Red. No Gate, production fix, Paper runtime, or develop merge is claimed.

## Next required stages

Obtain normative CAT confirmation, run the project’s prescribed CAT Gate, then implement the minimum permitted Green fix and direct regression suite. Check interaction with existing invalid-JSON and metadata-type candidates before integration.
