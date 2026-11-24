---
cat_version: '0.2'
kind: work
id: simplecoins.a02.durable-partial-batch-custody
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/656
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.first-batch-durable-intent-recovery
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — durable partial-batch custody and continuation

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/656 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.durable-partial-batch-custody | simplecoins.a02.bounded-saved-commit-worker;simplecoins.a02.verified-receipt-planning-view;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.durable-partial-batch-custody.md;src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/domain/GrantReceiptState.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedGrantDeliveryService.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java;src/test/java/co/surumene/simplecoins/domain/GrantPartialCustodyChecks.java;src/test/java/co/surumene/simplecoins/domain/GrantPartialCustodyTest.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAuditTest.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/domain/GrantReceiptState.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedGrantDeliveryService.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java | src/test/java/co/surumene/simplecoins/domain/GrantPartialCustodyChecks.java;src/test/java/co/surumene/simplecoins/domain/GrantPartialCustodyTest.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAuditTest.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Confirmed behavior — preserve verified partial value across sessions

A verified partial physical save records a durable cumulative progress floor in the same authenticated grant record, before an intent may be cleared or the sealed epoch handed back. Legacy v1/v2 records decode with progress zero. Startup audit rejects a missing authoritative playerdata file for any durably confirmed partial, not only a final ACK. A fresh receipt must not roll back behind the floor, even if the separate intent disappeared; a completed pending receipt may still be recovered, not blindly ACKed. A new batch intent is tied to the exact previous confirmed progress, grant and source path; a stale earlier intent left after a confirmed partial can be removed only after current receipt agrees with the durable floor. When a save advanced beyond the floor, reconstruct the exact intended physical transition and verify without rewriting. A current partial equal to the confirmed floor may start another batch with a new inventory snapshot; legitimate movement of earlier physical coins is not itself a new issuance. Confirm only after the same protected actual saved partial proof, before cleanup; continue pending until final durable ACK. Empty capacity is a no-write outcome. Legacy isolated methods without a durable intent remain unable to authorize partial handback. A verified partial handback releases only the writer epoch, NOT player admission or proof of normal journal custody. No full writer closure, give/join, admission or release claim.

Red: durable partial APIs absent; prove reload, stale/foreign view, rollback, wrong active identity, forged progress, legacy compatibility, and intent crash-boundary cleanup. Green: exact prefix and monotonic custody, old/new batch intent recovery, no reissue and final ACK in real Paper. Normal journal accounting for exposed partials and actual login/close triggers remain separate.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-partial-custody-model.json | @work/.cat-flow/cache/a02-partial-custody-vectors.json | @work/.cat-flow/cache/a02-partial-custody-queue.json | @repo:SimpleCoins/tests/cat/a02-grant-delivery-junit-binding.json | @work/.cat-flow/generated/A02PartialCustodyDecisionGeneratedTest.java | @work/.cat-flow/cache/a02-partial-custody-obligations.json | — | false |
