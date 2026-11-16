---
cat_version: '0.2'
kind: work
id: simplecoins.a12.actual-exchange-result-guard
status: draft
process: simplecoins.currency.crafting-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/pull/459
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A12 — 現行登録レシピと実イベント結果の正規価値検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/444 |
| https://github.com/surumeneco/SimpleCoins/pull/459 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/drafts/a12-exchange-output-process.md |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.crafting-exclusion | simplecoins.a12.actual-exchange-result-guard | simplecoins.a12.paper-late-prepare-output-mutation;simplecoins.v0.coin-recipe-registration |

## Technologies

| Technology |
| --- |
| Paper 26.2 build 134; Java 25; genuine PDC and item_model; Prepare/Craft/Crafter event source; real recipe registry |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.actual-exchange-result-guard.md;src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java;tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml | src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java | tests/paper-vanilla-choice-probe/src/main/java/co/surumene/simplecoins/a12probe/VanillaChoiceProbe.java;.github/workflows/paper-vanilla-choice-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| paper-a12 | regression | junit | @repo:SimpleCoins | gradle test | real Paper substitution; genuine exchanges; late listener | actual Paper+default deny | CI log | 480 |

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
| ci | passed | [actual Paper #43839148555](https://github.com/surumeneco/SimpleCoins/actions/runs/43839148555); [Domain #43839148787](https://github.com/surumeneco/SimpleCoins/actions/runs/43839148787); [Phase6 #43839148526](https://github.com/surumeneco/SimpleCoins/actions/runs/43839148526); [recipe #43839148775](https://github.com/surumeneco/SimpleCoins/actions/runs/43839148775) (tested HEAD `d6508dfa`, ordinary CI only) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Bounded behavior

The accepted Drive 01 rule predates this Work: only 20 shapeless equivalent coin exchanges can consume marked currency; currency output has exact PDC, item_model, count and face value. In the existing verified Paper #444/#456/#457 tests another recipe under the same key returns STONE, yet the old guard allows it. The limited correction must check actual registered recipe output and event outcome against the accepted CoinExchangeFormula, not key-only. It must leave normal unmarked PAPER crafting and all original 20 legitimate exchanges intact. Test-first observation requires the substituted-output case to fail, then pass after repair.

This Work is a **bounded issue-work implementation record**, not a Confirmed CAT Process or proof of formal CAT Gates. Existing A12 Process/PI/TCE files are Draft and their authorization/Gate chain is not fabricated. A separate #459 late HIGHEST listener may still replace an otherwise genuine prepared output after the guard. This is not solved by one earlier event check; full final-consumption atomicity, Shift, true redstone and ledger settlement remain unfinished. The guard must never be represented as making G0, release or admission safe.

## Verified bounded Red–Green observations

- **Red**, exact test-only HEAD `79c6ff7f27bca5a9d4dee6ed64a08c806f4cb697`: on official Paper 26.2, [run #43838565855](https://github.com/surumeneco/SimpleCoins/actions/runs/43838565855) fails after registering a genuinely different ShapelessRecipe under `simplecoins:exchange_n1_f1`. Extracted uploaded Paper log: `PAPER_A12_PROBE_FAILED`, `java.lang.IllegalStateException: A12_RED: same-key STONE output was approved key=simplecoins:exchange_n1_f1`. The existing 20 authentic recipes succeeded before substitution. The original recipe was restored in `finally`.
- **Green**, exact source+CI HEAD `d6508dfa06989f587e0f38aa7a8f1dfae0840e40`: real Paper run #43839148555 and same-head Domain, Phase6 and coin registration run all passed. The actual substituted recipe is refused: `PAPER_A12_SUBSTITUTED_OUTPUT_REJECTED_BY_GUARD`. With the replacement installed, the actual Paper Prepare event returned `observed=AIR result=AIR`, and a real CrafterCraftEvent was `cancelled=true result=STONE`; neither test consumed any physical player money. Authentic 20 recipe input/output agreements and original recipe restoration also passed.
- **Still vulnerable outside this bounded repair:** a second plugin registered after the currency guard at `HIGHEST` can still change a genuine prepared output to STONE. The existing #459 late-listener test remains green as a *counterexample* (`originallyMoney=true observed=STONE apiResult=STONE`). This Work does **not** establish actual manual CraftItemEvent consumption safety, Shift repeat, powered Crafter consumption, atomic event order, monetary journaling, G0 or admission publication.
- CAT normative Process/PI/TCE remain Draft; CI Work structural validation, Red–Green and develop integration are not official Process Closure, Model Conformance or Implementation Conformance and must not be reclassified as those Gates.
