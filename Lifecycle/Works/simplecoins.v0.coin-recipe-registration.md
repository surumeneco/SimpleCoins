---
cat_version: '0.2'
kind: work
id: simplecoins.v0.coin-recipe-registration
status: draft
process: simplecoins.currency.exchange-registration
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
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

# Work: simplecoins.v0.coin-recipe-registration

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view |
| https://github.com/surumeneco/SimpleCoins/blob/921efd863426ca9586c340520a992914fc664981/src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java |
| https://github.com/surumeneco/SimpleCoins/blob/921efd863426ca9586c340520a992914fc664981/src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.exchange-registration | simplecoins.v0.coin-recipe-registration | simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/paper/*;.github/workflows/*;build.gradle.kts | src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/paper/*;.github/workflows/* |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

External/semantic gates are reported evidence. `passed` or `not-applicable` requires a concrete evidence/rationale reference.

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

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Work scope and checked source state

- Source requirement: approved gameplay design `01_貨幣・財布・レジ.md` fixes 20 equal-value shapeless conversions and identity via denomination PDC/item_model, `PAPER` (not ordinary paper), 1.0.0 scope.
- Checked `develop@921efd863426ca9586c340520a992914fc664981`: `CoinExchangeRecipes.register()/unregister()` and `CoinItemCodec` exist, but `SimpleCoinsPlugin.onEnable()/onDisable()` do not construct/register recipes; guarded settlement/admission is still blocked.
- Goal: enable/disable precisely the 20 recipe registry entries with existing `CoinExchangeRecipes`, retaining default-deny login and fail-stop journal behavior. Registration errors must not leave partially installed recipes.
- Test-first evidence: isolated Paper 26.2 build 134 can prove 20 recipe IDs and genuine coin results are available, without admitting a player or claiming completed journal/settlement; enforce no probe class in production JAR.
- This is a Draft Work, not a legitimate Confirmed Process/PI/TCE. All formal product CAT Gates remain not-run; no fabricated approval is asserted.

## Acceptance boundaries

1. Existing official Paper startup remains default-deny and journal open/close/fail-stop guard remains unchanged.
2. Exactly 20 stable namespaced recipe IDs are registered after `onEnable`, and removed in safe `onDisable`; all recipe results are marked coin items with expected monetary values, not ordinary PAPER.
3. An isolated Paper smoke probe and domain JUnit succeed at the tested HEAD. No assertion of player Shift crafting, automated crafter runtime interactions, Geyser/Bedrock, external-plugins or complete settlement.
4. Feature can merge to develop only as a bounded independent integration step; `main`/release remains untouched.
