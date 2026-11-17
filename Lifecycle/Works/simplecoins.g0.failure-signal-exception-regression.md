---
cat_version: '0.2'
kind: work
id: simplecoins.g0.failure-signal-exception-regression
status: draft
process: simplecoins.settlement.startup-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/3
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/303
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

# G0 — 失敗通知自身の例外に対する決算ガードの独立回帰

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/303 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/SettlementStartupCoordinator.java |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.startup-publication | simplecoins.g0.failure-signal-exception-regression | simplecoins.g0.startup-publication-monitor-oracle;simplecoins.settlement.default-deny |

## Technologies

| Technology |
| --- |
| JDK25; JUnit 5; deterministic Java executor; actual SettlementStartupCoordinator and AdmissionBarrier; canonical CAT issue-work structure validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.g0.failure-signal-exception-regression.md;src/test/java/co/surumene/simplecoins/domain/SettlementStartupCoordinatorTest.java;.github/workflows/g0-failure-signal-exception-regression.yml | — | src/test/java/co/surumene/simplecoins/domain/SettlementStartupCoordinatorTest.java;.github/workflows/g0-failure-signal-exception-regression.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| g0.failure-signal.junit | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.domain.SettlementStartupCoordinatorTest | failureSignalThrowKeepsTheOriginalFaultAndAdmissionClosed;rejectedBackgroundExecutorWithThrowingFailureSignalNeverSchedulesWork | src/test/java/co/surumene/simplecoins/domain/SettlementStartupCoordinatorTest.java | build/test-results/test/TEST-co.surumene.simplecoins.domain.SettlementStartupCoordinatorTest.xml | 300 |

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

## Verification boundary

Approved 02B and Issue #3 require that a failed settlement must never expose an unverified value or permit player admission. The current experimental `SettlementStartupCoordinator` routes a stage or scheduling failure into `abort`, which records the original failure, calls `AdmissionBarrier.fatalFailure()`, and then invokes a separate failure signal; the signal itself may throw. The signal must not reopen admission or replace the diagnostic cause.

Legacy [PR #303](https://github.com/surumeneco/SimpleCoins/pull/303) retained **two unique JUnit cases** absent from the current `develop` test class. The present Work recovers only those two original test methods, byte-for-byte, into the current test class while preserving its eight established tests. It does not merge the historical PR head, replace the present coordinator, or alter its stage/publisher contracts.

1. A source-stage IOException followed by a throwing failure-signal callback retains the original IOException, appends the callback error as suppressed, calls the signal once, never publishes and permanently closes the guard.
2. A background executor rejecting the initial job, followed by a throwing failure signal, likewise retains the original RejectedExecutionException and its suppressed callback error, invokes the signal once, schedules no work and refuses a repeated startup.
3. The two cases are executed together with the prior eight JUnit cases under the present Java25 project, after exact-head CAT issue-work structural validation. Domain and Phase6 CI are separate regression checks.

This is **bounded characterization of existing code** in the independent `issue-work/verification` flow. It is not a formal CAT Confirmed specification, `spec-test-sync` or Script-owned Process/Model/Implementation Gate result. It neither completes G0 all-writer settlement nor proves Paper real-world lifecycle, published snapshot, crash recovery or safe admission release. No production source, config, admission guard, `main` or VPS change is permitted.

## Completion

Require exact-match original two historical methods and the unchanged eight baseline methods, green JUnit on the new feature HEAD, successful canonical Work structure check, successful Domain/Phase6 PR-head CI and remote readback. Integrate the isolated test/Work/workflow unit to `develop`; only then mark the old PR #303 superseded without merging its historical branch.
