---
cat_version: '0.2'
kind: work
id: simplecoins.grants.paper-inventory-slot-plan
status: draft
process: simplecoins.grants.paper-inventory-delivery
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

# Work: simplecoins.grants.paper-inventory-slot-plan

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantDeliveryBatchPlanner.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantReceiptState.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinItemCodec.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantReceiptPdc.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-inventory-delivery | simplecoins.grants.paper-inventory-slot-plan | simplecoins.grants.saved-receipt-universe-audit;simplecoins.grants.partial-delivery-batches |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.paper-inventory-slot-plan.md;src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java;src/test/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlanTest.java;src/test/java/co/surumene/simplecoins/paper/CoinRecipesLiveProbe.java | src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java | src/test/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlanTest.java;src/test/java/co/surumene/simplecoins/paper/CoinRecipesLiveProbe.java |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| grant-paper-slot-preflight | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.paper.PaperGrantInventorySlotPlanTest | partial-5200;occupied;full;changed-storage;forged-receipt;no-overwrite | GrantLedger;GrantDeliveryBatchPlanner;GrantReceiptState;Paper ItemStack | JUnit XML | 180 |

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

## Source-grounded acceptance and boundaries

- Adopted G3 and `03_運営設定・コマンド.md` require the offline grant to retain undelivered value when inventory is full, never overwrite existing physical items, split into canonical 64-item stacks, and keep inventory and progress receipt within the same future playerdata generation. The earlier `GrantDeliveryBatchPlanner` and `GrantReceiptState` already validate a durable grant, exact partial-value stack prefix, and non-skipped reservation; this Work must reuse rather than weaken them.
- Add a deterministic, strictly read-only Paper `ItemStack[]` storage-slot preflight. It must plan only vacant storage slots (no armor, offhand, crafting), keep stable denomination/count, produce the exact next `GrantReceiptState` for the selected canonical batch, and return no plan on full storage. The produced plan must be immutable with defensive snapshot isolation; reject an occupied slot or changed inventory/receipt before a future mutation.
- Do NOT infer actual durable inventory delivery from a successful in-memory plan. Do not mutate Player/Inventory/PDC, call `saveData()`, acknowledge GrantLedger, record monetary issuance, open admission, register a login handler, or claim exactly-once. These remain separately testable Work. This is not a release-ready implementation.
- Standalone JUnit models slot occupancy with an immutable list of representative fingerprints, because Bukkit `ItemFactory` is unavailable in the pure domain runner. An isolated official Paper 26.2 coin-recipe probe exercises real `ItemStack.serializeAsBytes()` for stable identity, denomination/PDC changes, count changes and disappearance. The test-only Paper probe belongs to the selected changed-file set.
- Tests cover original 5,200C split into 5,000C plus 2x100C under one free slot at a time; exact single-denomination 129x5C stacks 64/64/1; full capacity; unrelated occupied slots; foreign/stale/forged reservation; receipt and physical inventory drift before applying.
- The canonical CAT Work contract is mechanical scaffolding only. The Work is Draft, normative Process/PI/TCE Confirmed provenance not verified, official Gates not-run. GitHub CI outcomes are evidence for the narrow code/test only, not official conformance.
