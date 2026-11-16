---
cat_version: '0.2'
kind: work
id: simplecoins.a14.trader-default-operator-override
status: draft
process: simplecoins.trader-drop.defaults
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/340
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

# A14 — preserve explicit operator rate across shipped-default changes

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |
| https://github.com/surumeneco/SimpleCoins/pull/340 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.trader-drop.defaults | simplecoins.a14.trader-default-operator-override | simplecoins.trader.drop-config-validation |

## Technologies

| Technology |
| --- |
| Java 25; Paper API YamlConfiguration; seeded policy Random; JUnit 5; Gradle 9.2.1 |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a14.trader-default-operator-override.md;Lifecycle/CAT/Candidates/simplecoins.trader-default-drop-rate-10percent.md;src/test/java/co/surumene/simplecoins/paper/WanderingTraderDropConfigReaderTest.java;.github/workflows/a14-operator-trader-default-regression.yml | — | src/test/java/co/surumene/simplecoins/paper/WanderingTraderDropConfigReaderTest.java;.github/workflows/a14-operator-trader-default-regression.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

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

## Independent issue-work verification

The adopted Drive 03 source fixes the **shipped default** trader death-event drop chance to `0.1` (10%) with all nine denominations and the 256..1 conditional weighting. It separately promises **never to overwrite an operator's explicitly configured existing chance**. The old stacked PR #340 contained a concrete test for that override, but that JUnit method was absent from the newer develop branch while the 10% default and other tests had already been integrated.

Reinstate that original JUnit test without changing current Java production sources, shipped configuration, operator files or monetary issuance. The test provides an operator-configured legacy-compatible `chance=0.5` while the valid default is now `0.1`, uses the real `WanderingTraderDropConfigReader` and seeded 10,000 `WanderingTraderDropPolicy.draw` samples, and verifies the explicit 50% remains effective. Preserve the already-shipped default 10% regression and legacy alias rejection. Archive the exact old CAT **Candidate** bytes from #340 in the original candidate path; do not self-promote to Confirmed.

This verifies the isolated config reader behavior, not Paper death listeners, online drop issuance, live migration of existing YAML files, full G0/G3 or a formal CAT Gate. No product JAR or admission-guard write is authorized. If CI fails, diagnose the exact test/Work contract and repair the bounded independent unit before merging.

## Acceptance

- Original method `explicitlyConfiguredLegacyFiftyPercentIsNotSilentlyOverridden` restored without changing test semantics or overwriting the existing 10% default test.
- Correct original Candidate bytes and semantics recovered, status remains `candidate`; YAML in develop still uses 0.1.
- Exact-head root JUnit plus Domain/Phase6 CI successful, feature→develop squash merge and readback.
