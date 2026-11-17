---
cat_version: '0.2'
kind: work
id: simplecoins.a00.legacy-cat-draft-reconciliation
status: draft
process: simplecoins.paper.coin-identity
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/315
  - https://github.com/surumeneco/SimpleCoins/pull/319
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

# A00 — 旧PR #315/#319の未統合CAT Draftと現行実装の整合整理

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/315 |
| https://github.com/surumeneco/SimpleCoins/pull/319 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.paper.coin-identity;simplecoins.paper.coin-exchange;simplecoins.wallet | simplecoins.a00.legacy-cat-draft-reconciliation | simplecoins.currency.crafting-exclusion;simplecoins.a00.legacy-cat-draft-reconciliation |

## Technologies

| Technology |
| --- |
| GitHub exact Blob SHA source comparison; pinned CAT-TDD draft artifact-lint/issue-work validate; Java 25 source and JUnit evidence (read-only) |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a00.legacy-cat-draft-reconciliation.md;docs/cat/drafts/coin-identity-process.md;docs/cat/drafts/coin-identity-pi.md;docs/cat/drafts/coin-identity-tce.md;docs/cat/drafts/coin-exchange-formula-process.md;docs/cat/drafts/coin-exchange-formula-pi.md;docs/cat/drafts/coin-exchange-formula-tce.md;docs/cat/drafts/wallet-paper-gui-domain.md;.github/workflows/a00-legacy-cat-drafts.yml | — | .github/workflows/a00-legacy-cat-drafts.yml |

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

## Decision basis and bounded scope

The Issue #423 review left legacy PRs #315 and #319 open only because their CAT Draft files were absent from current develop. Actual code comparison against current develop@e28f2a8ded6c4f61e7119db8854d6a881d11c764 shows #315's CoinItemCodec, CoinExchangeFormula, two JUnit files byte-identical; CoinExchangeRecipes evolved in develop to validate actual recipe outputs rather than trusting recipe ID alone. For #319, WalletItemCodec, WalletCrafting, WalletMenu, WalletTransferTransaction and its JUnit file are byte-identical to current develop. SimpleCoinsPlugin has evolved beyond both old branches. Their old product code **must not be blindly merged or restored**.

Transfer the six separate coin identity / twenty-formula Draft Process/PI/TCE candidates (with the PI's backward reference to the TCE corrected) and the old wallet Paper GUI DomainSpec Draft into current docs/cat/drafts. Revise the wallet Draft's obsolete assertion that Paper adapter classes are missing and its old TaskQueue provenance, retaining concrete acceptance rules, observed implementation and unresolved durability. Keep existing a12-exchange-output-{process,pi,tce}.md as the distinct Paper crafting exclusion Process, not duplicate it with the formula semantics. All newly copied/updated artifacts remain **Draft**.

This is documentation reconciliation/cleanup only, not new product implementation, normatively Confirmed specifications, semantic approval, suite sync, or full CAT Gates. After exact current-artifact CAT syntax lint and Work structure validation, reconcile both old PRs with comments linking the merged independent replacement; close the superseded branches without merging stale code. Preserve admission default-deny, develop/current product, and main.

## Acceptance

- Exactly seven Draft artifacts under docs/cat/drafts and this Work plus dedicated CI; no production-source/test edits.
- Six coin/exchange Drafts retain stable Process/PI/TCE relationships; no Process TCE circular reference; TCE remains unconfirmed.
- Wallet Draft accurately states existing code and absence of durable G0/G3/normal-journal integration; no deprecated queue execution.
- CAT fixed script structure/lint passes; maintain exact code SHA fixtures and distinguish syntax check from formal normative Gate.
- Old PRs #315/#319 closed as superseded **only after** successful develop merge and independent readback, with explicit preservation of unique Drafts.
