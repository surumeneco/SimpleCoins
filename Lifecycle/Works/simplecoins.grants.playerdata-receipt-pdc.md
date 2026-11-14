---
cat_version: '0.2'
kind: work
id: simplecoins.grants.playerdata-receipt-pdc
status: draft
process: simplecoins.grants.playerdata-receipt
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

# Work: simplecoins.grants.playerdata-receipt-pdc

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantDeliveryBatchPlanner.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.playerdata-receipt | simplecoins.grants.playerdata-receipt-pdc | simplecoins.grants.durable-journal;simplecoins.currency.pdc-fixed;simplecoins.grants.partial-delivery |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2; JUnit 5 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.playerdata-receipt-pdc.md;src/main/java/co/surumene/simplecoins/domain/GrantReceiptState.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantReceiptPdc.java;src/test/java/co/surumene/simplecoins/domain/GrantReceiptStateTest.java;src/test/java/co/surumene/simplecoins/paper/PaperGrantReceiptPdcTest.java | src/main/java/co/surumene/simplecoins/domain/GrantReceiptState.java;src/main/java/co/surumene/simplecoins/paper/PaperGrantReceiptPdc.java | src/test/java/co/surumene/simplecoins/domain/GrantReceiptStateTest.java;src/test/java/co/surumene/simplecoins/paper/PaperGrantReceiptPdcTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| receipt-domain | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.GrantReceiptStateTest | complete-prefix;forged-active;failed-journal;owner-validation;rollover | GrantLedger;GrantDeliveryBatchPlanner | JUnit XML | 180 |
| receipt-pdc | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.paper.PaperGrantReceiptPdcTest | roundtrip;partial;malformed;wrong-type | Paper PDC BYTE_ARRAY | JUnit XML | 180 |

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

- **Existing approved design** (`03_運営設定・コマンド.md`, 2026-10-29 implementation approach): pending issuance reservations are distinct from physical coins; on delivery, inventory and receipt/progress must be saved together to the same Minecraft playerdata generation. A completed receipt avoids repeated issuance. While partially delivered, store the active reservation's sequence and cumulative amount, without discarding pending remainder. Mismatches, failed saves, missing/corrupt receipt must fail closed.
- **Observed code**: `GrantLedger` persists checksum-verified reservation sequence, UUID, recipient, amount, issuer, and optional chosen denomination. `GrantDeliveryBatchPlanner` already validates an arbitrary external `long` as a canonical full-stack prefix, but cannot prove the value belongs to this UUID/grant. No production PDC codec currently stores the coherent receipt and progress as one playerdata field.
- **Work boundary**: a pure `GrantReceiptState` tracks last completed reservation sequence and optionally one active reservation sequence+canonical grant UUID+cumulative delivered amount. Enforce invariants, check latest saved state against the actual `GrantLedger` and authenticated player identity, verify the active cumulative amount as a canonical prefix and prohibit skipping an earlier pending grant; expose transitions for one bounded `GrantDeliveryBatchPlanner.Batch` and full completion. A `PaperGrantReceiptPdc` adapter strictly reads/writes the entire state in **one BYTE_ARRAY PDC key** with fixed schema+SHA-256 integrity so intermediate in-memory field writes cannot describe a half-updated receipt. Absent key is the only empty state; wrong type, missing bytes, invalid checksum or malformed content MUST fail, not become zero.
- **Acceptance tests**: absent/valid state, partial 5,000 of 5,200 and 5 Coin x129 cursor, full inventory preserved, wrong grant ID/owner/untracked sequence/invalid prefix/negative progress fail; corrupt/torn/wrong-type PDC never interpreted as empty; completed receipt remains valid and allows `GrantLedger.acknowledge` after a subsequent restart.
- **Out of scope**: no registration of player login/events or `Player.saveData()`, no actual inventory mutation/coin issuance, no admission opening, no exactly-once proof. Future physical delivery MUST establish same-generation inventory/PDC playerdata save and actual saved-file readback under crash injection before this adapter can be wired for player-facing use.
- **CAT**: Issue→Work Draft generated with immutable CAT 896cae runner and mechanically validated; no normative Process/PI/TCE Confirmed, no model/implementation Gates are asserted from JUnit success.
