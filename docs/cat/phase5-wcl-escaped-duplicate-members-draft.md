# WCL capture: escaped duplicate JSON members (CAT Draft)

- Status: **Draft / candidate only**; not a Confirmed requirement.
- Work: TASK-20261101-004; source: `tools/audit_wcl_service_capture.py` on `develop`, blob `d79c9318dc4572751e01def2cc0c2a8eb0895969`.
- Existing coverage: PR #190 tests literal duplicate members; this candidate tests *decoded-equivalent* member names (JSON Unicode escape sequences), which must not be confused with duplicate plugin/provider records.
- Proposed PI: reject an untrusted capture containing two object members that decode to the same name, even when their raw spellings differ; report `invalid` and a machine-readable code containing `DUPLICATE`. Never infer that the capture attests a live Paper provider.
- Candidate vectors: top-level `schema_version` vs `schema_\\u0076ersion`; metadata `registration_list_complete` vs `registration_list_\\u0063omplete`; registration `owner_plugin` vs `owner_\\u0070lugin`; plugin `enabled` vs `enabl\\u0065d`.
- Baseline: a structurally valid capture without duplicate members retains its existing status.
- Expected Red: current Python `json.loads` decodes escaped member names before applying last-wins, so the CLI can silently accept the duplicate. These vectors are not authorization for production code changes.
- Next: normative CAT confirmation and formal Gates, then minimal implementation, direct unittest, Paper evidence and integration as applicable.
