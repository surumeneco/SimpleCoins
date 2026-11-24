---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-scheduled-delivery-lifecycle
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/654
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.protected-off-main-preflight
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — plugin-owned Paper scheduler and protected grant delivery lifecycle

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/654 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-scheduled-delivery-lifecycle | simplecoins.a02.bounded-saved-commit-worker;simplecoins.a02.verified-receipt-planning-view;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-scheduled-delivery-lifecycle.md;src/main/java/co/surumene/simplecoins/paper/PaperSavedGrantDeliveryService.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperSavedGrantDeliveryService.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Confirmed behavior — plugin-owned runtime custody and terminal callbacks

Implement the actual Paper tick driver of the existing protected preflight/save/ACK stages, not a new grant protocol. The product plugin owns one idle, bounded delivery service and closes it on disable and failed startup. No task or worker thread while idle; at most one active delivery, scheduled by Paper on primary. Start only with the enclosing runtime's real audited OPEN fence, authentic durable grant, detached player, zero online players and BLOCKED/SCANNING admission. No automatic join/command registration, activation, fence arm or admission change: the enclosing G0/G3 coverage prerequisite is unchanged. Preflight and ACK run only on the owned worker. Primary polls status, captures/saves in the existing protected coordinator, never joins a worker or scans the ledger/files. Full inventory is a no-progress completion only after the coordinator returns the same epoch. Complete ACK returns that epoch exactly once and only then reports the successful result. A partial proof remains sealed/pending; until actual multi-session custody is implemented, stop and cold reconcile rather than release. Timeout, worker failure, lost blocked admission or unexpected callback fail-stop; no retry, no success callback and no unsafe handback. Close cancels the one scheduled task, abandons worker ticket, prevents stale callbacks and does not wait for durable IO. A close racing with ACK may leave an unresolved durable commit and requires cold reconciliation, never automatic reissue. No new monetary semantics or configuration.

Red: real Paper probe references the missing product runtime service and must not compile before it exists. Green: queued amount/exact reservation cases drive the real service on primary, including duplicate start rejection, one-shot post-ACK return, same fence reuse for second ordered reservation and actual independently reopened saved money/receipt/journal on worker thread. Preserve existing real Paper preflight and crash suite. Product startup/disable owns the service but does not grant G0/G3 or expose public issuance. Formal CAT abstract PI evidence cannot replace physical proof or lifecycle observations.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-scheduled-lifecycle-model.json | @work/.cat-flow/cache/a02-scheduled-lifecycle-vectors.json | @work/.cat-flow/cache/a02-scheduled-lifecycle-queue.json | @repo:SimpleCoins/tests/cat/a02-grant-delivery-junit-binding.json | @work/.cat-flow/generated/A02ScheduledLifecycleDecisionGeneratedTest.java | @work/.cat-flow/cache/a02-scheduled-lifecycle-obligations.json | — | false |
