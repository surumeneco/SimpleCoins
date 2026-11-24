---
cat_version: '0.2'
kind: work
id: simplecoins.a02.bounded-saved-commit-worker
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

# A02/G3 — bounded worker custody and fail-closed handback

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/651 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.bounded-saved-commit-worker | simplecoins.a02.paper-saved-batch-product-coordinator;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.bounded-saved-commit-worker.md;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantCommitWorker.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStage.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantCommitWorkerChecks.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantCommitWorkerTest.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantCommitWorker.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStage.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantCommitWorkerChecks.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantCommitWorkerTest.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Confirmed behavior; bounded off-main saved-commit custody

The confirmed A02/G3 monetary semantics are unchanged. The current saved-batch coordinator can finalize on an ad hoc worker, but has no ownership, backlog, deadline or shutdown protocol. Provide one product-owned worker with at most one outstanding ticket, no queued next grant and a bounded monotonic deadline. No main-thread waiting or filesystem IO is added. A successful ticket is claimable exactly once, only before shutdown. A failure, expiry or shutdown abandons the handback and never permits another issuance on that worker. An operation already in durable ACK when the deadline expires can have an ambiguous disk outcome; do not interrupt durability, promise pending, or automatically retry. Require cold reconciliation. Worker checks deadline before commencing, before ACK, and before accepting completion. The seal stays closed on abandonment; worker faults its fence before retiring if still running. A successful complete batch may return its epoch only after successful claim; partial batch cannot return its epoch. Raw finish remains an internal compatibility path and cannot compete with an owned ticket.

Red: exact Java regression checks absent product worker before implementation; checks cover nonblocking polling, rejection of another in-flight or unclaimed operation, one-shot claim, deadline before result/ACK, shutdown with active or completed ticket, exceptions and successful reuse only after claim. Green: compile and execute the same checks; Gradle JUnit delegates to those checks. The Paper 26.2 saved-batch probe must exercise product-owned worker (not fixture-created verifier thread), prove physical receipt/ACK and reject duplicate finalization. Existing multi-JVM crash and ordered reservation coverage must remain green. No real G3 service/admission is enabled; exhaustive writer closure and pre-save off-main preflight remain open.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-bounded-worker-model.json | @work/.cat-flow/cache/a02-bounded-worker-vectors.json | @work/.cat-flow/cache/a02-bounded-worker-queue.json | — | — | @work/.cat-flow/cache/a02-bounded-worker-obligations.json | — | false |
