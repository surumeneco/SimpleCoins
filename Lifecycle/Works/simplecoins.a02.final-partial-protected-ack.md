---
cat_version: '0.2'
kind: work
id: simplecoins.a02.final-partial-protected-ack
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/643
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.protected-partial-saved-progress
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02 / G3 — final partial-batch persisted save → protected fresh proof → durable ACK

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/614 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.final-partial-protected-ack | simplecoins.a02.protected-partial-saved-progress;simplecoins.a02.real-paper-protected-saved-stages |

## Technologies
| Technology |
| --- |
| Java25 JUnit; Paper 26.2 NBT persisted compressed playerdata; exact GrantLedger and WriterEpochFence; Unix file/dir fsync |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.final-partial-protected-ack.md;src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStage.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStage.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a02.final-partial.junit | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.paper.SavedGrantProtectedAckStageTest | finalPartialBatchFreshSaveAcksWithoutReissuingEarlierCoins;finalPartialBatchMissingOrChangedCoinFailsClosed;singleBatchAckStillRejectsActivePriorCursor | src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java | build/test-results/test/TEST-co.surumene.simplecoins.paper.SavedGrantProtectedAckStageTest.xml | 300 |

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

## Confirmed A02 meaning and bounded final-batch implementation

The already user-Confirmed A02/G3 Process/PI/TCE and Drive 03/04/05 require exactly-once physical issuance across multi-batch inventory exhaustion and restart. `SavedGrantProtectedPartialStage.verifyPartial` proves one actual saved incomplete batch without ACK; `SavedGrantProtectedAckStage.finish` guards a **single-batch-only** completed receipt+physical save and ACK in one lease. Real Paper's first 128 of 130 coins already use the partial stage (#643), but its resumed final two coins still call the low-level journal ACK independently of any product protected stage. This Work closes that independent last-batch bridge only, without changing agreed semantics.

Introduce an **internal unregistered** `SavedGrantProtectedAckStage.finishLastPartialBatch` taking the same inputs as `finish`. Unlike the single-batch method, it requires an **authentic active previous receipt** with the same grant sequence and UUID, strictly positive partialValue less than grant amount, an inactive completed next receipt for that same grant, nonempty planned physical placements and pending exact reservation. Calculate remaining value with exact arithmetic. Reuse the same continuous `WriterEpochFence.withDrainedLease`, origin-backed source witness, file and parent-directory `FileChannel.force(true)`, fresh `SavedGrantDeliveryFileReadback.verifyTransition` and short `withDrainedSeal` durable `GrantLedger.acknowledge` instead of a new independent callback implementation. Crucially require newly saved coin value equals exactly `grant.amount() - prior.partialValue`; preserved preexisting slots and receipt are checked by the real transition verifier. Reject wrong receipt, stale/foreign seal, altered old 64-coin stack, missing last 2-coin stack, changed unrelated inventory and other mismatches without ACK. Any failure invalidates local fence; ACK persistence failure needs cold reconciliation, not retry issuance.

This scoped method **does not** prove the historical provenance or custody of older partial amounts solely from a last-batch receipt: those are independently required previously saved/verified source data plus a *complete* installed writer lock and continuing same-generation ownership. It must not be called when that proof is unavailable. Do not connect to SimpleCoinsPlugin, commands, AdmissionPermit, G0, runtime writer arming or a public distribution service. Product writer universe remains incomplete; the supplied local `Seal` is only a bounded evidence step.

**Expected-reason TDD Red:** expand real compressed NBT JUnit fixture with an authentic 65×5000C grant, first complete 64-coin active receipt persisted in slot0, final 1-coin saved in previously vacant slot1 with completed receipt. Call missing `finishLastPartialBatch` reflectively; a genuine unimplemented method must fail. Existing single-batch `finish` continues to reject the active predecessor. **Green:** real file+dir force, receipt+both physical coin slots and STONE preserved, one durable ACK and no re-issuance, same SEALED fence. Negative tests reject absent newly added coin and changed prior stack; pending remains after reopen and fence FAULTED. Full root tests and actual Paper ten-JVM crash path must remain Green.

For real Paper evidence, duplicate the disposable world **after fifth partial-save SIGKILL**, run an isolated alternate sixth Paper boot on the copy without SIGKILL: finish exactly the last 2 coins using the product final-batch helper, then restart the copy and verify durable third ACK and original 660000C without reissue. Preserve the original sixth SIGKILL-before-ACK → seventh ACK-only recovery on the original world, with identical preexisting markers. All alternate boots require independent loopback/no-player Guard. This is a test-only branch fork of the same real saved scenario, not production enablement. Do not merge main, deploy or claim full G3 closure.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-final-partial-model.json | @work/.cat-flow/cache/a02-final-partial-vectors.json | @work/.cat-flow/cache/a02-final-partial-queue.json | — | — | @work/.cat-flow/cache/a02-final-partial-obligations.json | — | false |
