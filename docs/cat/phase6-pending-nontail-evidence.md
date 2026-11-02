---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.pending-nontail-durable-evidence
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
  - https://github.com/surumeneco/SimpleCoins/pull/10
  - https://github.com/surumeneco/SimpleCoins/pull/58
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6 — pending grants and non-tail durable evidence (CAT Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| grant-pending-durability | GrantLedger.pending(player) must not expose an in-memory grant list when a prior durable grant record has disappeared or changed |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| GRANT-PENDING-01 | Two grants are reserved, then the first durable record is deleted while the head and tail remain intact | Reject pending() with fail-closed exception; do not return a list based only on memory | missingNonTailRecordMustNotYieldPendingList |
| GRANT-PENDING-02 | Two grants are reserved, then the first durable record is replaced with malformed bytes | Reject pending() rather than return a stale list | corruptNonTailRecordMustNotYieldPendingList |
| GRANT-PENDING-03 | Two grants and all durable evidence are intact | Preserve ordinary pending list with both grants | intactJournalReturnsBothPendingGrants |
| GRANT-PENDING-04 | Durable tail is missing | Preserve existing fail-closed tail rejection | missingTailAlreadyFailsClosed |

## Test vectors

| ID | Setup | Required result |
| --- | --- | --- |
| GRANT-PENDING-01 | reserve seq1/seq2; delete 1.grant; pending(player) | IllegalStateException |
| GRANT-PENDING-02 | reserve seq1/seq2; corrupt 1.grant; pending(player) | IllegalStateException |
| GRANT-PENDING-03 | reserve seq1/seq2; pending(player) | List size 2 |
| GRANT-PENDING-04 | reserve seq1/seq2; delete 2.grant; pending(player) | IllegalStateException |

## Status and scope

**Draft only.** The current checkHealthy() validates the durable head and the most recent grant, not every historical record. Existing PR #58 isolates the non-tail wasApplied() boundary; this Draft isolates the distinct pending() read path. No assertion is made that the correct production strategy must synchronously rescan every record; a verified integrity index or equivalent authoritative proof may satisfy the eventual contract.

CAT Process/PI/TCE confirmation and normative machine Gates, production Green, Paper crash/restart acceptance: **not-run**. Do not merge an expected Red Draft.
