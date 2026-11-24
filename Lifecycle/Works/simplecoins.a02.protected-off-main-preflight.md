---
cat_version: '0.2'
kind: work
id: simplecoins.a02.protected-off-main-preflight
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/653
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.bounded-saved-commit-worker
  - simplecoins.a02.verified-receipt-planning-view
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — same-seal off-main preflight, exclusive main save and durable ACK

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/653 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.protected-off-main-preflight | simplecoins.a02.bounded-saved-commit-worker;simplecoins.a02.verified-receipt-planning-view;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.protected-off-main-preflight.md;src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbt.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantCommitWorker.java;src/test/java/co/surumene/simplecoins/domain/ProtectedGrantPreflightChecks.java;src/test/java/co/surumene/simplecoins/domain/ProtectedGrantPreflightTest.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantCommitWorkerChecks.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbt.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantCommitWorker.java | src/test/java/co/surumene/simplecoins/domain/ProtectedGrantPreflightChecks.java;src/test/java/co/surumene/simplecoins/domain/ProtectedGrantPreflightTest.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantCommitWorkerChecks.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Confirmed behavior — same protected generation from preflight through actual save and ACK

No change to the A02 monetary semantics or coverage prerequisite. The existing first-batch coordinator verifies the entire ledger, discovers saved files and parses NBT on Paper primary before physical save. Capture immutable storage fingerprints and receipt on primary only after closing and draining the supplied, independently audited writer epoch. Off-main, under that same drained seal, verify source identity, content witness, saved receipt, full durable journal view and pure slot plan. Before releasing the read lease, require unchanged source and an unexpired ticket. No Bukkit Player, World or ItemStack may be accessed by that worker. The already protected epoch must never reopen in the interval between read and save. The main thread receives one bounded worker ticket, checks same player, current receipt/storage and current journal view, then performs real inventory/PDC and saveData under a thread-scoped exclusive write interval inside the same seal. Other roots remain refused; nesting only from a live same-thread parent. No waiting for another writer, filesystem scan, journal scan or ACK on main. The exclusive write interval ends before returning the existing sealed batch for off-main final durability and ACK. Full inventory returns no physical mutation and resumes only after consuming its valid ticket. Failures or stale/expired/closed tickets fault the seal, never retry physical issuance. Existing legacy synchronous methods stay unregistered for regression only. A bounded ticket's expiry applies to unclaimed success too. A non-I/O revision check detects in-process journal changes after preflight, but is NOT independent disk validation or full writer-coverage proof; all journal writers and external writes must be included in the enclosing protection.

Red: local pure Java checks fail compilation before the sealed exclusive writer/current snapshot APIs exist; actual Paper probe invokes the new worker preflight path and expects pre-save pending status, no money added before the main callback, real saved physical/receipt ACK after final worker, and duplicate completion rejected. Green: same Java checks for external root rejection, stale seal, nested writer, exclusive read/commit exclusion, fault after exception, journal view changes, and bounded unclaimed success. Existing real Paper crash suite remains required. The existing CAT JUnit binding covers only abstract A02 PI decision predicates, not the physical preflight/worker/lease behavior; generated PI tests cannot replace the actual Paper and direct Java regression. No production admission, writer arm, main release, or complete G3 claim.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-protected-preflight-model.json | @work/.cat-flow/cache/a02-protected-preflight-vectors.json | @work/.cat-flow/cache/a02-protected-preflight-queue.json | @repo:SimpleCoins/tests/cat/a02-grant-delivery-junit-binding.json | @work/.cat-flow/generated/A02ProtectedPreflightDecisionGeneratedTest.java | @work/.cat-flow/cache/a02-protected-preflight-obligations.json | — | false |
