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

The verified Paper PR #489 shows a synthetic CartographyItemEvent for input 0/1 + result 2 is delivered to Paper and remains uncancelled by current SimpleCoins. It also proves only that a merchant PAPER recipe can be registered, not that a PDC coin is eligible nor that a trade occurred. The current production CoinExchangeRecipes listens to Prepare/Craft/Crafter, not merchant/cartography transactions. The sole unintegrated PR #316 PaperCurrencyConsumption pure predicate and JUnit do not install a listener.

## New Drafts and remaining normative decisions

The new source-bound Drafts are:
- `docs/cat/drafts/a12-station-consumption-process.md` — merchant/cartography physical result operation guard scope.
- `docs/cat/drafts/a12-station-consumption-pi.md` — source inputs and allow/cancel outputs.
- `docs/cat/drafts/a12-station-consumption-tce.md` — partial four-way decision partition for result/not-result and monetary marker/readability.

Technical choices remain unresolved: native merchant/cartography event sequences; whether all acquisition actions are caught via InventoryClickEvent, CartographyItemEvent, InventoryDragEvent or inventory-action-specific guard; result-slot 2 semantics per view; plugin callback priority/after-callback mutation; JE/BE and G0 journal integration. The statement `CartographyItemEvent` was *synthetically* dispatched must not be silently elevated to proof of native consumption.

This Work preserves the already approved restriction but does not itself authorize a particular Paper adapter, promote CAT normative Confirmed, or bypass Process Closure/Model/TDD/implementation conformance. The next `spec-implementation` Work requires legitimate decision provenance, confirmed Process/PI/TCE and formal compiler-owned Gates.

## Acceptance and integration

1. All three source-bound CAT Drafts have correct Process→PI/TCE provenance and pass the actual pinned CAT artifact linter; the issue-work structure passes the canonical validator.
2. Distinguish accepted gameplay meaning, proposed event technology, real Paper synthetic evidence and unverified final coin-consumption paths.
3. The PR modifies only three Draft files, one Work and its validation workflow. No monetary item/source, listener, config, guard, protected branch, or test-oracle edits.
4. The independent Draft/spec documentation increment may merge into `develop` after exact-head CI, with all formal product CAT Gates still not-run. No implicit human normative confirmation is recorded.
