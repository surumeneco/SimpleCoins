---
cat_version: '0.2'
kind: work
id: simplecoins.a02.verified-receipt-planning-view
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/651
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.paper-main-worker-saved-commit
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — one verified player-scoped view for receipt planning

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/651 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.verified-receipt-planning-view | simplecoins.a02.paper-saved-batch-product-coordinator;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.verified-receipt-planning-view.md;src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/domain/GrantReceiptState.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java;src/test/java/co/surumene/simplecoins/domain/GrantLedgerVerifiedPlayerSnapshotChecks.java;src/test/java/co/surumene/simplecoins/domain/GrantLedgerVerifiedPlayerSnapshotTest.java;.github/workflows/a02-verified-receipt-planning-view.yml;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/domain/GrantReceiptState.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java | src/test/java/co/surumene/simplecoins/domain/GrantLedgerVerifiedPlayerSnapshotChecks.java;src/test/java/co/surumene/simplecoins/domain/GrantLedgerVerifiedPlayerSnapshotTest.java;.github/workflows/a02-verified-receipt-planning-view.yml;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Confirmed behavior; coherent read-only receipt planning

Existing A02 normative money semantics and same-seal ACK checks remain unchanged. The current Paper inventory preflight calls receipt verify/begin/advance/complete and pending independently; each call fully scans all durable grant records (including unrelated owners). This is multiplicative world-size IO on the primary-thread pre-save path. Within one logical *read-only* planning operation, obtain one authentic, immutable, player-scoped snapshot by fully checking every persisted record once under the ledger monitor. Reuse the captured ordered pending list and receipt/identity predicates to derive the canonical next batch. Public receipt APIs continue to capture a fresh verified view per invocation; independent public ledger reads and all writes/ACK retain their existing durable verification. No snapshot is a current-world witness, durable commit receipt, or authorization to bypass the installed-writer fence. The snapshot cannot be fabricated by callers and cannot see later reservations or ACK; this isolation is intentional. The actual save-to-ACK pipeline must still reverify current authoritative evidence.

Red: Java check on the new immutable snapshot contract is not compilable on the exact pre-change tree because the snapshot API does not exist. Green: execute the same checks on real filesystem ledger; verify interleaved owners, exact/amount forms, canonical partial and final cursor, foreign/forged/skipped grant rejection, immutable snapshot, later ACK/reservation isolation, and full integrity failure on unrelated non-tail corruption. The same checks run via JUnit and real Paper ordered-reservation probe. No service is activated, no weakened corruption detection, no release or admission promotion. Moving the remaining single full scan off-main is a separate prerequisite, not achieved here.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-planning-view-model.json | @work/.cat-flow/cache/a02-planning-view-vectors.json | @work/.cat-flow/cache/a02-planning-view-queue.json | — | — | @work/.cat-flow/cache/a02-planning-view-obligations.json | — | false |
