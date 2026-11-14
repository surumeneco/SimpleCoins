---
cat_version: '0.2'
kind: work
id: simplecoins.grants.saved-receipt-universe-audit
status: draft
process: simplecoins.grants.saved-receipt-audit
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

# Work: simplecoins.grants.saved-receipt-universe-audit

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbt.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.saved-receipt-audit | simplecoins.grants.saved-receipt-universe-audit | simplecoins.grants.durable-journal;simplecoins.grants.saved-playerdata-receipt-audit |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2; NBT |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.saved-receipt-universe-audit.md;src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAuditTest.java | src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAuditTest.java |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| receipt-universe | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.paper.SavedGrantReceiptUniverseAuditTest | acknowledged-missing;rollback;pending-no-player;ack-recovery;manual-manifest-denied | GrantLedger;PlayerDataReader;SavedGrantReceiptNbt;WorldStorageDiscovery | JUnit XML | 180 |

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

- Adopted G3: issuance grants are durable, and a completed/acknowledged grant cannot be recovered from absent or rolled-back playerdata as a fresh reservation. Pending grants for yet-unseen players are NOT automatically issued or counted toward circulating currency.
- Existing `GrantLedger` verifies checksum/head/record history and receipt consistency for one player, but startup must also enumerate all previously acknowledged grants to detect playerdata that vanished entirely. Existing `WorldStorageDiscovery.Manifest` has a retained origin only after a physical, bounded full directory discovery and can recheck file source-set closure. Existing `PlayerDataReader` and `SavedGrantReceiptNbt` decode and verify one saved playerdata NBT receipt.
- Scope: add immutable, read-only `GrantLedger.historySnapshot()` with full durable integrity verification, then `SavedGrantReceiptUniverseAudit.scan()` over *all* manifest playerdata files. Reject a manually fabricated Manifest lacking discovery provenance; check directory closure before/after scanning; verify each real saved player's receipt against the journal; reject any acknowledged grant whose playerdata file is missing or whose receipt rolled back. A pending grant with a missing playerdata file remains pending, never counted as delivered. Count pending grants for which the durable playerdata receipt is ahead of journal acknowledgement, but do not acknowledge automatically.
- Include GZIP NBT fixtures (the same actual PlayerDataReader parser used for saved playerdata) with empty and completed receipts, missing file, malformed receipt and recovered pending. Audit/report remains observational: no database publication, no grant acknowledgements and no Bukkit/player state mutations.
- Bounded caveat: File source-set closure alone cannot prove simultaneous content-generation coherence or that a real Paper.saveData() completed. This Work is a conservative read-only candidate, deliberately NOT registered in SimpleCoinsPlugin; it does not open admission or issue currency. Contents may change between read and recheck; future integration needs a stable content witness, Paper lifecycle proof and crash injection.
- CAT Work Draft syntax validated with pinned 896cae source; legitimate normative Process/PI/TCE Confirmed and Gates remain not-run.
