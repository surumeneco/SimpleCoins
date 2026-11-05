---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.wcl-capture-time-type-observation
status: candidate
process: simplecoins.phase5
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/develop/tools/audit_wcl_service_capture.py
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/phase5-wcl-service-capture-audit.md
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: en
---

# Phase 5: malformed WCL capture time type (code-to-spec Candidate)

## Observation

At source blob `d79c9318dc4572751e01def2cc0c2a8eb0895969`, `audit()` marks missing/non-string `capture.server_start` and `capture.captured_at` as `MISSING_CAPTURE_METADATA`. It then calls `.replace("Z", "+00:00")` on these same values inside a `try` that catches `KeyError`, `TypeError`, and `ValueError`, **not `AttributeError`**.

A capture containing an integer or null timestamp therefore raises `AttributeError` instead of returning the stable status object. In the CLI, `main()` does not catch `AttributeError` either, so the expected invalid status/exit-code boundary is skipped. The extracted source expression with a null `server_start` produced `AttributeError: 'NoneType' object has no attribute 'replace'` under Python 3.

The current `phase5-wcl-service-capture-audit.md` Draft already lists malformed inputs as `invalid` with machine-readable exit codes (WCL-AUDIT-06). That Draft is not a Confirmed normative decision.

## Candidate vectors for review

| ID | Input / premise | Proposed observable outcome | Evidence status |
| --- | --- | --- | --- |
| WCL-TIME-01 | Otherwise valid capture, `server_start: null` | `audit` returns `status: invalid`, no Python exception | Existing source exposes AttributeError; test expected Red |
| WCL-TIME-02 | Otherwise valid capture, `captured_at: 0` | `audit` returns `status: invalid`, no Python exception | Expected Red |
| WCL-TIME-03 | CLI reads a JSON capture with a null timestamp | Parseable `invalid` status and exit 3 rather than traceback | Expected Red |
| WCL-TIME-04 | Complete capture with valid offset-aware ISO timestamps | Existing structurally_complete status preserved | Control, existing behavior |

## Authority and next stage

This is a Candidate characterization and a regression-test proposal. No production source change is included. Confirm or correct the invalid-time contract before implementing. Use the canonical CAT Gate sequence and generated current-item test as applicable; do not mark a hand-written regression or source-level expression check as `process-closure`, `model-conformance`, or Paper runtime evidence. Live WCL registration, JAR provenance and actual Paper capture remain separately unverified.
