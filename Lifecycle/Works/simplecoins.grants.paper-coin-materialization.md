---
cat_version: '0.2'
kind: work
id: simplecoins.grants.paper-coin-materialization
status: draft
process: simplecoins.grants.paper-inventory-delivery
source_refs:
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

# Work: simplecoins.grants.paper-coin-materialization

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinItemCodec.java |
| https://github.com/surumeneco/SimpleCoins/pull/419 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-inventory-delivery | simplecoins.grants.paper-coin-materialization | simplecoins.grants.paper-inventory-slot-plan;simplecoins.grants.saved-receipt-universe-audit |

## Technologies

| Technology |
| --- |
| Paper 26.2; Java 25; Gradle 9.2; JUnit 5 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.paper-coin-materialization.md;src/main/java/co/surumene/simplecoins/paper/PaperGrantInventoryMaterializer.java;src/test/java/co/surumene/simplecoins/paper/CoinRecipesLiveProbe.java;.github/workflows/paper-coin-recipes-live-smoke.yml | src/main/java/co/surumene/simplecoins/paper/PaperGrantInventoryMaterializer.java | src/test/java/co/surumene/simplecoins/paper/CoinRecipesLiveProbe.java;.github/workflows/paper-coin-recipes-live-smoke.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| paper-grant-coin-staging | regression | junit | @repo:SimpleCoins | gradle test jar paperCoinRecipesProbeJar | real-paper-pdc;existing-storage;forged-before;preserve-coins;no-mutation | CoinItemCodec;GrantLedger;PaperGrantInventorySlotPlan | JUnit; Paper probe log | 180 |

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

## Accepted behavior and constraints

- Confirmed product intent: offline give uses canonical monetary denominations, split at 64, does not overwrite inventory, and retains the remaining reservation if insufficient space. Currency identity is a PAPER ItemStack carrying immutable `CoinItemCodec` PDC denomination and `item_model`.
- This Work takes the already authenticated, drift-rechecked Paper storage placement plan and generates a **defensive copy** of the proposed 36-slot ItemStack array using the existing canonical `CoinItemCodec`. Every newly generated coin stack must match planned denomination/count/material/PDC. No original player ItemStack, Inventory or PDC may change.
- A real official Paper 26.2 test-only probe must exercise 5200C in a 1-slot allocation then 100C after a second slot appears, count 64 splitting, invalid/stale storage rejection, and prove originals remain unchanged. The CI workflow must explicitly grep its own marker; JUnit alone without Bukkit ItemFactory is insufficient for physical ItemStack construction.
- Still NOT the actual player inventory+receipt `saveData()` commit, readback, crash recovery/settlement journal or admission. This is a staging component only and remains unregistered in the shipping plugin; do not report exactly-once physical delivery or release.
- CAT: issue-work Draft syntax only, unverified normative Confirmed Process/PI/TCE, official Gates not-run; successful CI and Paper probe are separately recorded.
