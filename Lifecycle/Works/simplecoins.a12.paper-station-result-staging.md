---
cat_version: '0.2'
kind: work
id: simplecoins.a12.paper-station-result-staging
status: draft
process: simplecoins.currency.station-consumption-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A12 — 正規貨幣のMerchant/Cartography結果操作拒否・Paper試験

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a12-station-consumption-process.md |
| https://github.com/surumeneco/SimpleCoins/pull/520 |
| https://github.com/surumeneco/SimpleCoins/pull/522 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.station-consumption-exclusion | simplecoins.a12.paper-station-result-staging | simplecoins.currency.crafting-exclusion;simplecoins.settlement.default-deny |

## Technologies

| Technology |
| --- |
| Java |
| JUnit |
| Paper |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.paper-station-result-staging.md;src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/paper/StationResultGuardLiveProbe.java;tests/paper-station-product-smoke/*;tests/paper-station-guard-probe/src/main/java/co/surumene/simplecoins/a12probe/StationGuardProbe.java;tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;build.gradle.kts;.github/workflows/paper-station-product-guard.yml;.github/workflows/paper-station-guard-probe.yml | src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/paper/StationResultGuardLiveProbe.java;tests/paper-station-product-smoke/*;tests/paper-station-guard-probe/src/main/java/co/surumene/simplecoins/a12probe/StationGuardProbe.java;tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;build.gradle.kts;.github/workflows/paper-station-product-guard.yml;.github/workflows/paper-station-guard-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | blocked | Closed World is explicitly false in Confirmed A12 Process; compiler model exit code 2 |
| model-conformance | not-run | Formal Gate cannot advance past process-closure |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | passed | [Exact-head #44207755155](https://github.com/surumeneco/SimpleCoins/actions/runs/44207755155) native Merchant/Cartography result+Shift, [#44207754896](https://github.com/surumeneco/SimpleCoins/actions/runs/44207754896) product event 16 cases, [#44207754835](https://github.com/surumeneco/SimpleCoins/actions/runs/44207754835) vanilla craft; all 13 workflows succeeded at `8dc87ac05a4943436bdc305c1adcc811277c2d5a` |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Verification boundary

This is a bounded **issue-work verification staging**, not a claim that the
Confirmed Process has passed formal spec-implementation Gates. Paper 26.2 build
134 dispatches synthetic InventoryClickEvent through its real PluginManager
with a three-slot proxy view of Merchant/Cartography result slot 2. The fixture
checks ordinary PAPER remains permitted, correctly marked and malformed-PDC
coins are denied, and moving input/lower items remains permitted. No NMS
physical result consumption, socket, actual client, third-party update or
settlement publication is claimed by this particular fixture. Prior #518–#522 supply **pre-change** packet-handler contrasts. This PR's separate actual Paper handler fixture observes product preservation for Merchant/Cartography results under ordinary and Shift/QUICK_MOVE clicks, without a real socket or player login.

Product result-slot listener has been staged after a genuine expected Red
on the former product (Paper event bus run 44205074411). This is **only** a
partial event-surface mitigation, not complete CAT acceptance. The Confirmed
Process remains explicitly open-world; script-owned Process Closure remains
blocked rather than being rewritten to passed.
Direct-NMS writers, later plugin overrides, authenticated network clients, and unverified other click types (number-key, double-click, drag) remain outside this local listener proof.
