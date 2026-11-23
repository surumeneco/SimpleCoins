---
cat_version: '0.2'
kind: work
id: simplecoins.a02.protected-partial-saved-progress
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/636
  - https://github.com/surumeneco/SimpleCoins/pull/638
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.protected-saved-ack-bridge
  - simplecoins.a02.atomic-persisted-ack-lease
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — protect real partial-batch saved physical coin + receipt, with no ACK

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/636 |
| https://github.com/surumeneco/SimpleCoins/pull/638 |
| https://github.com/surumeneco/SimpleCoins/pull/467 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.protected-partial-saved-progress | simplecoins.a02.protected-saved-ack-bridge;simplecoins.a02.atomic-persisted-ack-lease |

## Technologies
| Technology |
| --- |
| Java25 JUnit; genuine compressed UUID.dat/GrantLedger, canonical 65x5000C partial batch; existing WriterEpochFence exclusive drained IO lease and saved readback |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.protected-partial-saved-progress.md;src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedPartialStage.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;.github/workflows/a02-protected-partial-saved-progress.yml | src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedPartialStage.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;.github/workflows/a02-protected-partial-saved-progress.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a02.protected-partial-real-playerdata | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.paper.SavedGrantProtectedAckStageTest | partialRealSavedBatchMaintainsPendingNoAck;partialSavedCoinMissingFaultsAndKeepsPending | src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java | build/test-results/test/TEST-co.surumene.simplecoins.paper.SavedGrantProtectedAckStageTest.xml | 300 |

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

## Approved meaning and bounded product implementation

The existing Confirmed A02 Process/PI/TCE (especially `v1p/r1p`) requires that physically saved **partial** delivery be tracked by a real PDC cursor and remain in the authentic pending GrantLedger, **never ACK** before the complete grant is proven. PR #636/#638 implemented a real protected saved proof + durable ACK for the **single-batch complete** path. It explicitly rejects a partial next receipt, correctly, but does not yet offer a product component to read back a genuine partial batch under the same writer epoch.

Add only a package-private, **unregistered** `SavedGrantProtectedPartialStage.verifyPartial`: accept an authentic pending GrantLedger handle, a legitimate one-batch `PaperGrantInventorySlotPlan` whose `nextReceipt` remains active and whose cumulative partial value advances but is strictly below the grant total, a real discovery-origin `WorldStorageDiscovery.Manifest`, real saved-before root, a preexisting already drained `WriterEpochFence.Seal` and read budgets. In the **same exclusive withDrainedLease** while new writer admission is blocked, capture a new real `SavedPlayerdataContentWitness`, file+parent directory `FileChannel.force(true)`, fresh UUID.dat decode and `SavedGrantDeliveryFileReadback.verify` of newly placed physical PAPER/count/model/PDC and same-root persisted receipt, prevent unplanned inventory/ender/equipment changes, require unchanged witness+sealed epoch. Ensure the genuine **newly saved coins' sum equals exactly the canonical cursor increment** (rather than just a plausible receipt). Return the fresh verified Proof. Do not call `ledger.acknowledge`, reserve another grant, alter playerdata, mutate Paper inventory, open Admission or start another writer epoch.

A prior active receipt may resume a subsequent bounded batch if verified against the real journal, but the caller remains responsible for independently establishing that earlier saved partial progress is authentic and that the player could not handle an unverified physical state. Never claim that a single final-batch read proves physical custody for already moved earlier batches.

**Expected-reason Red**: existing real compressed UUID.dat JUnit fixture is expanded to write a canonical 64-coin 5000C stack for a typed 65-coin pending reservation. The genuine `PaperGrantInventorySlotPlan` with one free slot produces `nextReceipt.active=true` and progress 320,000C, with 5,000C still pending. Test fails because the protected partial-stage product class is absent, not from broken NBT/journal setup. **Green**: fresh same-root receipt and exactly 64 physical coins, pending reservation still durable after reopening GrantLedger, no ACK, writer epoch remains sealed, STONE untouched. A receipt-only save without the physical 64 coins must fail and fault fence, with pending reservation preserved. Existing complete single-batch 5000C ACK + concurrent resume test must remain Green, and the full root suite and A02 CAT Process Closure/Model Conformance must pass. TCE `v1p` remains the normative case; do not change TCE, promote Drafts, or fabricate full G3 Gate proof.

This new helper is only a bounded physical readback step; actual grant continuation, Paper give/save coordinator, multiple login sessions, partial movement across saves, crash reconciliation/ACK, complete installed writer roster, normal journal, full G0/G3 and release remain unimplemented. Maintain PASSIVE Agent, startup default deny, `main`, VPS unchanged.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-partial-stage-model.json | @work/.cat-flow/cache/a02-partial-stage-vectors.json | @work/.cat-flow/cache/a02-partial-stage-queue.json | — | — | @work/.cat-flow/cache/a02-partial-stage-obligations.json | — | false |
