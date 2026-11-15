---
cat_version: '0.2'
kind: work
id: simplecoins.a12.exchange-recipe-result-integrity
status: draft
process: simplecoins.currency.crafting-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# A12 — 正規両替レシピIDと出力・置換の境界検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/CoinExchangeFormula.java |
| https://github.com/surumeneco/SimpleCoins/pull/436 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.crafting-exclusion | simplecoins.a12.exchange-recipe-result-integrity | simplecoins.a12.vanilla-paper-choice-boundary;simplecoins.v0.coin-recipe-registration |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; live recipe registry; authentic SimpleCoins ItemStack/PDC; independently registered listener |

## Repositories

`Allowed paths` uses `;` as separator.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.exchange-recipe-result-integrity.md;tests/paper-vanilla-choice-probe/build.gradle.kts;tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml | — | tests/paper-vanilla-choice-probe/build.gradle.kts;tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml |

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

## Bounded verification objective

- The approved 01/02B rule is that 20 exchange recipes preserve physical currency value, and tagged currency is not legitimate vanilla crafting input. The existing guard in `CoinExchangeRecipes` authorizes exchange using a stable recipe key and coin PDC/model input values.
- Boot official isolated Paper 26.2 with product JAR, admission guard and independently installed test plugin. Discover the actual already-registered `CoinExchangeRecipes` listener from Paper's `CraftItemEvent` handler registry, rather than constructing an unrelated listener.
- For **all 20** registered product recipes, construct a legal input matrix with authentic ItemStacks and require `isApproved` to accept it. Also reject wrong denominations, null/non-keyed and ordinary vanilla recipes using the same real instance.
- Choose one real registered recipe. Within the disposable test server, remove that single Bukkit recipe and register **another ShapelessRecipe under the same key but a nonmonetary `STONE` output** with ordinary `PAPER` ingredient choices; inspect the live Paper registry and existing guard's acceptance of the now-adversarial result. Keep the original instance and restore its exact registration in `finally`.
- If the registry refuses replacement, record that actual constraint. If it accepts replacement and the current guard still says `isApproved=true` for the original coin matrix, record a concrete **guard trust-boundary counterexample**, not a completed player craft or final security exploit.
- No product source change, no actual monetary balance, no player connection. Do not publish settlement, open guard, or infer real JE/BE craft/trading safety.
- A confirmed weakness must be narrowed to the required additional output/registry integrity invariant for legitimate normative review; it is not permission to alter production without Confirmed Process/PI/TCE and formal CAT Gates.

## Interpretation boundary

This Work is verification-only and Draft. Ordinary CI and registry acceptance are not equivalent to crafting completion, recovery or normative CAT approval.
