---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-before-save-sigkill-recovery
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/467
  - https://github.com/surumeneco/SimpleCoins/pull/468
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A02/G3 — 実Paperの保存前SIGKILL・予約維持・再起動後初回配布

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/466 |
| https://github.com/surumeneco/SimpleCoins/pull/467 |
| https://github.com/surumeneco/SimpleCoins/pull/468 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-before-save-sigkill-recovery | simplecoins.a02.paper-partial-receipt-restart;simplecoins.a02.paper-final-save-pre-ack-crash |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 build 134; isolated JAR; Paper playerdata; SIGKILL; real persisted GrantLedger; JUnit/domain regression via CI |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-before-save-sigkill-recovery.md;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Evidence gap and bounded experiment

The existing real-Paper test suite exercised saved partial delivery, SIGKILL, saved final receipt before ACK, recovery-only ACK and idempotent ACK across independent Paper JVMs. It has **not** independently exercised the opposite boundary: a genuinely reserved grant materialized only into a detached Paper player's in-memory inventory/PDC, then a forced process crash **before** any playerdata save call.

This verification Work adds a disposable, guarded, offline Paper sequence *after* the existing seventh boot:

1. Eighth JVM: load three previously acknowledged authentic grants and their saved physical money; persist a free slot as an independent setup step, then reserve grant sequence 4. Prepare the exact coin/receipt pair using the production plan/materializer, update only detached in-memory player storage/PDC, and **do not save** that staged pair. Compare authoritative UUID.dat with the bytes immediately before staging, verify durable pending grant 4 and force SIGKILL (exit 137), with admissions closed.
2. Ninth JVM: load from authentic UUID.dat and confirm the newly staged physical coin and completed receipt **did not appear**. Verify the original 660,000 C, unrelated inventory and completed receipt 3 remain; grant 4 is still pending. Use the same production staging/materializer, perform genuine Paper saveData, verify one-file inventory/receipt transition by the production after-save file-readback adapter, then and only then ACK sequence 4.
3. Tenth JVM: verify saved fourth coin and receipt, four delivered authentic journal records, zero pending reservations and no duplicate/reissued monetary value. Repeat ACK against an authentic delivered handle and compare disk bytes unchanged.

This is not a normative CAT Gate and never opens the admission guard, changes product code, deploys to VPS or promotes Draft to Confirmed. It is limited to one synthetic offline player, a single server's filesystem and Paper JVM SIGKILL (not OS power loss). A pass provides actual Paper evidence for this specific crash insertion point only, not G3 exactly-once proof for concurrent clients or every external plugin writer.

## Acceptance and failure rule

- All existing seven Paper phases remain passing; three additional independent JVMs use exact log markers and crash status checks.
- No partially staged physical money or receipt survives pre-save SIGKILL; the previously saved slot vacancy survives, while the journal reservation survives unchanged and pending.
- One real save + file proof + durable ACK produces precisely one additional coin and no loss of existing currency; tenth JVM validates replay without writes.
- Any mismatch, unexpected autosave, premature ACK, missing saved source, premature clean shutdown or failed CI preserves fail-closed status and blocks integration. Keep actual CI success distinct from formal CAT compliance.
