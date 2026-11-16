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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43740989379 (bounded Paper registry/guard counterexample, not physical craft or CAT Gate) |
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

## Measured Paper 26.2 counterexample

- Exact test HEAD `b11443d787594e6cbb5424eb8dbc79d6fbfce068` ran on official Paper 26.2 build 134 with authentic SimpleCoins `CoinItemCodec` money, product `CoinExchangeRecipes` event listener retrieved from actual `CraftItemEvent` registry, default-deny admission and zero players. [CI #43740989379](https://github.com/surumeneco/SimpleCoins/actions/runs/43740989379) had contract-valid Draft structure (`semantic_approval_verified:false`), Gradle, Paper and static CI success.
- **All 20** actual registered exchange output+input combinations were verified against the `CoinExchangeFormula` output PDC/quantity and accepted by the real guard; a wrong denomination in each matrix was rejected. Logs: `PAPER_A12_ALL_TWENTY_REAL_EXCHANGE_INPUT_OUTPUT_APPROVALS_PASSED`.
- The isolated test removed only `simplecoins:exchange_n1_f1`, installed a different `ShapelessRecipe` with **the same key** and **STONE output**, and confirmed via Paper `Bukkit.getRecipe` that the replacement was active. The original coin matrix nevertheless received `guard.isApproved(replacement,matrix)==true`. Exact Paper log: `PAPER_A12_KEY_ONLY_APPROVAL_VALUE_LOSS_COUNTEREXAMPLE key=simplecoins:exchange_n1_f1 result=STONE guardedInputCoins=PAPER`.
- The original registered recipe was restored in `finally` and rechecked on the isolated server (`PAPER_A12_DISPOSABLE_RECIPE_RESTORED_PASSED`). No real user inventory or ledger was used or modified.
- This is a **verified result-integrity trust-boundary violation in the guard's approval decision**, not a real human-client finished craft nor a proof that another enabled plugin currently attacks the registry. An external plugin with registry mutation privileges could violate physical exchange invariance; release readiness must keep this unresolved until an approved repair and integration tests exist.

## Required repair behavior (proposal, not authorization)

- An accepted coin exchange must independently prove the **actual current recipe and visible result ItemStack** match the approved `CoinExchangeFormula` output denomination, amount and stable PDC/model, in addition to validating each consumed input slot. A recipe key by itself cannot certify output value.
- Check Prepare/Craft/Crafter and any altered output event path, including conflicts from later-priority/other plugin listeners; fail closed on mismatch and avoid creating/consuming physical value without a journaled mutation.
- Define precisely when the result and inventory are authoritative, and what event/post-event verification means under Paper's mutable recipe registry. If a guaranteed atomic observation cannot be established, preserve admission denial rather than claiming G0/A12.
- Create or reuse genuine Process/PI/TCE and formal CAT Gates before editing production; retain this verification-only Work as source evidence, not self-Confirmed approval.

## Interpretation boundary

This Work is verification-only and Draft. Ordinary CI and registry acceptance are not equivalent to crafting completion, recovery or normative CAT approval.
