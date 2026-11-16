---
cat_version: '0.2'
kind: work
id: simplecoins.a12.paper-late-prepare-output-mutation
status: draft
process: simplecoins.currency.crafting-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
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

# A12 — 正規両替のPrepare後段に別Pluginが出力を差し替えた時の実Paper観測

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/456 |
| https://github.com/surumeneco/SimpleCoins/pull/457 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/drafts/a12-exchange-output-process.md |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.crafting-exclusion | simplecoins.a12.paper-late-prepare-output-mutation | simplecoins.a12.paper-prepare-result-collision |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; authentic registered currency recipes, detached NMS Player, PrepareItemCraftEvent at HIGHEST and MONITOR |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.paper-late-prepare-output-mutation.md;tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml | — | tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml |

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

## Question and bounded verification

- The accepted Drive 01 requirement is that a marked physical coin is never consumed as ordinary PAPER and that all 20 authorized exchanges have equal input/output value. Existing Paper Prepare/Crafter test cases show that the current guard accepts same-key STONE output substitution. They do **not** establish whether an additional enabled plugin can mutate the result **after** the product's normal HIGHEST guard while a legitimate exchange is still registered.
- Keep all original registered recipes intact. On a disconnected synthetic NMS player in isolated official Paper, supply legitimate authentic coin inputs to an existing verified exchange. Register a **separate test-only HIGHEST PrepareItemCraftEvent listener after the product listener**, scope to that exact currency recipe, and replace only the prepared result with STONE. Register a MONITOR observer for the same event. Call the real Bukkit `craftItemResult` API once, capture event count, MONITOR result and API result.
- Require that the late test listener actually ran exactly once, that the observation ran, that the still-registered recipe output remained correct, and that no online player/inventory mutation occurred. Classify either STONE surviving or a safe rejection by the final observed result; **do not invent a mandatory STONE result**. Remove both test listeners in `finally` and keep all Product source and inventory unchanged.
- This is a verification-only `issue-work` Draft with no normative authority. It does not perform real user crafting, Shift crafting, physical money consumption, journal settlement or G0/G3 and cannot show CAT formal Gate success. A later mutation may require a fundamentally different atomic decision/commit mechanism; do not silently choose a design.
