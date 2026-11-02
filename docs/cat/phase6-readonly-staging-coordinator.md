---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.settlement.readonly-staging
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: Read-only settlement source staging coordinator (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.settlement | Read-only source staging and fail-closed settlement boundary |
| simplecoins.snapshot | Verified saved-source manifest and closure identity |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| staging.playerdata | manifest includes playerdata and an empty world source | Stage playerdata and mark both source IDs complete only after successful reads | SC-STAGING-01 |
| staging.failure | playerdata malformed or ownership resolver returns unknown | Abort without returning a publishable staged result | SC-STAGING-02 |
| staging.world-completion | world source contains multiple chunks | Mark complete after entire source traversal, never after one chunk | SC-STAGING-03 |
| staging.no-publication | read-only staging coordinator executes | Do not invoke SettlementAccumulator.commit or mutate Paper world | SC-STAGING-04 |

Status: Draft, not Confirmed. Sources: SimpleCoins 02A, 02B, 05_1.0.0_実装計画.md (G0, A02/A03).

## Process
A staging coordinator shall derive required source IDs from a verified manifest, traverse all authoritative saved region/entity/playerdata files once, route each to the existing fail-closed accounting adapters, and mark a world source complete only after its entire file has been read. Playerdata remains world-independent. BlockLocker and mob ownership must be supplied through explicit caller-owned resolvers; no unknown ownership may be guessed. Failure to read or resolve any source must abort staging and must not return a publishable result.

## Boundary
This is **read-only disk staging only**, not a production settlement or publication API. The caller still must establish a consistent world snapshot, reconcile loaded Entities/chunks, resolve current BAN/playerdata and WWC/WWW ownership, verify register sales, and complete formal CAT gates before committing or exposing statistics. The coordinator must never auto-commit or mutate Paper worlds.

## Test model
- A manifest with a saved playerdata source and empty world source stages playerdata and completes both source IDs.
- A malformed playerdata file or unresolved ownership throws and yields no staged result.
- World source completion occurs after traversal, not per chunk.
- No call to SettlementAccumulator.commit is made by the coordinator.

Formal CAT machine gates: not-run; dedicated Chat plugin/Skill unavailable. Draft is not Confirmed.
