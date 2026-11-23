---
cat_version: '0.2'
kind: work
id: simplecoins.a01.saved-staged-publish-bridge
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/637
  - https://github.com/surumeneco/SimpleCoins/pull/638
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.atomic-sealed-publication
  - simplecoins.a02.atomic-persisted-ack-lease
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01/G0 — connect actual verified SAVED staged source generation to atomic publication

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/637 |
| https://github.com/surumeneco/SimpleCoins/pull/638 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.saved-staged-publish-bridge | simplecoins.a01.atomic-sealed-publication;simplecoins.a01.writer-epoch-publication-implementation |

## Technologies
| Technology |
| --- |
| Java25 JUnit; actual saved compressed UUID.dat + WorldStorageDiscovery + SettlementScanRunner + WorldSnapshotTraversal.ContentWitness; existing WriterEpochFence, SettlementProtectedPublisher, source authorizations |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.saved-staged-publish-bridge.md;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java;src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java;.github/workflows/a01-saved-staged-publish-bridge.yml | src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java | src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java;.github/workflows/a01-saved-staged-publish-bridge.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a01.staged-source-publish | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.domain.SettlementProtectedPublisherTest | stagedRealSavedCoinPublishesOnlyFromOriginalContentWitness;staleSavedFileRevokesBeforePublish;freshDiscoveryCannotReuseOldStage | src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java | build/test-results/test/TEST-co.surumene.simplecoins.domain.SettlementProtectedPublisherTest.xml | 300 |

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

## Confirmed source meaning and bounded next product wiring

Current Confirmed A01 Process/PI/TCE requires that real SAVED+LIVE monetary source candidates from the full configured world and playerdata universe be complete, drawn from an identical protected writer epoch, accurately owned/BAN/attributed, journal-reconciled and ready for normal journal handoff before G0 can atomically publish. PR #637's `SettlementProtectedPublisher` accepts a bare `SettlementAccumulator`, so a caller may classify abstract A01 Evidence Booleans as TRUE and directly publish without having bound the accumulator to the actual `WorldStorageDiscovery.Manifest` or the matching `SettlementScanRunner.Staged.savedContentWitness` from the disk traversal. Direct bare publication must remain a **separate bounded old internal API**, not a deployed permission.

This Work extends the still-unregistered product publisher with a **source-attested pathway**. It must receive `SettlementScanRunner.Staged`, the EXACT discovery instance `WorldStorageDiscovery.Manifest` and the independently supplied Confirmed A01 Evidence. On TRY, enforce non-null `savedContentWitness`; authentic `Manifest.origin` provenance; exact same object identity via `witness.requireOriginalManifest(manifest)`; all mandatory SAVED source IDs included in the `Staged.requiredSourceIds`; and `witness.requireUnchanged()` immediately before the final commit. These checks are performed while `WriterEpochFence.withDrainedLease` prevents reopening the same sealed generation (no monitor held across potentially slow SHA/NBT/directory recheck), and then invoke existing `evaluateAndPublish` which acquires its short, internally atomic `withDrainedSeal` commit before it consumes the staged real accumulator and completes admission barrier. A missing/stale source, foreign Manifest or source write must revoke the entire candidate/fence and never open READY. HOLD needs no SAVED IO and must leave staging unconsumed; REVOKE remains fail-closed.

Tests must use REAL, bounded, compressed UUID.dat coin NBT and `WorldStorageDiscovery.discover` → `SettlementScanRunner.stage`, not a hand-constructed `Staged`/witness or fake success flag; positive case verifies the authentic saved 5000C balance and zero players, while source overwrite and same-roots new Manifest are rejected. Include an empty authentic source set if useful but do not claim that one source or absence of online users establishes the full configured server's 24-JAR/World/WCL/BAN/Journal coverage. No metadata authorization or source registry coupling is asserted beyond the original Manifest+content witness. A bare caller-supplied `Evidence(... all true)` is still **not physical/environment proof**. This method is not invoked by `SimpleCoinsPlugin`, and source/accounting/loaded WCL and all writer closure remain independent blockers to real service activation.

Runtime is still PASSIVE Agent, default-deny Guard, no G0 publication, G3 distribution enablement, `main`, VPS, or release. Use the pinned CAT Process Closure and Model Conformance, genuine Red before implementation, full JUnit and relevant CI at exact PR head; record bounded result in #423. Avoid extra per-method writer probes merely to populate a report.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-saved-stage-model.json | @work/.cat-flow/cache/a01-saved-stage-vectors.json | @work/.cat-flow/cache/a01-saved-stage-queue.json | — | — | @work/.cat-flow/cache/a01-saved-stage-obligations.json | — | false |
