---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-final-save-pre-ack-crash
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/467
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

# G3/A02 — 部分配布最終保存直後・未ACK SIGKILLと次回起動のACK回復

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/467 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantReceiptState.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptNbt.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/SavedPlayerAssets.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-final-save-pre-ack-crash | simplecoins.a02.paper-partial-receipt-restart;simplecoins.a02.paper-real-plan-save-readback |

## Technologies

| Technology |
| --- |
| Paper 26.2 build 134; Java 25; seven isolated actual JVMs; playerdata GZIP NBT; real durable GrantLedger journal |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-final-save-pre-ack-crash.md;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | 

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

## Existing decision and acceptance

The previously accepted Drive 03/02B grant design requires exactly one accepted grant's physical currency and a complete/partial receipt to live in the SAME real playerdata save. If physical money has been durably saved with a complete receipt but the independent journal ACK was interrupted, subsequent startup must recognize that receipt and finish **only** its durable journal ACK, never create the money a second time. Partial nonfinal receipts remain pending even through SIGKILL.

The existing #467 six-JVM test validates a crash **after** the first 128 of 130 coins are saved (receipt active=3, partial=640000) and a clean ACK after the remaining 2 coins are saved. Keep JVMs 1..5, including the first forced crash unchanged. In JVM 6, after the real Paper playerdata has saved and verified the last two 5000-denomination coins and completed receipt=3, and after proving the ledger is still pending, emit a stable marker and **SIGKILL before** any `GrantLedger.acknowledge` or clean shutdown. Use GitHub Actions to require a signal-9 exit 137 and ensure Paper has not entered shutdown.

In JVM 7, ensure the product's own startup source/receipt audit completes without fail-open or fail-stop. Load exactly the same detached player UUID and saved data. The original unrelated stone, prior two single-coin grants, the two 64-coin stacks and remaining 2 coins must all be present and recognized with original real PDC denomination, not just display PAPER. Verify the saved complete receipt=3 against the pending actual journal via production NBT decoder, and require that 3 is still pending before recovery. Call ACK **without delivering any inventory**; independently reload the durable journal (3 delivered, 0 pending), then repeat ACK and check journal bytes and playerdata bytes unchanged. Assert no connected user and default-deny admission.

This is verification-only `issue-work`. No product coordinator, command, inventory mutation on a live human player, online admission release, G0 simultaneous world closure, OS power-loss durability or fully deployed G3. CAT Process/PI/TCE Drafts and normative Gates are not being silently Confirmed.
