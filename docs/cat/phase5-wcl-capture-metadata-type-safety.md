# WCL capture metadata type safety — CAT Draft

Status: Draft (code-to-spec characterization); not Confirmed.
Task: TASK-20261101-004.
Reference: `tools/audit_wcl_service_capture.py` on `develop`, blob `d79c9318dc4572751e01def2cc0c2a8eb0895969`.
Fixture: `tests/test_audit_wcl_service_capture.py`, blob `021a36ed693e660c20a977da5d128c65e34dd04b`.

## Observed boundary

The read-only audit consumes untrusted, caller-exported JSON metadata. Its `server_start` and `captured_at` fields may be valid JSON values that are not strings. Current code records `MISSING_CAPTURE_METADATA` for these values, but then calls `.replace("Z", "+00:00")` without guarding their types. A JSON number, array, object or boolean therefore may raise `AttributeError` instead of returning a structured invalid result. The CLI's exception boundary likewise does not catch `AttributeError`.

## Draft characterization vectors

- For each of `server_start` and `captured_at`, replace a valid timestamp with `null`, a number, a boolean, an array, or an object.
- The proposed safe outcome is `status=invalid`, including `MISSING_CAPTURE_METADATA` and `INVALID_CAPTURE_TIME`, with no uncaught exception.
- For an actual JSON file, the CLI should return exit code 3 with a parseable JSON invalid result, not a Python traceback.

This is a candidate robustness contract, **not** a Confirmed semantic change or evidence of live Paper provenance. The companion tests are expected Red on the pinned source. Normative CAT approval and the required Gate must precede any production fix; no runtime ownership inference may be drawn from this fixture.
