---
cat_version: '0.2'
kind: work
id: simplecoins.grants.partial-delivery-batches
status: draft
process: simplecoins.grants.partial-delivery
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

# Work: simplecoins.grants.partial-delivery-batches

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantDistributionPlan.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.partial-delivery | simplecoins.grants.partial-delivery-batches | simplecoins.grants.durable-journal;simplecoins.currency.pdc-fixed |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.partial-delivery-batches.md;src/main/java/co/surumene/simplecoins/domain/GrantDeliveryBatchPlanner.java;src/test/java/co/surumene/simplecoins/domain/GrantDeliveryBatchPlannerTest.java | src/main/java/co/surumene/simplecoins/domain/GrantDeliveryBatchPlanner.java | src/test/java/co/surumene/simplecoins/domain/GrantDeliveryBatchPlannerTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| grant-delivery-batches | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.GrantDeliveryBatchPlannerTest | amount-prefix;exact-denomination;inventory-full;non-prefix-reject;already-complete | GrantLedger;GrantDistributionPlan;IssuanceBudget | JUnit XML | 180 |

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

## Source-grounded acceptance and boundaries

- **Adopted specification**: An offline amount reservation is *not* physical issued money; grant history and playerdata receipt/inventory must survive restart and never double issue. When inventory is full, unreceived amount remains pending and delivery retries. The design notes record a currently processed grant sequence and cumulative delivered amount in the same playerdata save. A partial delivery may be split over repeated logins, but successful receipt advancement is never inferred without saved playerdata evidence.
- **Observed code**: `GrantLedger` stores monetary amount, recipient, optional exact denomination and item count; `GrantDistributionPlan` constructs a globally bounded immutable list of fixed-identity coin stacks for a grant of at most 1,024 output stacks. Neither exposes a cursor proving that a persisted cumulative delivered *value* is precisely one prefix of those stacks. Without that constraint a forged or intermediate amount could resume at an arbitrary decomposition and duplicate/miss coins.
- **Scope**: pure `GrantDeliveryBatchPlanner` consuming a grant and *verified externally persisted* cumulative delivered value with available inventory slot count. It reconstructs the original denomination-preserving 64-item stack order, requires the delivered value exactly match a prefix boundary, then selects the next 0..capacity stacks without value duplication, returning an immutable batch with new cumulative value, completion flag and no side effects. Reject negative/excess/non-prefix progress, unbounded preflight, or previously delivered grants not in the pending path. Arithmetic must stay exact.
- **Tests-first**: 5,200C with one slot = 5,000C then 2x100C, exact denomination 129 coins = stack counts 64/64/1 across three one-slot batches, 0 free slots preserves state, invalid partial amount (e.g. 100C before 5,000C) rejected, too-large grant refuses unbounded materialization. The immutable output remains bounded.
- **Out of scope**: actual Paper ItemStack insertion, filesystem playerdata receipt save/flush, durable ledger acknowledgement, world/journal monetary settlement, player admission, online/offline resolution and public command names. This Work alone **does not prove exactly-once delivery**; that needs crash-injection at every physical save stage.
- **CAT boundaries**: issue-work Draft only, no self-Confirmed formal Process/PI/TCE; mechanical Work contract validation and ordinary JUnit Red/Green are distinct from normative CAT process/model/implementation Gates.

## Observed Red / Green and concurrent develop change

- Red head 9fbf9794dd6f7b55ce00f682399c42295fd5707d: [Domain foundation run 43609984719](https://github.com/surumeneco/SimpleCoins/actions/runs/43609984719) failed as expected because new GrantDeliveryBatchPlanner type was absent.
- Green head 9de089fad6fc8c997e09ba29ca0ffffba1a5491d: [Domain foundation run 43610110370](https://github.com/surumeneco/SimpleCoins/actions/runs/43610110370) completed successfully for the added planner and existing Domain/guard JUnit regressions.
- Concurrent target: [PR #414](https://github.com/surumeneco/SimpleCoins/pull/414) was squash-merged as 51936c1f556acb16346b1b6953527f90d263ea2c. Both Works change different files. The newly triggered PR check must verify the combination against the newer develop target before integrating this one.
- At this entry: final combined CI, actual playerdata save, real issuance, formal CAT Gates, and release are not yet verified.
