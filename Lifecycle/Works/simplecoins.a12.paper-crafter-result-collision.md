---
cat_version: '0.2'
kind: work
id: simplecoins.a12.paper-crafter-result-collision
status: draft
process: simplecoins.currency.crafting-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/pull/444
  - https://github.com/surumeneco/SimpleCoins/pull/456
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

# A12 — CrafterCraftEventの結果出力検査に関する実Paper境界

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/444 |
| https://github.com/surumeneco/SimpleCoins/pull/456 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.crafting-exclusion | simplecoins.a12.paper-crafter-result-collision | simplecoins.a12.paper-prepare-result-collision |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; real Crafter block and CrafterCraftEvent; live exchange-key registry; genuine currency |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.paper-crafter-result-collision.md;tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml | — | tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

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

## Bounded test contract

- Reuse the latest successful official Paper 26.2 + default-deny integration test for all 20 equal-value money exchanges and the same-key STONE output substitution. The product's actual `CrafterCraftEvent` guard must be registered by the product plugin.
- During the substitution only, create a real disposable `Material.CRAFTER` block at an unoccupied location in the isolated world, place precisely one authentic marked SimpleCoins coin into the crafter's actual inventory, and call `Bukkit.getPluginManager().callEvent` with a real `CrafterCraftEvent` carrying that registered substituted recipe and its STONE result.
- Observe `event.isCancelled()` and `event.getResult()` **after all event handlers**; emit distinct fail-closed or unauthorized-acceptance markers and require only the observed decision and the normal rollback of this temporary test block. Preserve original recipe in `finally`.
- This dispatches an actual Bukkit plugin event but **does not** trigger a redstone pulse, automatic inventory consumption, ItemStack spawning or final world economy transition. Do not claim a working exploit, completed Crafter transaction, G0/G3, or an actual networked player action from the event result.
- The test may not change production classes, permissions, admission, CAT Confirmed or formal Gates; Work stays independent issue-work Draft.
