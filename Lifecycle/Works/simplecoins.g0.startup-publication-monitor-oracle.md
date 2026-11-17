---
cat_version: '0.2'
kind: work
id: simplecoins.g0.startup-publication-monitor-oracle
status: draft
process: simplecoins.settlement.startup-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/277
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# G0 — 起動stagingと公開/ジャーナル監視の独立結合回帰

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/277 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/SettlementStartupCoordinator.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/SettlementSnapshotPublisher.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.startup-publication | simplecoins.g0.startup-publication-monitor-oracle | simplecoins.settlement.default-deny;simplecoins.settlement.world-epoch |

## Technologies

| Technology |
| --- |
| JDK25, JUnit5, in-process deterministic executor queues, actual SettlementAccumulator/StartupCoordinator/SnapshotPublisher/AdmissionBarrier, injected BooleanSupplier journal monitor, CAT issue-work structural validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.g0.startup-publication-monitor-oracle.md;src/test/java/co/surumene/simplecoins/domain/SettlementStartupPublicationBoundaryIntegrationTest.java;.github/workflows/g0-startup-publication-monitor-oracle.yml | — | src/test/java/co/surumene/simplecoins/domain/SettlementStartupPublicationBoundaryIntegrationTest.java;.github/workflows/g0-startup-publication-monitor-oracle.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| publication-boundary | regression | junit | @repo:SimpleCoins | gradle :test --tests co.surumene.simplecoins.domain.SettlementStartupPublicationBoundaryIntegrationTest | stage-visibility;late-player;publisher-exception;failed-stage;journal-default-deny;journal-loss | SettlementStartupCoordinator;SettlementSnapshotPublisher;AdmissionBarrier | JUnit XML | 180 |

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

## Bounded source-grounded contract

The user-approved 02B/04 require G0 all-source consistency, fail-closed publication and player admission only after complete authoritative settlement; normal journal integrity is also a prerequisite. The current `SettlementStartupCoordinator` stages in a separate executor and invokes an atomic publisher on the main executor. The current `SettlementSnapshotPublisher` has a **default constructor whose journal monitor is false**, and even after gate state READY, `current()` will decline publication and permanently fail the gate if no live journal evidence exists.

Old open [#277](https://github.com/surumeneco/SimpleCoins/pull/277) contains **four unique integration JUnits** for source staging vs publisher visibility, a player appearing during main-thread publication, a publisher exception, and background-source failure. These tests were written before the default-deny journal monitor change; the old positive test assumes `SettlementSnapshotPublisher(gate)` alone can publish even after a model-valid stage. That assumption is no longer correct. Preserve the original four test oracles while injecting a **test-only, explicitly true journal monitor** for successful publication cases. Add two separate negative control tests: the default publisher must never expose staged results after READY and must terminally fail, and loss of an initially active journal monitor must revoke visible publication permanently rather than restoring it if the signal becomes active again.

All observations use deterministic Java Queues and constructed `SettlementAccumulator`, **not** actual Paper worlds, live/saved synchronized snapshots, a real running journal, or a full G0 proof. The test-only true monitor is an injected *simulation* and cannot authorize production admission. Product source/config/admission files and `main` remain unmodified. Work status is Draft; no CAT Confirmation or formal process/model/implementation Gate results.

## Acceptance

Use a fresh branch from fixed develop, preserve old integration test behavior where valid and adapt only for the current default-deny journal contract. Dedicated JDK25 JUnit XML must confirm all six scenarios (4 historical + 2 new) pass, with Domain/Phase6 regressions green on exact HEAD. Readback and squash merge only those 3 Work/test/CI paths to develop. Close old #277 **without merging its old branch** after independent content migration. Failure or unknown journal must deny publication; never weaken `SettlementSnapshotPublisher` to accommodate an outdated test. Actual Paper save and all-writer G0 remains unverified.
