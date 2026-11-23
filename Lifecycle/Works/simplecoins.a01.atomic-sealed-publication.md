---
cat_version: '0.2'
kind: work
id: simplecoins.a01.atomic-sealed-publication
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/635
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.writer-epoch-publication-implementation
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01/G0 — single drained writer seal must span final snapshot publication

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/635 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.atomic-sealed-publication | simplecoins.a01.writer-epoch-publication-implementation;simplecoins.a01.shared-writer-epoch-fence |

## Technologies
| Technology |
| --- |
| Java 25 JUnit5; existing domain WriterEpochFence, SettlementSnapshotPublisher, SettlementWriterEpochDecision and AdmissionBarrier; canonical CAT-TDD 896cae8313a533a3c74073a8c3150daad314ff90 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.atomic-sealed-publication.md;src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java;src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java;.github/workflows/a01-atomic-sealed-publication.yml | src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java | src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java;.github/workflows/a01-atomic-sealed-publication.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a01.protected-publisher | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.domain.SettlementProtectedPublisherTest | publishUnderDrainedSealWithGenuineAccumulator;holdDoesNotConsumeCandidate;rejectsUnsealedOrInFlightAndRevokes;lateFaultRevokesVisibleResult;resumeCannotRaceWithAtomicCommit | src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java | build/test-results/test/TEST-co.surumene.simplecoins.domain.SettlementProtectedPublisherTest.xml | 300 |

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

## Authoritative contract and bounded technical route

The already user-confirmed A01 Process/PI/TCE requires a complete same-generation monetary candidate, denial of new writer roots, full drain of admitted writers, no online players, attribution/BAN/journal reconciliation and a safe handoff to normal mutation journaling before any **atomic publication** may authorize admission. Existing `SettlementWriterEpochDecision` deliberately interprets independently-proven evidence and does NOT itself protect the interval or publish anything. Existing `WriterEpochFence.requireDrained` is a point-in-time check: a different thread may `resume` a seal after that check and start a new monetary writer before an unrelated `SettlementSnapshotPublisher.publish` or `AdmissionBarrier.complete`. We must not infer across-time atomicity from two sequential checks.

This independent feature implements a **bounded, unregistered domain composition unit** that accepts an existing `WriterEpochFence.Seal` plus a real incomplete `SettlementAccumulator` whose required sources must commit, and reuses the Confirmed `SettlementWriterEpochDecision`. A new `WriterEpochFence.withDrainedSeal` holds the actual internal writer-admission mutex across a *short synchronous callback*; it requires the same current seal, SEALED state and zero in-flight invocations and forbids an independent `resume` or `enterRoot` from interleaving before callback completion. The callback must not wait for or invoke any writer needing this fence, perform blocking disk/world scanning, or dispatch async work. It runs only the **final atomic domain publication** on a pre-staged candidate and `AdmissionBarrier.complete`. A callback exception invalidates its fence and revokes the snapshot and gate. `SettlementWriterEpochDecision` returning HOLD must leave the `SettlementAccumulator` unconsumed and the barrier SCANNING; REVOKE must close it. A foreign/stale seal, in-flight writer, missing source, wrong barrier, player presence or changed seal must fail closed, never mint a permit.

This step does **not** decide the environmental evidence: the caller-supplied A01 Booleans remain insufficient without actual producer proofs; every applicable **Paper, installed plugin, NMS, async and direct file writer** must be covered by a demonstrated fence. The ordinary plugin still starts with the Agent PASSIVE, `SettlementSnapshotPublisher(admission)` unbound journal monitor, no source coordinator and independent Admission Guard denial. This new service must NOT be connected to runtime on the strength of simulated true evidence. The current installed writer roster and G0/G3 remain unproven; the new product code may only become routable after real sources, owner/BAN and handoff certificates exist. No live JAR, `main`, VPS, G3 ACK or Admission toggle in this Work. Synchronized callback may not block main thread on worker drains or I/O; those belong before this narrow final phase.

## Required acceptance

- Genuine JUnit Red because the named product publication unit does not exist, with a source-bound expected behavior; final Green only after minimal implementation.
- Confirmed A01 Process Closure/Model Conformance verified in actual CAT runner. Other CAT stages separately tracked, not claimed passed from JUnit/CI alone.
- Real `SettlementAccumulator` complete, journal monitor `true`, matching seal, true evidence: immutable published `Result` and READY only after successful atomic callback. Before callback/current when SCANNING must stay hidden.
- Evidence missing: HOLD, no candidate consumption or READY. In-flight, foreign/stale seal, null source, incomplete source, online player or exception: failure/revoke; result never visible.
- Resume attempting concurrently *while* synchronous publication callback is executing cannot enter writer `OPEN` state until after the publication callback; do not interpret this as permission to resume after successful handoff without a separate normal mutation journal guarantee.
- Root full JUnit and Phase6 regression Green on same final PR head. `develop` merge only after CI success.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-seal-publication-model.json | @work/.cat-flow/cache/a01-seal-publication-vectors.json | @work/.cat-flow/cache/a01-seal-publication-queue.json | — | — | @work/.cat-flow/cache/a01-seal-publication-obligations.json | — | false |
