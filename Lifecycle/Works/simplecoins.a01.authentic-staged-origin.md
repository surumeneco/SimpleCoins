---
cat_version: '0.2'
kind: work
id: simplecoins.a01.authentic-staged-origin
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/639
  - https://github.com/surumeneco/SimpleCoins/pull/640
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.saved-staged-publish-bridge
  - simplecoins.a01.paper-g1-staged-seal-publication
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01/G0 — reject forged accumulator grafted onto legitimate SAVED witness

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/639 |
| https://github.com/surumeneco/SimpleCoins/pull/640 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.authentic-staged-origin | simplecoins.a01.saved-staged-publish-bridge;simplecoins.a01.atomic-sealed-publication |

## Technologies
| Technology |
| --- |
| Java25 JUnit; genuine saved UUID.dat NBT, WorldStorageDiscovery and WorldSnapshotTraversal; trusted SettlementScanRunner stage producer; actual 56,000C BlockLocker/WCL/WWC/WWW real Paper CI |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.authentic-staged-origin.md;src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java;src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java;.github/workflows/a01-saved-staged-publish-bridge.yml | src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java | src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java;.github/workflows/a01-saved-staged-publish-bridge.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a01.reject-forged-saved-stage | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.domain.SettlementProtectedPublisherTest | rejectsForgedAccumulatorWithAuthenticSourceWitness;stagedRealSavedCoinPublishesOnlyFromOriginalContentWitness | src/test/java/co/surumene/simplecoins/domain/SettlementProtectedPublisherTest.java | build/test-results/test/TEST-co.surumene.simplecoins.domain.SettlementProtectedPublisherTest.xml | 300 |

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

## Source-bounded defect and correction

Confirmed A01/02B requires that published amounts originate from the actual complete SAVED/LIVE physical source generation and not a caller-asserted total. PR #639's internal `SettlementProtectedPublisher.evaluateAndPublishStaged` requires an authentic source Manifest and unchanged `WorldSnapshotTraversal.ContentWitness`. However `SettlementScanRunner.Staged` is currently a public four-component Java record: any caller can construct a different `SettlementAccumulator` and attach the genuine ContentWitness and required IDs from a previous traversal. Both physical-file hash checks still pass; the unrelated invented money is atomically published under the test's externally asserted Evidence. This is an incorrect composition of otherwise valid evidence. No new A01 monetary semantics are proposed.

Restore a **producer-authenticated stage handle**, not a new externally fabricable Boolean: `SettlementScanRunner.stage` and `stageWithBanCoverage` alone may return a marked `Staged` after their full verified traversal/owner staging. Existing public three/four-argument `Staged` constructors stay available for test/compatibility but produce **untrusted** values. A private Stage constructor/factory exclusively inside `SettlementScanRunner` mints an internal immutable origin bit; outside code cannot set that bit or copy it to an arbitrary accumulator. The source-attested `SettlementProtectedPublisher.evaluateAndPublishStaged` must reject an untrusted or detached Staged on TRY even with valid real witness/Manifest and every externally asserted Evidence flag true. Reject **before** source hashing or snapshot publication; invalidate the isolated fence and block admission. HOLD retains no consumption and existing behavior. The standalone `evaluateAndPublish` remains a separately bounded unregistered primitive for prior tests, not a runtime bypass permission.

**Red**: create actual compressed UUID.dat holding genuine 5,000C, run `WorldStorageDiscovery.discover` and real `SettlementScanRunner.stage` to obtain legitimate witness, then attach that witness/report/required-set to a fresh unrelated accumulator containing 90,000C. Under a sealed local fence all A01 booleans TRUE, the old implementation currently publishes the forged value; new test must fail for this concrete reason. **Green**: the forged candidate is rejected (IOException) and guard failed/fence faulted; the original actual-stage positive control still publishes only 5,000C under the test's limited gate. Run the existing actual Paper 55,000C/56,000C WCL/BlockLocker/BAN/source fixture CI, full JUnit, A01 process closure/model conformance and A01 writer-seal regression for the same PR HEAD.

The producer bit proves *only* this Java value was emitted by the verified source runner; it does not prove the all-installed-writer closure, source identity of external Ban/WCL probes, actual production journal handoff, completeness of other configured worlds, G0 runtime, G3, or player admission. Keep `SimpleCoinsPlugin` and passive Agent unchanged. No main/VPS/release.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-authentic-stage-model.json | @work/.cat-flow/cache/a01-authentic-stage-vectors.json | @work/.cat-flow/cache/a01-authentic-stage-queue.json | — | — | @work/.cat-flow/cache/a01-authentic-stage-obligations.json | — | false |
