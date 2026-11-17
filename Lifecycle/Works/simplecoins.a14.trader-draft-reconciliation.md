---
cat_version: '0.2'
kind: work
id: simplecoins.a14.trader-draft-reconciliation
status: draft
process: simplecoins.paper.trader-drop-config
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/316
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: documentation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A14 — 行商人発行関連の旧CAT Draftを現行の10%仕様へ照合

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/316 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.paper.trader-drop-config;simplecoins.paper.trader-drop-selection;simplecoins.paper.trader-event;simplecoins.paper.trader-stack-materialization | simplecoins.a14.trader-draft-reconciliation | simplecoins.a14.trader-issuance;simplecoins.grants.paper-save-commit |

## Technologies

| Technology |
| --- |
| Fixed upstream CAT-TDD 896cae83 lint/Work validator; current GitHub source/blob; read-only config SHA evidence |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a14.trader-draft-reconciliation.md;docs/cat/drafts/trader-config-process.md;docs/cat/drafts/trader-config-pi.md;docs/cat/drafts/trader-config-tce.md;docs/cat/drafts/trader-selection-process.md;docs/cat/drafts/trader-selection-pi.md;docs/cat/drafts/trader-selection-tce.md;docs/cat/drafts/trader-event-process.md;docs/cat/drafts/trader-event-pi.md;docs/cat/drafts/trader-event-tce.md;docs/cat/drafts/trader-materialization-process.md;docs/cat/drafts/trader-materialization-pi.md;docs/cat/drafts/trader-materialization-tce.md;.github/workflows/a14-trader-draft-reconciliation.yml | — | .github/workflows/a14-trader-draft-reconciliation.yml |

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

## Scope and technical decision

The legacy [PR #316](https://github.com/surumeneco/SimpleCoins/pull/316) contains twelve separate CAT **Draft** Process/PI/TCE artifacts for trader config, weighted selection, death event/journal and stack materialization. These artifacts are missing on develop even though the candidate product classes exist. Do **not** merge the old stacked code: it predates later safety fixes and user default changes.

The authoritative [03 design](https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view) explicitly changed default drop probability to **0.1 / 10% on 2026-11-12**, superseding 2026-11-11's 50%. The current `src/main/resources/config.yml` agrees with `chance: 0.1`; all nine face values and weights 256/128/64/32/16/8/4/2/1 remain. The unmerged Drafts still mention 50%: update them before preserving. The selection TCE `enabled=true` alone never implies a drop when probability is less than one; avoid claiming closed full-branch proof. Correct self/circular reference edges in event PI/TCE.

Migrated files stay `status: draft`; they are **not** a decision, source-test sync, normative CAT Confirmed, complete trader issuance, or Paper/JE/BE acceptance. `TraderDeathIssuanceListener` exists but is intentionally unregistered until G0/G3 journal/saved-world atomicity and normal accounting succeed. Do not enable it, admit players or touch main.

## Acceptance

- Exactly twelve source-bound trader CAT Drafts, four Process/PI/TCE sets, preserve distinct source references and current 10% default.
- Correct Process↔PI↔TCE reference directions and explicit partial coverage where random draws are not enumerated.
- CI validates real upstream CAT Work/artifact syntax, Java/config source identity and no production rollback.
- If all tests succeed, merge the independent documentation work to develop, then assess old PR #316 for remaining genuinely unique code and close it as superseded only if those paths are already covered.
