---
cat_version: '0.2'
kind: work
id: simplecoins.a02.saved-transition-readback
status: draft
process: simplecoins.grants.saved-transition-integrity
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
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.a02.saved-transition-readback

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadback.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.saved-transition-integrity | simplecoins.grants.saved-preserved-slots | simplecoins.grants.saved-delivery-proof;simplecoins.grants.planned-storage |

## Technologies

| Technology |
| --- |
| Java 25, Paper 26.2 saved NBT, JUnit 5 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.saved-transition-integrity.md;src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadback.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadbackTest.java | src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadback.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryReadbackTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| grant-saved-preservation | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.paper.SavedGrantDeliveryReadbackTest | unchanged-occupied;changed-count;deleted;wrong-PDC;unplanned-extra;pre-existing-source;receipts | SavedGrantReceiptNbt;SavedPlayerAssets;SavedGrantDeliveryReadback | JUnit XML | 180 |

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

- G3 requires a physical grant to preserve all existing player storage and avoid any unapproved loss or extra issuance. Current `SavedGrantDeliveryReadback.verify` only checks *planned new* coin slots, and can accept lost/rewritten **unplanned** inventory items; that is not sufficient as a transaction proof.
- A new **read-only** before/after same-playerdata-root transition comparator verifies the grant receipt and every existing non-target storage slot (including arbitrary NBT byte[] contents), preserves Ender Chest and equipment, requires target empty before and correct canonical coin after, and rejects all unplanned new slots.
- It must not infer that the two observations are consecutive durable Paper generations. It does not change any inventory, PDC, journal or admission. `saveData()` synchronous persistence, ABA and crashed on-disk last writer remain separate unknowns under A02.
- Red must fail for a real missing behavior, Green and regression are separate from normative CAT gates; authority remains Draft, and approval/Gates must not be self-issued.

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
