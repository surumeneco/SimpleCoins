# Phase 5 WCL malformed-record characterization (CAT Draft)

Status: Draft / code-to-spec characterization, not Confirmed.
Task: TASK-20261101-004.
Source: `tools/audit_wcl_service_capture.py` on `develop`, blob `d79c9318dc4572751e01def2cc0c2a8eb0895969`.
Fixture: `tests/test_audit_wcl_service_capture.py`, blob `021a36ed693e660c20a977da5d128c65e34dd04b`.

## Boundary

The read-only audit consumes externally supplied JSON. Invalid nested records must not crash the audit, silently become verified ownership, or suppress independent missing-provider/plugin evidence. Structural completeness is not runtime provenance and is not permission to count assets.

## Characterization vectors

- Non-object plugin record coexists with an otherwise valid plugin inventory: `INVALID_PLUGIN_RECORD`, invalid.
- Non-object registration record coexists with valid provider registrations: `INVALID_REGISTRATION`, invalid.
- A required plugin with a non-boolean enabled flag: `INVALID_PLUGIN_ENABLED`, invalid.
- A required provider with a non-boolean owner_enabled flag: `INVALID_PROVIDER_ENABLED`, invalid.
- Malformed record plus missing required provider: both `INVALID_REGISTRATION` and `REQUIRED_PROVIDER_MISSING`, invalid.

These are observations of the current audit contract only. Do not promote to Confirmed or modify production semantics without the authorized CAT process. The vectors do not attest to Paper runtime identity, persisted animal ownership, or provider authenticity.

## Verification

Execute the accompanying Python unittest against the exact source revision and record the result. Formal CAT Gate and Paper integration remain separate.
