---
cat_version: '0.2'
kind: work
id: simplecoins.a02.coherent-startup-receipt-audit
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/655
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.verified-receipt-planning-view
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — coherent bounded startup receipt audit

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/655 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.coherent-startup-receipt-audit | simplecoins.a02.bounded-saved-commit-worker;simplecoins.a02.verified-receipt-planning-view;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.coherent-startup-receipt-audit.md;src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java;src/test/java/co/surumene/simplecoins/domain/GrantLedgerVerifiedPlayerSnapshotChecks.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAuditTest.java | src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java | src/test/java/co/surumene/simplecoins/domain/GrantLedgerVerifiedPlayerSnapshotChecks.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAuditTest.java |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Confirmed behavior — one coherent read-only journal generation for the startup audit

The already registered read-only saved receipt universe audit must preserve the existing complete-history validation, player-specific receipt checks, missing/rolled-back acknowledged playerdata rejection, immutable source witness and unchanged durable journal recheck. Capture the full authenticated journal once and group its history into immutable player-scoped views, including an empty view for unknown UUIDs. Each saved player is validated against that same captured revision; final full durable verification must reject foreign or stale snapshots, any journal mutation or on-disk discrepancy, including unrelated players. No per-player disk journal scan is needed. No write/ACK/admission/settlement authorization derives from this view. This is a performance-preserving implementation of the existing contract, not a change of grant semantics or source authority.

Red: missing coherent universe API; empty/foreign/immutable view, revision changes including a later ACK, unrelated on-disk corruption and actual startup audit mutation during scan. Green: existing direct Java and saved receipt universe audit regressions, including a mutation injected between player observations. Existing full ledger verification at capture and final validation remains.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-coherent-startup-audit-model.json | @work/.cat-flow/cache/a02-coherent-startup-audit-vectors.json | @work/.cat-flow/cache/a02-coherent-startup-audit-queue.json | @repo:SimpleCoins/tests/cat/a02-grant-delivery-junit-binding.json | @work/.cat-flow/generated/A02CoherentStartupAuditGeneratedTest.java | @work/.cat-flow/cache/a02-coherent-startup-audit-obligations.json | — | false |
