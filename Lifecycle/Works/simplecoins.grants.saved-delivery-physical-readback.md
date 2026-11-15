---
cat_version: '0.2'
kind: work
id: simplecoins.grants.saved-delivery-physical-readback
status: draft
process: simplecoins.grants.saved-delivery-proof
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

# Work: simplecoins.grants.saved-delivery-physical-readback

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbt.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/SavedPlayerAssets.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.saved-delivery-proof | simplecoins.grants.saved-delivery-physical-readback | simplecoins.grants.pdc-receipt;simplecoins.grants.planned-storage |

## Technologies

| Technology |
| --- |
| Java 25, Paper 26.2 item NBT, JUnit 5, GZIP/NBT |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.saved-delivery-physical-readback.md;src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadback.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadbackTest.java | src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadback.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadbackTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| grant-saved-item-proof | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.paper.SavedGrantDeliveryReadbackTest | full-grant;partial-grant;count-tamper;denomination-tamper;receipt-tamper;unrelated-storage | PlayerDataReader;StoredItemValue;SavedGrantReceiptNbt;PaperGrantInventorySlotPlan | JUnit XML | 180 |

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

## Source-grounded acceptance

- Adopted 03 G3: A completed or partial offline grant must never be acknowledged or replayed based on an isolated receipt alone. The physical player inventory and receipt need verification from one authoritative saved playerdata generation before journal acknowledgement.
- Existing source provides (a) `SavedGrantReceiptNbt.readAndVerify` binding a playerdata PDC to the durable GrantLedger and (b) `SavedPlayerAssets.extract` with strict saved inventory and physical currency value recognition. Existing `PaperGrantInventorySlotPlan` describes the exact new slot indices, denominations, counts and next receipt.
- This Work is **read-only** and accepts one already-decoded, same-root playerdata Map plus durable journal, player UUID and an immutable prior planned issuance. It must verify the next receipt equals the one saved, every planned slot has a **matching canonical coin material, count, PDC denomination and item_model**, and all saved inventory sources are structurally accounted. A same-value but different coin denomination/count is **not** proof of this exact delivery.
- For the planned slot, reject absent slot, malformed NBT, wrong count, wrong denomination, wrong model, duplicate slots, wallet conflicts and changed receipt. Existing unrelated storage contents are not mutated; result is a read-only, bounded observation.
- Tests must use the actual bounded compressed PlayerDataReader and NBT parser for at least one valid and tampered fixture; other fixtures may use typed saved NBT Map. Red is the expected absence of the new readback type, Green/regression is actual JUnit/CI.
- **Hard boundary:** This observation does not prove that Paper `Player#saveData()` synchronously saved inventory and PDC, that inventory remained immobile afterward, or that a saved file cannot roll back. It does not itself put items into an inventory, save playerdata, acknowledge a grant, connect a Paper login listener, publish settlement, or open admission. CAT normative Process/PI/TCE Confirmed and mechanical Gates remain not-run unless independently obtained.
