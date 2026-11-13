---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.wcl.provider-identity-exact-match
status: draft
process: simplecoins.wcl.service-capture-audit
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: WCL provider / plugin identity exact-match characterization

## Scope

Read-only code-to-spec characterization of `tools/audit_wcl_service_capture.py` at develop blob `d79c9318dc4572751e01def2cc0c2a8eb0895969`. The structural audit is not a live Paper service attestation. No player data is read. This Draft does not change the accepted provider contract or authorize production changes.

## Observed behavior and candidate vectors

| Vector | Mutation of a structurally complete capture | Existing source outcome |
| --- | --- | --- |
| WCL-IDENTITY-01 | Duplicate required plugin record, with a conflicting JAR identity | `invalid`, `DUPLICATE_PLUGIN` |
| WCL-IDENTITY-02 | Add unexpected provider while keeping required providers | `incomplete`, `UNEXPECTED_PROVIDER` |
| WCL-IDENTITY-03 | Change `wwc` provider ID to `WWC` | `incomplete`, `REQUIRED_PROVIDER_MISSING` and `UNEXPECTED_PROVIDER` |
| WCL-IDENTITY-04 | Add whitespace to a required plugin name | `incomplete`, `REQUIRED_PLUGIN_MISSING` |
| WCL-IDENTITY-05 | Duplicate provider plus missing required provider | `invalid`, `DUPLICATE_PROVIDER`, with missing-provider evidence preserved |

These vectors test existing source semantics, not a new policy on whether aliases or extra providers should be accepted. They complement existing basic missing/duplicate-provider tests by exercising simultaneous evidence and identity alias boundaries.

## Status and limits

Draft / unconfirmed. Formal CAT Process/PI/TCE closure, TestModel/vectors/Queue/current-item gates, Paper live capture, and integration validation remain not-run. Expected outcomes require direct verification against the pinned source and current branch; no test result is asserted here.
