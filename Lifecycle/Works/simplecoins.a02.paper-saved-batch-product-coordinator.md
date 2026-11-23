---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-saved-batch-product-coordinator
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/646
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.final-partial-protected-ack
  - simplecoins.a02.real-paper-protected-saved-stages
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — connect authentic Paper grant plan, physical save, protected readback and journal ACK

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/646 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-saved-batch-product-coordinator | simplecoins.a02.final-partial-protected-ack;simplecoins.a02.real-paper-protected-saved-stages |

## Technologies
| Technology |
| --- |
| Paper26.2 Java25, authentic GrantLedger/PDC/UUID.dat, real Paper detached Player.saveData, shared WriterEpochFence, source discovery and physical saved-readback, CI crash/reboot |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-saved-batch-product-coordinator.md;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a02.real-product-full-chain | tests | github-actions | @repo:SimpleCoins | Paper26.2 save/reload grant probe | PAPER_A02_PRODUCT_COORDINATOR_SAVED_ACK_PASSED;PAPER_A02_PRODUCT_COORDINATOR_REBOOT_NO_REISSUE_PASSED | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java | smoke-product-coordinator/*.log | 1800 |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Approved meaning and release-critical implementation scope

The Confirmed A02 Process/PI/TCE and current Drive 02B/03 require authentic reserved physical coins plus an authentic playerdata receipt to survive an actual Paper save before the issuance journal records ACK. Current product code has `PaperGrantInventorySlotPlan`, `PaperGrantInventoryMaterializer`, `PaperGrantReceiptPdc`, protected partial `verifyPartial`, protected complete `finish`, and protected final-after-partial `finishLastPartialBatch`, but no **single product call path** that actually places coins, calls Paper `Player.saveData`, seals the writer interval and delegates to those product protected stages. Existing real Paper probe assembled these steps in test-only source.

Implement unregistered `PaperGrantSavedBatchCoordinator.saveAndFinalizeFirstBatch`: under a preexisting externally audited OPEN writer fence (it must **never arm itself**), on the Paper primary thread, require a genuinely detached player and zero online players, an authentic pending GrantLedger reservation/UUID, current PDC receipt equal to fresh real discovery-origin saved UUID.dat, no prior active partial receipt, a strictly unchanged 36-slot plan, then create genuine ItemStacks, set storage and receipt, call actual `Player.saveData`. Close writer admission and require zero existing admitted roots (fail closed rather than blocking main thread), rediscover original UUID.dat after save and call **existing product** saved-batch stage: single-batch completed `SavedGrantProtectedAckStage.finish` durable ACK or incomplete first batch `SavedGrantProtectedPartialStage.verifyPartial` and keep pending. The new coordinator must not duplicate low-level `ledger.acknowledge` and must never infer success from `saveData` void return; after any mutation/save/proof failure, irrevocably invalidate the caller-supplied fence and report ambiguous durability so caller must cold-recover rather than mint again. If inventory is full, return `NoCapacity` with ledger pending and neither save nor PDC mutation. Do not allow a previously active partial receipt through this first-batch endpoint: full multi-session continuation requires independently durable earlier-batch custody evidence and remains a separate release-critical follow-up. Do not fabricate a writer coverage token or open Admission.

**Red:** In a disposable real Paper 26.2 playerdata copy from prior successful first issuance, reserve authentic second 5000C grant and call `PaperGrantSavedBatchCoordinator.saveAndFinalizeFirstBatch` through reflection before product code exists. Require an actual new slot1 5000C, saved signed receipt seq2 and durable ACK on that copy: missing class/method must fail for that reason. **Green:** same Paper JVM executes actual source/plan/materializer/save/seal/full-protected stage, reads both original slot0 and new slot1 plus STONE, durable ledger history2/pending0. Another independent JVM on the copy must prove no duplicate issuance or torn receipt. Existing 10-JVM + 2 alternate final-partial JMV SIGKILL/recovery baseline must remain green. Include adverse no-capacity and prior-active receipt rejection in JUnit or actual Paper fixture if feasible.

This connects a **real product monetary delivery chain** toward release rather than another stand-alone protected readback experiment. It does **not** claim all installed Paper/Plugin/async/direct-file writer coverage; test-only fence is artificially armed, and the normal startup Agent remains PASSIVE. The production entrypoint must stay unregistered until comprehensive cross-writer closure, journal handoff, admission and live login scheduling are verified. Main, VPS and release are not modified.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-product-batch-model.json | @work/.cat-flow/cache/a02-product-batch-vectors.json | @work/.cat-flow/cache/a02-product-batch-queue.json | — | — | @work/.cat-flow/cache/a02-product-batch-obligations.json | — | false |
