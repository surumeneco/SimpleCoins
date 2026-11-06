---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.startup-admission-barrier
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6 startup settlement admission barrier (Experimental Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| SimpleCoinsPlugin | Prevent admissions until authoritative settlement is verified and published |
| PlayerConnectionValidateLoginEvent | Paper 26.2 login validation adapter, not implemented in this prototype |
| SettlementScanRunner | Disk staging only; loaded-world reconciliation and publication remain separate |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| ADMIT-01 | Initial state or active scan | Admission remains denied | initialGateDenies |
| ADMIT-02 | Stale generation completion | Previous scan cannot open admission | staleCompletionRejected |
| ADMIT-03 | Scan incomplete or players online | Admission remains denied and failure is terminal | incompleteOrOnlineRejection |
| ADMIT-04 | Current verified scan with zero players | Admission can be opened by controlled publication | verifiedRelease |
| ADMIT-05 | Shutdown or later serious consistency fault | Admission becomes denied again | stopOrFatalRevokes |

## Scope and limitations

This is a **Draft** of the proposed gate and an offline pure-Java spike, not a confirmed Process/PI/TCE or production implementation. The spike uses a boolean input for snapshot verification; authoritative validation, actual Paper listener registration, all online player eviction, disk-vs-live source coherence, and atomic snapshot publication remain unimplemented. The tests are direct JDK21 assertions, not a JUnit/CAT Gate or live server validation. No release/merge authorization follows.
