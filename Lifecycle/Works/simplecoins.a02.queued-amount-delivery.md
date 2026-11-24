---
cat_version: '0.2'
kind: work
id: simplecoins.a02.queued-amount-delivery
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/648
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

# A02/G3 — actual ordered amount and typed grant delivery across independent saved commits

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/648 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.queued-amount-delivery | simplecoins.a02.paper-saved-batch-product-coordinator;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.queued-amount-delivery.md;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Confirmed behavior and necessary correction

Drive 03 makes offline amount-form reservations mandatory, with minimum-number denominations, and retains undelivered reservations independently. The current product coordinator incorrectly requires a typed denomination, although the authentic ledger and batch planner accept amount grants, and compares the final receipt against whether ALL pending grants for the player are gone. For the oldest pending reservation followed by another pending reservation this is false even after a legitimate completed save/ACK. A later grant must remain pending and must not invalidate the successfully completed earlier grant. No new normative semantics or approval are introduced.

1. With the existing actual detached Paper player and no admitted players, an authentic oldest pending typed grant followed by an amount reservation must save exactly the typed grant and durably ACK only that grant. Proof and ACK run off-main under the same externally supplied protected seal.
2. A successful complete (not partial) commit may hand its drained epoch back on the primary thread exactly once for subsequent work. No resumed epoch before successful verification, no resumed epoch after a partial/failed/stale commit, and no Admission permission or native Agent activation. Reuse the same externally owned fence and re-read the original source for the next grant; never manufacture an alternate epoch or skip earlier pending reservations.
3. The later amount grant is delivered using the existing minimum-coin planner (5200 = 5000 x 1 + 100 x 2), physical placement in vacant slots, authentic PDC, real Paper save, and fresh protected worker proof and ACK. Preserve the previously acknowledged coins and unrelated items. Actual independent JVM restart must prove exact denomination/count, total 15,200C (initial 5,000C + typed 5,000C + amount 5,200C), three durable ACKs and no pending or reissue.
4. This only connects the legitimate existing G3 product stages for ordered reservations. Main-thread pre-save traversal, production scheduler and cancellation, partial-session custody/recovery, full installed-writer coverage, online commands, G0 and admission are not certified here.

**Red before production change**: genuine isolated Paper 26.2 queue phase starts with earlier durable grant1, reserves typed grant2 and amount grant3, saves and runs the product worker. Existing coordinator must fail the first completed grant despite grant2's ACK because grant3 remains pending. Test must inspect exact per-reservation state and fail if the product faults or prematurely issues the later grant. A distinct amount-form case verifies that an untyped amount reservation is rejected before save by the current product. After correction both independent physical and durable oracles must pass; retain all existing real Paper SIGKILL/reboot and domain regressions. No release or main deployment.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-queued-model.json | @work/.cat-flow/cache/a02-queued-vectors.json | @work/.cat-flow/cache/a02-queued-queue.json | — | — | @work/.cat-flow/cache/a02-queued-obligations.json | — | false |
