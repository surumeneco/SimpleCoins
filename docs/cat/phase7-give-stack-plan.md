---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.give-stack-plan
status: draft
process: simplecoins.admin-give
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: Pure give stack planning

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.admin-give | Translate a requested monetary amount or denomination quantity to per-recipient stack specifications |
| simplecoins.currency-denominations | Use the existing nine fixed denominations |
| simplecoins.issuance-budget | Respect the existing global per-command stack budget |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| GIVE-PLAN-01 | Amount 5200 | One 5000 coin and two 100 coins, keeping exact value | amount5200UsesFewestCoinsWithExactValue |
| GIVE-PLAN-02 | Amount of 650000 | Split denomination stack counts into at most 64 items each | amountAbove64CoinsSplitsIntoValidStacks |
| GIVE-PLAN-03 | Explicit denomination count 129 | Produce stack quantities 64, 64 and 1 | specificDenominationSplits129Into64_64_1 |
| GIVE-PLAN-04 | Multiple recipients | Check the global stack budget before generating any stacks | recipientBudgetUsesWholeCommandNotEachPlayer |
| GIVE-PLAN-05 | Zero, negative or oversized request | Reject invalid inputs and guard expansion | invalidAndExtremeRequestsFailBeforeExpansion |
| GIVE-PLAN-06 | Successful plan | Return unmodifiable stack values in descending denomination order | returnedPlanIsImmutable |

## Scope

This is an implementation Draft for the confirmed gameplay requirements in SimpleCoins design documents 01 and 03. It delegates monetary decomposition and stack capacity enforcement to the existing CoinMoney and IssuanceBudget public APIs. It does not generate Minecraft inventory items, perform grant reservations, decide offline identity, or claim JE/BE acceptance. This Draft is not a confirmed normative CAT artifact; semantic gates and implementation gates must be recorded separately.
