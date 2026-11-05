# WCL capture duplicate JSON member boundary — CAT Draft

Status: Draft / code-to-spec candidate, **not Confirmed**.
Task: TASK-20261101-004.
Source: `tools/audit_wcl_service_capture.py` on `develop` (blob `d79c9318dc4572751e01def2cc0c2a8eb0895969`).

## Candidate safety property

The read-only WCL structural audit accepts externally supplied JSON, not an authenticated Paper runtime attestation. JSON objects with duplicate member names are ambiguous between parsers and may replace a previously declared provider, enabled flag, or capture metadata field. Python's default `json.loads` silently retains the last value. An adversarial capture must not be treated as structurally complete merely because its last duplicate value appears valid.

## Vectors

1. Duplicate `schema_version` at top level, with an earlier conflicting value.
2. Duplicate `registration_list_complete` in capture metadata, earlier false and later true.
3. Duplicate `owner_plugin` in a registration, earlier wrong owner and later expected owner.
4. Duplicate `enabled` in a required plugin, earlier false and later true.

Candidate result: `status=invalid` and a machine-readable duplicate-member code; CLI exit code 3. No inference about actual ownership or the trustworthiness of any Paper service is authorized by this Draft.

These are Expected Red characterization vectors. The normative decision, formal CAT Gate, and product change are separate, and no production fix is authorized by this document alone.
