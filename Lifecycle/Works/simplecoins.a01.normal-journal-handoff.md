---
cat_version: '0.2'
kind: work
id: simplecoins.a01.normal-journal-handoff
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010
  - https://github.com/surumeneco/SimpleCoins/pull/637
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.atomic-sealed-publication
  - simplecoins.a01.saved-staged-publish-bridge
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01/A11 — atomic protected startup to normal journal-backed accounting

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/pull/637 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.normal-journal-handoff | simplecoins.a01.atomic-sealed-publication;simplecoins.a01.saved-staged-publish-bridge;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit; real saved NBT and durable NormalMutationJournal; exclusive epoch and immutable accounting snapshot |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.normal-journal-handoff.md;src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java;src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java;src/main/java/co/surumene/simplecoins/domain/NormalSettlementSession.java;src/main/java/co/surumene/simplecoins/domain/SettlementSnapshotPublisher.java;src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java;.github/workflows/a01-atomic-sealed-publication.yml | src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java;src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java;src/main/java/co/surumene/simplecoins/domain/NormalSettlementSession.java;src/main/java/co/surumene/simplecoins/domain/SettlementSnapshotPublisher.java | src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java;.github/workflows/a01-atomic-sealed-publication.yml |

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

## Technical scope under existing Confirmed A01 and Drive 02B

The prior sealed-only publisher invalidates its own permit after any resume. Merely keeping the old seal forever prevents normal writes; merely ignoring its invalidation exposes stale accounting. Establish one product handoff route with a genuine saved stage, same uninterrupted sealed epoch, an actual fully verified NormalMutationJournal cut, and an externally accountable active normal tracking observer. No changes to product meaning.

- Worker preparation verifies original saved stage/manifest/content and all journal records under the exclusive drained lease, constructs the immutable complete baseline and obtains a journal-instance-bound sequence/revision cut. Does not publish or open writers. Main final handoff verifies the same seal, barrier generation, cut, no players and independent A01 evidence, then switches to a new opaque normal OPEN epoch, binds journal-backed snapshot state, and makes admission READY as one atomic boundary. HOLD stays unpublished; stale/foreign/reused token, stopped observer, changed journal phase or head, wrong barrier or writer fault fails closed. Do not consume an incomplete candidate or use a Boolean alone as proof. Old isolated sealed-only calls are not the runtime handoff.
- Full physical reconciliation absorbs prior journal history into the new baseline; never replay old historical deltas onto the already authoritative totals or automatically reissue an old PREPARED operation. Journal records remain durable, every restart redoes physical settlement. The normal session binds to that exact journal and its changing revision. A writer outside the session changing even an existing record phase invalidates the view.
- For an independently observed and verified single movement (PLAYER/FREE/DEAD/EXTERNAL, distinct accounts) prepare a durable operation before physical application, verify the exact physical result through the responsible synchronous adapter, mark EVENT_APPLIED and update balances and dead-location evidence exactly once. Preserve existing zero-balance registrations, reject underflow/unregistered or inconsistent endpoints, duplicate conflicts, and keep ambiguous PREPARED records for cold reconciliation. Repeated successful same-session operation is not executed twice; prior-session duplicates are explicitly historical, not success. Atomically expose the new immutable result only after durable application. A failing physical verification, journal, epoch or normal tracker revokes admission. Actual all-path normal event observer, registration/BAN transitions, same-owner dead relocation, asynchronous event finality, and live Paper binding are separate unmet responsibilities; this path cannot be enabled in bootstrap by simulated evidence.
- Real saved NBT / real durable journal JUnit, with explicit test-only physical/observer callbacks. Verify old history is not reapplied, worker-stage invisibility, successful controlled writer reopen and normal transfer, no duplicate physical operation, dead-location accounting, same-head journal phase drift rejection, journal/observer loss and failed physical callback. Preserve old saved-stage/atomic-seal regressions. No actual installed writer closure or runtime admission claim.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-handoff-model.json | @work/.cat-flow/cache/a01-handoff-vectors.json | @work/.cat-flow/cache/a01-handoff-queue.json | @repo:SimpleCoins/tests/cat/a01-writer-epoch-junit-binding.json | @work/.cat-flow/generated/A01HandoffGeneratedTest.java | @work/.cat-flow/cache/a01-handoff-obligations.json | — | false |
