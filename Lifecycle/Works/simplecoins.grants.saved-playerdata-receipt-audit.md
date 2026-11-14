---
cat_version: '0.2'
kind: work
id: simplecoins.grants.saved-playerdata-receipt-audit
status: draft
process: simplecoins.grants.saved-playerdata
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

# Work: simplecoins.grants.saved-playerdata-receipt-audit

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantReceiptPdc.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/PlayerDataReader.java |
| https://github.com/PaperMC/Paper/blob/main/paper-server/src/main/java/org/bukkit/craftbukkit/CraftOfflinePlayer.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.saved-playerdata | simplecoins.grants.saved-playerdata-receipt-audit | simplecoins.grants.durable-journal;simplecoins.grants.playerdata-receipt-pdc |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2; NBT |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.saved-playerdata-receipt-audit.md;src/main/java/co/surumene/simplecoins/paper/PaperGrantReceiptPdc.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbt.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbtTest.java | src/main/java/co/surumene/simplecoins/paper/PaperGrantReceiptPdc.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbt.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbtTest.java |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| saved-receipt-readback | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.paper.SavedGrantReceiptNbtTest | nbt-parse;corruption;identity;non-prefix;absent-receipt | PlayerDataReader;GrantLedger;PaperGrantReceiptPdc | JUnit XML | 180 |

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

## Source-grounded acceptance

- Adopted 03 design: player inventory and last completed receipt or active grant sequence/partial monetary value are saved as one playerdata generation and reread during crash recovery; damage or inconsistency denies further issuance rather than treating receipt as empty.
- Observed SimpleCoins: `PlayerDataReader.read()` reads compressed authoritative playerdata via bounded NBT; `GrantReceiptState.verify()` independently validates saved progress against durable `GrantLedger`. `PaperGrantReceiptPdc` currently reads the receipt only from live PDC; the saved NBT read-back path is missing.
- PaperMC source for `CraftOfflinePlayer.getPersistentDataContainer()` reads the top-level NBT compound `BukkitValues`; Paper PDC entries are namespaced NBT keys. The current `PaperGrantReceiptPdc.RECEIPT` is `simplecoins:grant_receipt_v1`, BYTE_ARRAY type. Verify with actual Paper 26.2 before relying on this beyond read-only staging.
- Scope: expose the existing exact byte format for saved-file inspection without duplicating encoding logic; parse `BukkitValues.simplecoins:grant_receipt_v1` from an independently decoded `Map<String,Object>` and verify the resulting `GrantReceiptState` against the correct player and the durable grant journal. Absent PDC is empty only if no receipt key exists, and a previously acknowledged grant's missing receipt must still fail journal consistency. Wrong-type field, malformed compound, torn/changed bytes, noncanonical partial value, wrong grant ID or owner must fail closed.
- Regression: write actual compressed NBT test fixtures via `DataOutputStream` and `GZIPOutputStream`, reread through `PlayerDataReader`, verify progress & complete receipt; exercise mismatched/missing/wrong-type NBT; include incomplete earlier grant and physical playerdata corruption cases.
- Out of scope: this is a pure READ-ONLY verified candidate parser, not registered in `SimpleCoinsPlugin`; it cannot prove the same-generation `Player.saveData()` save or grant exactly-once in real Paper. No physical mint, no server entry permission, no release.
- CAT remains Issue→Work Draft (contract syntax only), normative Process/PI/TCE and mechanical semantic Gates not-run.
