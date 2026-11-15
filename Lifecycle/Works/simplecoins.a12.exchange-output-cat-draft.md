---
cat_version: '0.2'
kind: work
id: simplecoins.a12.exchange-output-cat-draft
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
work_kind: specification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A12 — 登録レシピ改変と結果出力完全性のCAT規範候補

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/444 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/43740989379 |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.crafting-exclusion | simplecoins.currency.pi.exchange-result;simplecoins.currency.tce.exchange-result | simplecoins.a12.exchange-recipe-result-integrity |

## Technologies

| Technology |
| --- |
| CAT-TDD markdown-v2; Java 25; Paper 26.2; current CoinExchangeRecipes observation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.exchange-output-cat-draft.md;docs/cat/drafts/a12-exchange-output-process.md;docs/cat/drafts/a12-exchange-output-pi.md;docs/cat/drafts/a12-exchange-output-tce.md;.github/workflows/a12-exchange-output-cat-draft.yml | — | .github/workflows/a12-exchange-output-cat-draft.yml |

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

## Design status and decision handoff

- Existing **confirmed game requirement** in Drive 01: physical coin PDC/model identity is fixed, 20 shapeless exchanges preserve value and tagged coin cannot be consumed by ordinary paper crafting.
- Observed real Paper 26.2 counterexample in PR #444: register a `STONE` output under `simplecoins:exchange_n1_f1`; Paper accepts the replacement and `CoinExchangeRecipes.isApproved` still reports true for genuine coin inputs. This proves only the guard approval path, not a completed live-player craft.
- Process/PI/TCE under `docs/cat/drafts/a12-exchange-output-*` are **unapproved Drafts**, not Confirmed. They describe candidate output/result integrity at Prepare/Craft/Crafter events and rejection of unknown/tampered outputs. They cannot be moved to Confirmed by the AI or by a passing linter/CI.
- Decision needed before production: authoritative observation of **actual result** relative to a later-priority plugin, inventory click, Shift repetition and journal commit. If it cannot be guaranteed at the same time as item consumption, define an alternative fail-closed/cancel mechanism and residual risk to the user's approval; do not invent automatic promotion.
- After legitimate normative approval: run canonical CAT process-closure, model-conformance, TCE vectors and generated-current-only Red/Green in an appropriate Work, then perform Paper integration and develop merge. **No source edits, no registry guard disable, and no admission opening are authorized by this Draft alone.**
