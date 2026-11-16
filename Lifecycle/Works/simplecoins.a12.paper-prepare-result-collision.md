---
cat_version: '0.2'
kind: work
id: simplecoins.a12.paper-prepare-result-collision
status: draft
process: simplecoins.currency.crafting-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/pull/444
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

# A12 — 実Paperクラフト準備イベントにおける正規ID・改変出力の拒否能力

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/444 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/drafts/a12-exchange-output-tce.md |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.crafting-exclusion | simplecoins.a12.paper-prepare-result-collision | simplecoins.a12.exchange-recipe-result-integrity |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; Paper Bukkit craftItemResult/PrepareItemCraftEvent; test-only NMS Player; authentic coin ItemStack |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.paper-prepare-result-collision.md;tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml | — | tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | ---: |

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

- Keep the already-verified official Paper experiment that registers a different `ShapelessRecipe` under an existing `simplecoins:exchange_*` key with actual `STONE` output, while real authentic SimpleCoins marked money is supplied as the input matrix and `CoinExchangeRecipes.isApproved` reports true.
- Instead of assuming this API decision is equivalent to an event, use **Paper's actual `Bukkit.craftItemResult(matrix, world, player)` path** with a synthetic **detached** NMS Player, which is documented to generate `PrepareItemCraftEvent`. Verify the product's already registered listener sees the event, observe resulting ItemStack after Prepare event, and distinguish a failure-to-reject observed STONE from a successful cancellation/null result.
- During the isolated test, install an independent `MONITOR` listener that counts only Prepare events with the substituted exact recipe key, and records the observed final prepared output after the product `HIGHEST` listener. Restore the authentic original recipe in `finally` and unregister the observer; never create/receive a real physical item in a player's inventory.
- Do not create an online network player, call real multiplayer login, perform a finished CraftItemEvent or Crafter redstone operation, alter production source, grant admission, or claim economic loss. The expected observation is bounded Paper event handling with a detached player. If this use of the Bukkit API fails or Paper refuses it, report that as unverified rather than changing the test to fake a result.
- Separate accepted game rule (20 equal-value exchanges) from proposed security invariant and all unapproved CAT Drafts. No new Confirmed semantics or CAT formal Gates follow from this experiment.
