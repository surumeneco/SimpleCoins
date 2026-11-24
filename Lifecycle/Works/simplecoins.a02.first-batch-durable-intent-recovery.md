---
cat_version: '0.2'
kind: work
id: simplecoins.a02.first-batch-durable-intent-recovery
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
  - simplecoins.a02.paper-scheduled-delivery-lifecycle
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — durable first-batch intent and no-remint recovery

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/655 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.first-batch-durable-intent-recovery | simplecoins.a02.bounded-saved-commit-worker;simplecoins.a02.verified-receipt-planning-view;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.first-batch-durable-intent-recovery.md;src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/domain/PlayerDataReader.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadback.java;src/test/java/co/surumene/simplecoins/domain/GrantFirstBatchIntentChecks.java;src/test/java/co/surumene/simplecoins/domain/GrantFirstBatchIntentTest.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/domain/PlayerDataReader.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadback.java | src/test/java/co/surumene/simplecoins/domain/GrantFirstBatchIntentChecks.java;src/test/java/co/surumene/simplecoins/domain/GrantFirstBatchIntentTest.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Confirmed behavior — first-batch durable intent, recovery without another physical issuance

The protected preflight must persist the actual bounded compressed prior playerdata in an exclusive, checksummed record bound to the authentic pending grant, UUID and canonical discovered path before any real save. Reconstruct the canonical first-batch plan from the prior NBT storage vacancies and current verified ledger; require exact equality with the initial Paper-derived placements. No intent is created for an empty plan. A repeat preflight may reuse an immutable intent only if the real saved receipt and preserved storage still match the prior state; if the real receipt already equals the exact planned next state, independently verify the physical transition against the persisted prior root and skip all physical writes. A receipt with no intent, mismatch, invalid data or unexpected physical storage is rejected, not reissued. The worker still performs the existing protected file/dir fsync and fresh readback before ACK. A partial remains pending and sealed; this Work does not grant cross-session normal-mutation custody or admission. After verified complete durable ACK, the redundant intent may be removed; cleanup failure is fail-closed and never causes another issue. No semantic change, new public command, writer coverage or release claim.

Red: missing first-batch intent APIs; actual file round-trip, immutable write, foreign/misnamed and corruption rejection, stale view, and no intent after ACK. Green: direct Java and Paper regression with real persisted prior root, exact saved next state recovered through product scheduler without reissuing. Physical verification remains under same comprehensive externally provided writer seal.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-durable-intent-model.json | @work/.cat-flow/cache/a02-durable-intent-vectors.json | @work/.cat-flow/cache/a02-durable-intent-queue.json | @repo:SimpleCoins/tests/cat/a02-grant-delivery-junit-binding.json | @work/.cat-flow/generated/A02DurableIntentDecisionGeneratedTest.java | @work/.cat-flow/cache/a02-durable-intent-obligations.json | — | false |
