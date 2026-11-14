---
cat_version: '0.2'
kind: work
id: simplecoins.grants.distribution-plan
status: draft
process: simplecoins.give.command-neutral-plan
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
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

# Work: simplecoins.grants.distribution-plan

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/IssuanceBudget.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.give.command-neutral-plan | simplecoins.grants.distribution-plan | simplecoins.grant.exactly-once;simplecoins.currency.pdc-fixed |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.distribution-plan.md;src/main/java/co/surumene/simplecoins/domain/GrantDistributionPlan.java;src/test/java/co/surumene/simplecoins/domain/GrantDistributionPlanTest.java | src/main/java/co/surumene/simplecoins/domain/GrantDistributionPlan.java | src/test/java/co/surumene/simplecoins/domain/GrantDistributionPlanTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| distribution-plan | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.GrantDistributionPlanTest | amount-minimum;exact-coin-stacks;wallet-unit-stacks;multi-recipient-budget;overflow-reject | CoinMoney;IssuanceBudget | JUnit XML | 180 |

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

## Source-grounded behavior and acceptance

- **Already decided by user**: `03_運営設定・コマンド` defines two give forms, amount -> minimum coin decomposition and explicit item/count -> exact denomination or nonstackable wallet, splitting coins at 64 per stack, with each selected target receiving the whole request. `05_1.0.0_実装計画` requires permission and crash-safe issuance but not a particular command name.
- **Current source**: `CoinMoney.minimumCoins`, `CoinDenomination`, and `IssuanceBudget` implement denominations and a 1024-total-output-stack budget, but there is no immutable command-neutral per-recipient issuance layout linking decomposition, actual 64-size item stacks, wallet stack size 1, multi-recipient count and exact total value.
- **Scope**: define a pure immutable `GrantDistributionPlan` value with three constructors `forAmount`, `forExactCoins`, `forEmptyWallets`. It must calculate each physical coin stack (1..64), preserve denomination and value, apply the **global** stack budget to all recipients *before* allocating stacks, protect `long` amount multiplication/total and expose unbounded total command value for multiple targets. No random wallet UUID should be preallocated or cloned.
- **Tests-first contract**: 5200 -> 5000 x1 + 100 x2, 129 exact 1-C coins -> stack amounts 64/64/1, wallets count as one inventory stack each, limits are exact across multiple recipients (1024 accepted; 1025 rejected), zero/negative/overflow and empty targets rejected, output list immutable and identities stable across name changes.
- **Release boundary**: domain plan only; do not register a Paper `/give` command, issue actual currency, update grants, publish settlement, admit players, change the undecided public command name or merge `main`. Later integration requires authenticated UUID target resolution, durable `GrantLedger` reserve/receipt, inventory save and real monetary journal accounting.
- **CAT authority**: issue-work Draft, not self-Confirmed Process/PI/TCE; contract lint/independent JUnit Red/Green may be recorded without declaring normative CAT Gate success.
