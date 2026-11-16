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
| ci | passed | [actual Paper #43862051940](https://github.com/surumeneco/SimpleCoins/actions/runs/43862051940); [Domain #43862051908](https://github.com/surumeneco/SimpleCoins/actions/runs/43862051908); [Phase6 #43862051942](https://github.com/surumeneco/SimpleCoins/actions/runs/43862051942) (ordinary CI only, tested feature HEAD `0dd252dc8d6a33389b76d74c31a0aac82718b2c7`) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Existing decision and acceptance

The previously accepted Drive 03/02B grant design requires exactly one accepted grant's physical currency and a complete/partial receipt to live in the SAME real playerdata save. If physical money has been durably saved with a complete receipt but the independent journal ACK was interrupted, subsequent startup must recognize that receipt and finish **only** its durable journal ACK, never create the money a second time. Partial nonfinal receipts remain pending even through SIGKILL.

The existing #467 six-JVM test validates a crash **after** the first 128 of 130 coins are saved (receipt active=3, partial=640000) and a clean ACK after the remaining 2 coins are saved. Keep JVMs 1..5, including the first forced crash unchanged. In JVM 6, after the real Paper playerdata has saved and verified the last two 5000-denomination coins and completed receipt=3, and after proving the ledger is still pending, emit a stable marker and **SIGKILL before** any `GrantLedger.acknowledge` or clean shutdown. Use GitHub Actions to require a signal-9 exit 137 and ensure Paper has not entered shutdown.

In JVM 7, ensure the product's own startup source/receipt audit completes without fail-open or fail-stop. Load exactly the same detached player UUID and saved data. The original unrelated stone, prior two single-coin grants, the two 64-coin stacks and remaining 2 coins must all be present and recognized with original real PDC denomination, not just display PAPER. Verify the saved complete receipt=3 against the pending actual journal via production NBT decoder, and require that 3 is still pending before recovery. Call ACK **without delivering any inventory**; independently reload the durable journal (3 delivered, 0 pending), then repeat ACK and check journal bytes and playerdata bytes unchanged. Assert no connected user and default-deny admission.

This is verification-only `issue-work`. No product coordinator, command, inventory mutation on a live human player, online admission release, G0 simultaneous world closure, OS power-loss durability or fully deployed G3. CAT Process/PI/TCE Drafts and normative Gates are not being silently Confirmed.

## Verified real Paper fault-injection evidence

- The initial exact-head [Paper CI #43861854349](https://github.com/surumeneco/SimpleCoins/actions/runs/43861854349) failed at **Work structural validation only**, reporting `## Checks/検査: invalid Markdown table separator`: a 9-column header had 10 separator columns. This was corrected in the Work itself and is not counted as any Java Red or game-behavior failure.
- On corrected HEAD `0dd252dc8d6a33389b76d74c31a0aac82718b2c7`, [actual Paper #43862051940](https://github.com/surumeneco/SimpleCoins/actions/runs/43862051940), Domain #43862051908 and Phase6 #43862051942 all completed success. All five earlier independent Paper phases passed, including the previous partial receipt 640000 and SIGKILL.
- At the sixth real Paper JVM, the pre-existing first two 5000 coins and original STONE survived, the two 64-coin stacks and final two-coin stack were saved with completed receipt=3; `SavedGrantDeliveryFileReadback.verify` succeeded while reservation3 was still pending. Without calling `GrantLedger.acknowledge`, the process was SIGKILLed with observed exit137; runner logged `PAPER_A02_FINAL_SAVED_RECEIPT_BEFORE_ACK_SIGKILL_INJECTED`.
- Seventh independent JVM booted the genuine SimpleCoins journal, passed its actual startup saved-source/receipt audit, read full compressed shared Paper playerdata, and verified the 5 money slots' denomination PDC, saved monetary values, counts 1+1+2+64+64=132 coins (660000 total) and retained STONE. Receipt=3 was fully complete although ledger reservation3 was still pending. Log `PAPER_A02_FINAL_SAVED_RECEIPT_CRASH_RECOVERED_WITHOUT_ISSUE_PASSED physicalValue=660000 receipt=3 pendingBeforeAck=1`.
- Without changing player inventory, the seventh JVM ACKed grant3, reopened the independent persistent journal with three delivered and zero pending, then reACKed idempotently while byte-comparing journal record3 and playerdata to the before-ACK values. Log `PAPER_A02_FINAL_SAVED_PRE_ACK_SIGKILL_SEVENTH_BOOT_ACK_ONLY_PASSED durableGrants=3 pending=0 originalMoneyPreserved=true`.
- This is a **disposable, disconnected CraftPlayer probe**, not a production network delivery path; no connected player funds were changed. Real external writer races, full G0 settlement, power loss/fsync, global mutation lock, and normative CAT Confirmed/Gates remain unresolved. Admission stayed default-deny. Final feature SHA will be tested separately after this documentation evidence update.
