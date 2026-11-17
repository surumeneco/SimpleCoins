---
cat_version: '0.2'
kind: work
id: simplecoins.a12.station-consumption-spec
status: draft
process: simplecoins.currency.station-consumption-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/pull/489
  - https://github.com/surumeneco/SimpleCoins/pull/491
  - https://github.com/surumeneco/SimpleCoins/pull/492
  - https://github.com/surumeneco/SimpleCoins/pull/493
  - https://github.com/surumeneco/SimpleCoins/pull/494
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: specification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A12 — Merchant/Cartographyの通常PAPER貨幣消費拒否を形式仕様に写像する

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/489 |
| https://github.com/surumeneco/SimpleCoins/pull/316 |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.station-consumption-exclusion | simplecoins.a12.station-consumption-spec | simplecoins.currency.crafting-exclusion;simplecoins.a12.paper-station-consumption-boundary |

## Technologies

| Technology |
| --- |
| Paper 26.2 build 134; CartographyItemEvent; Merchant InventoryClickEvent; actual GitHub source; pinned CAT-TDD artifact linter and issue-work validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.station-consumption-spec.md;docs/cat/drafts/a12-station-consumption-process.md;docs/cat/drafts/a12-station-consumption-pi.md;docs/cat/drafts/a12-station-consumption-tce.md;.github/workflows/a12-station-consumption-draft.yml | — | .github/workflows/a12-station-consumption-draft.yml |

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

## Prior approved meaning and current source situation

The Drive 01 and 05 product requirements already prohibit consuming any authentic PDC/model PAPER coin in ordinary crafting or stations except the 20 value-equivalent SimpleCoins exchanges, while ordinary PAPER remains usable. This Work does **not** seek approval to change that gameplay rule.

Official Paper 26.2 evidence has now advanced beyond the initial **synthetic** CartographyItemEvent test in PR #489. PR #491 proves Minecraft's actual MerchantOffer accepts and decrements one authentic 5000C PDC/model PAPER **on a detached NMS copy**; PR #492 proves actual CartographyTableMenu generates FILLED_MAP from map slot0 plus authentic 5000C PAPER slot1 (the map input slot0 does not accept money as a map); PR #493 proves real result Slot.onTake decrements the coin copy; PR #494 proves direct NMS CartographyTableMenu.clicked also decrements it and transfers FILLED_MAP to cursor. In the *direct NMS invocation only*, InventoryClickEvent and CartographyItemEvent observers both received 0 callbacks, because the network packet listener was not invoked. **This does not prove real player packets bypass Bukkit events, nor that real player assets were consumed.**

Current production CoinExchangeRecipes listens to Prepare/Craft/Crafter, not merchant/cartography result operations. The sole unintegrated old PR #316 PaperCurrencyConsumption pure predicate and JUnit do not install a listener. Material protection at the network result operation, other-plugin direct NMS modifications and normal accounting all remain unimplemented/unverified.

## New Drafts and remaining normative decisions

The new source-bound Drafts are:
- `docs/cat/drafts/a12-station-consumption-process.md` — merchant/cartography physical result operation guard scope.
- `docs/cat/drafts/a12-station-consumption-pi.md` — source inputs and allow/cancel outputs.
- `docs/cat/drafts/a12-station-consumption-tce.md` — partial four-way decision partition for result/not-result and monetary marker/readability.

Remaining technical choices: the network packet→Paper InventoryClickEvent/CartographyItemEvent→NMS result onTake ordering; all acquisition actions including Shift/number-key/drag; cancellation efficacy before physical reduction; direct noncooperating NMS writers; callback priority/after-callback mutation; JE/BE and G0/G3 journal/saved generation. Result slot 2 is verified in the detached NMS cartography menu, but network-to-Paper mapping and real user consumption are not. Do not convert the **synthetic** event result from #489 into network-event proof; likewise, direct NMS reduction in #491–#494 is not a full native client transaction.

This Work preserves the already approved restriction but does not itself authorize a particular Paper adapter, promote CAT normative Confirmed, or bypass Process Closure/Model/TDD/implementation conformance. The next `spec-implementation` Work requires legitimate decision provenance, confirmed Process/PI/TCE and formal compiler-owned Gates.

## Acceptance and integration

1. All three source-bound CAT Drafts have correct Process→PI/TCE provenance and pass the actual pinned CAT artifact linter; the issue-work structure passes the canonical validator.
2. Distinguish accepted gameplay meaning, proposed event technology, real Paper synthetic evidence and unverified final coin-consumption paths.
3. The PR modifies only three Draft files, one Work and its validation workflow. No monetary item/source, listener, config, guard, protected branch, or test-oracle edits.
4. The independent Draft/spec documentation increment may merge into `develop` after exact-head CI, with all formal product CAT Gates still not-run. No implicit human normative confirmation is recorded.
