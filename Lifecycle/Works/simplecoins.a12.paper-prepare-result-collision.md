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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43820540144 (actual Paper Prepare event, not CAT formal Gate) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Measured official Paper result

- At exact tested head `85365b03f9a889bbbde945ac7b3180995b3e70df`, isolated official Paper 26.2 build 134 plus independent default-deny guard and current SimpleCoins JAR ran the registered production `CoinExchangeRecipes` listener. Real Paper CI [#43820540144](https://github.com/surumeneco/SimpleCoins/actions/runs/43820540144), Domain foundation and static Phase6 CI all succeeded on that head. New Work CAT structure: `contract_valid:true`, `semantic_approval_verified:false`.
- After the legitimate `simplecoins:exchange_n1_f1` registry entry had been temporarily replaced with a ShapelessRecipe whose output is `STONE` and whose ingredient is ordinary `PAPER`, real genuine coin input still yielded `isApproved=true`.
- A test-only **detached, offline NMS Player** was supplied to the real `Bukkit.craftItemResult(moneyMatrix, world, player)` path. A separate `MONITOR` listener observed exactly **one** registered `PrepareItemCraftEvent` for the target key after the product `HIGHEST` listener. The observed prepared result was `STONE`, and the official Paper API returned `STONE` as the computed craft result. Exact log: `PAPER_A12_ACTUAL_PREPARE_RESULT_OBSERVED prepareEventCount=1 postMonitor=STONE result=STONE disconnectedSyntheticPlayer=true`.
- The original recipe was restored in `finally`, and the observer unregistered. No player entered the server, no inventory grant or actual end-to-end finished CraftItemEvent occurred, and no economic loss was produced or asserted.
- This **upgrades the previously observed key-only approval gap to a real Prepare-event-path result-integrity failure**, bounded to Paper's programmatic crafting API. It is a release-blocking technical observation under the accepted value-preservation requirement, not authorization to self-Confirm the unapproved A12 Process/PI/TCE or change production before legitimate CAT gates.

## Verification boundary

- Keep the already-verified official Paper experiment that registers a different `ShapelessRecipe` under an existing `simplecoins:exchange_*` key with actual `STONE` output, while real authentic SimpleCoins marked money is supplied as the input matrix and `CoinExchangeRecipes.isApproved` reports true.
- Instead of assuming this API decision is equivalent to an event, use **Paper's actual `Bukkit.craftItemResult(matrix, world, player)` path** with a synthetic **detached** NMS Player, which is documented to generate `PrepareItemCraftEvent`. Verify the product's already registered listener sees the event, observe resulting ItemStack after Prepare event, and distinguish a failure-to-reject observed STONE from a successful cancellation/null result.
- During the isolated test, install an independent `MONITOR` listener that counts only Prepare events with the substituted exact recipe key, and records the observed final prepared output after the product `HIGHEST` listener. Restore the authentic original recipe in `finally` and unregister the observer; never create/receive a real physical item in a player's inventory.
- Do not create an online network player, call real multiplayer login, perform a finished CraftItemEvent or Crafter redstone operation, alter production source, grant admission, or claim economic loss. The expected observation is bounded Paper event handling with a detached player. If this use of the Bukkit API fails or Paper refuses it, report that as unverified rather than changing the test to fake a result.
- Separate accepted game rule (20 equal-value exchanges) from proposed security invariant and all unapproved CAT Drafts. No new Confirmed semantics or CAT formal Gates follow from this experiment.
