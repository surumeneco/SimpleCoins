---
cat_version: '0.2'
kind: work
id: simplecoins.a02.protected-saved-ack-bridge
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/614
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.grant-save-commit-implementation
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02 / G3 — protected saved-file flush → authoritative fresh readback → single durable grant ACK

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/614 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.protected-saved-ack-bridge | simplecoins.a02.grant-save-commit-implementation;simplecoins.a02.cooperative-drain-verify-ack |

## Technologies
| Technology |
| --- |
| Java25 JUnit; Paper 26.2 NBT persisted compressed playerdata; exact GrantLedger and WriterEpochFence; Unix file/dir fsync |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.protected-saved-ack-bridge.md;src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStage.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;.github/workflows/a02-protected-saved-ack-bridge.yml | src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStage.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;.github/workflows/a02-protected-saved-ack-bridge.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a02.protected-ack.junit | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.paper.SavedGrantProtectedAckStageTest | successfullyFsyncsFreshProofAndAcksWithinOneSeal;refusesOpenUnsealedAndWrongGrantOrPartial;refusesMissingPhysicalCoinOrChangedStorage | src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java | build/test-results/test/TEST-co.surumene.simplecoins.paper.SavedGrantProtectedAckStageTest.xml | 300 |

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

## Confirmed A02 and technical implementation boundary

The Confirmed A02 Process/PI/TCE and Drive 02B/03 require one genuinely reserved issuance grant to remain **pending** until its authentic physical money and a completed receipt are durably saved in the same playerdata generation and the relevant writer admissions/drain are protected through durable `GrantLedger` ACK. The existing `SavedGrantDeliveryFileReadback.verify` proves physical money, receipt, original unaffected storage and saved source provenance, but deliberately does **not** fsync or ACK. The pre-existing low-level `GrantLedger.acknowledge(grant, receiptSequence)` intentionally accepts a caller-provided sequence; #510 demonstrates that bypassing an end-to-end generation guard with a stale proof can durably false-ACK. No generic boolean claim or old file hash is an adequate substitute.

This independent bounded implementation introduces an **internal package-private, unregistered** product assembly stage; it will not be an enabled player command, plugin listener or production G3 public authorization. Its sole responsibility is to compose existing mandatory operations in a single already supplied `WriterEpochFence.Seal` scope:
1. Require an authentic *pending* `GrantLedger.Grant`, matching player and a `PaperGrantInventorySlotPlan.nextReceipt()` that is completed, non-active, with matching grant sequence. Reject no-op, incomplete and counterfeit reservations without mutating playerdata or ledger.
2. Require the supplied actual `WriterEpochFence.Seal` to be **SEALED and drained** before first file inspection. A seal of the wrong fence, stale seal, OPEN state or outstanding writer must be rejected. This check is local to the connected fence and does not prove entire installed server writer coverage.
3. Require the `WorldStorageDiscovery.Manifest` to be an origin-backed, current discovery including the canonical UUID playerdata and use only that verified path. Use Java FileChannel to force the **actual existing saved playerdata file and its parent directory** on a supported filesystem, without following source/ancestor symlinks. On any force error, refuse ACK and retain pending.
4. Within the unchanged seal, perform **fresh** `SavedGrantDeliveryFileReadback.verify` against that file and the original saved NBT root; require that the returned completed receipt matches the pending grant and no unrelated item changed. Re-check the seal immediately before invoking `GrantLedger.acknowledge` and after its return; a mismatch fails closed. The stage does not itself execute `Player.saveData`, construct paper items or permit a player.
5. Any failure invalidates the local fence and never re-opens it. If the ledger ACK itself might have persisted before a later failure, report **indeterminate**, do not falsely promise that the reservation remains pending. This stage intentionally cannot claim a complete 24-JAR/native/async writer roster, crash atomicity across two stores or G3 release.

**Negative TDD oracle:** before implementation, the new JUnit must fail for the missing assembler/ack and, with a deliberately missing physical coin or unsealed fence, must leave the real persisted `.grant` reservation pending. A successful bounded Green does not mean that an ordinary user can trigger distribution: this bridge remains package-private and no production startup hook activates the current PASSIVE eight-family Agent. The global writer closure, own persistent transaction/rollback orchestration, main-thread/async handoff, complete give and journal once-only integration, JE/BE and full G3 remain independent dependencies.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-protected-ack-model.json | @work/.cat-flow/cache/a02-protected-ack-vectors.json | @work/.cat-flow/cache/a02-protected-ack-queue.json | — | — | @work/.cat-flow/cache/a02-protected-ack-obligations.json | — | false |
