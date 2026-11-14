---
cat_version: '0.2'
kind: work
id: simplecoins.v2.geyser-bootstrap-order
status: draft
process: simplecoins.presentation.geyser-bootstrap-order
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: Geyser provider startup ordering for staged custom item assets

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |
| https://github.com/surumeneco/SimpleCoins/pull/407 |
| https://github.com/surumeneco/SimpleCoins/pull/408 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.presentation.geyser-bootstrap-order | simplecoins.v2.geyser-bootstrap-order | simplecoins.settlement.default-deny;simplecoins.currency.pdc-stable |

## Technologies

| Technology |
| --- |
| Paper 26.2 Bukkit legacy plugin.yml dependency ordering; JUnit 5; official Geyser-Spigot |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/resources/plugin.yml;src/test/java/co/surumene/simplecoins/PaperBootstrapDescriptorCandidateTest.java;.github/workflows/paper-geyser-real-loader-smoke.yml | src/main/resources/plugin.yml | src/test/java/co/surumene/simplecoins/PaperBootstrapDescriptorCandidateTest.java;.github/workflows/paper-geyser-real-loader-smoke.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| descriptor | verification | JUnit | . | gradle test --tests co.surumene.simplecoins.PaperBootstrapDescriptorCandidateTest | geyser-order-optional-provider | plugin.yml;Paper startup code | JUnit report | 180 |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | Confirmed Process/PI/TCE not supplied |
| model-conformance | not-run | No Confirmed CAT specification |
| implementation-conformance | not-run | No normative specification to compile |
| ci | not-run | New candidate branch not yet checked |
| merge | not-run | Unreviewed candidate |
| real-use | not-run | Client compatibility not tested by dependency metadata |

## Source-grounded defect and expected test

- The current SimpleCoins bootstrap calls `getPlugin("Geyser-Spigot")` in `onEnable` and stages resources only if Geyser is already enabled; otherwise it logs 'not present' and does not stage, leaving the currency pack absent for that boot.
- Its `plugin.yml` currently declares `softdepend` for Nexo, BlockLocker, WWC, WWW and WCL, **not** Geyser-Spigot, so the ordering is incidental. PR #408 demonstrates successful real provider ordering in one environment, not a guaranteed loader edge in others.
- Preserved expected behavior: with Geyser installed, its bootstrap should precede SimpleCoins; without Geyser, SimpleCoins should still be able to start in guarded JE-only mode. A `softdepend: Geyser-Spigot` descriptor edge provides the conditional ordering without a hard dependency.
- TDD test-first: in the descriptor test, require the optional provider after `softdepend:`, and preserve the `depend: SimpleCoinsGuard` hard dependency. The expected Red is the missing optional edge. Green changes only the descriptor.
- Official Paper real-Geyser smoke checks actual enabling order (not merely the ordering listed in YAML) and full Java regressions remain the merge gate. This Work is source-bound issue-work Draft, not authority to mark CAT process/model gates as passed. No new gameplay or JE/BE public release is authorized.
