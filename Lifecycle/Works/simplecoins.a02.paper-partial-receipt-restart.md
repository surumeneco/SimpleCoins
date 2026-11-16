---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-partial-receipt-restart
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/466
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

# G3/A02: 部分現物配布の実Paper保存・再起動・最終ACK検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/466 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantDeliveryBatchPlanner.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-partial-receipt-restart | simplecoins.a02.paper-real-plan-save-readback;simplecoins.a02.after-ack-crash-replay-verification |

## Technologies

| Technology |
| --- |
| Paper 26.2 build 134; Java 25; 6 independent Paper JVM sessions; real playerdata and fixed product grant journal |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-partial-receipt-restart.md;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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
| ci | passed | [real Paper SIGKILL #43859884378](https://github.com/surumeneco/SimpleCoins/actions/runs/43859884378); [Domain #43859884403](https://github.com/surumeneco/SimpleCoins/actions/runs/43859884403); [Phase6 #43859884470](https://github.com/surumeneco/SimpleCoins/actions/runs/43859884470), tested feature HEAD `aece73ccd18d9df5bf31910242c4f0ce4739fa56`. Ordinary CI only, not normative CAT gates. |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Bounded contract and acceptance

The adopted 03/02B specification says that a reservation may be split according to actual free inventory capacity, with accumulated partially delivered value and exact reservation identity saved in the same Paper playerdata as physical currency. Full storage does not consume the reservation; journal ACK is forbidden until full physical progress is durably read back. Restart must resume the exact canonical undelivered stack prefix without duplicating the previous delivery.

Preserve the four existing independent JVM stages of the real Paper #466 test unchanged. In fifth Paper JVM, the already completed two original grants/coins must remain. Create 34 occupied/2 free storage slots without destroying original coin and unrelated stored stone, save the precondition to disk, and reserve exactly 130 actual 5000-denomination coins (650000 value) as new grant #3. The existing planner should allocate exactly two canonical stacks of 64 to the only free slots, with next PDC receipt active on reservation 3 and partialValue=640000. Persist real Paper inventory and receipt; verify same-file saved transition and explicitly reject ACK while partial. Inventory must be full after this batch. No online player or actual server participant.

After the fifth process has read back the real partial receipt and confirmed the ACK remains pending, **forcibly SIGKILL the Paper JVM before clean server shutdown** and require exit status 137; preserve its on-disk playerdata and original journal. In a sixth independent JVM, load that partial physical inventory and PDC from the real persisted shared playerdata root, check product startup audit did not fail, remove one disposable nonmonetary filler stack (simulated disposal but no game events) to create exactly one vacant slot, and persist this changed baseline. From restored active receipt3, stage the remaining final 2-coin stack and physically save the inventory plus completed receipt3 to one playerdata generation. Compare the actual pre/post saved roots with production file readback before ever calling ledger ACK; verify exactly one new 10000 value stack, both earlier 64-coin stacks and all remaining original/other storage retained. Only then ACK grant3. Independently reopen journal and ensure exactly 3 delivered, no pending. Full-inventory retry before removing filler must remain unmodified and pending. Never claim live-player delivery or OS power-loss durability.

This is an independent `issue-work` verification only. All CAT Process/PI/TCE Drafts remain Draft, normative Gates not-run. The existing default-deny is maintained. This is NOT a real grant coordinator, release permission, or completed G3.

## Observed execution evidence and retained limits

- Tested six real official Paper 26.2 JVM processes from exact feature HEAD `26e49f75142df67547e923a548b9ab0e0bb921a4`; existing first-four #466 crash/ACK tests retained. [Real Paper run #43854639964](https://github.com/surumeneco/SimpleCoins/actions/runs/43854639964) completed success; `PAPER_A02_SAVE_CRASH_ACK_CRASH_THIRD_BOOT_NO_REISSUE_PASSED` and `PAPER_A02_REAL_PLAN_SAVE_PROOF_ACK_REAL_PAPER_PASSED` remain present.
- Fifth independent Paper boot: `PAPER_A02_PARTIAL_640000_SAVED_NO_ACK_PASSED activeSequence=3 completed=2 slots=2`. Two genuine 5000-value 64-item stacks are saved in Paper `players/data/<UUID>.dat`, receipt contains `partialValue=640000`, no slot remains, journal grant3 stays pending. Premature `ledger.acknowledge(grant, receipt.completedSequence())` rejects the incomplete grant.
- Sixth independent Paper boot: `PAPER_A02_PARTIAL_RESTART_FULL_STORAGE_NO_DUPLICATE_PASSED`; after safely deleting only one synthetic nonmonetary filler (not real user inventory), `PAPER_A02_PARTIAL_RESTART_FINAL_SAVED_PROOF_PASSED newValue=10000 completed=3`. The canonical remaining two-coin stack was physically saved and verified against actual file transitions before any ACK. `PAPER_A02_PARTIAL_TWO_JVM_EXACT_STACK_PREFIX_ACK_PASSED delivered=3 pending=0`; fresh ledger reopen confirms all reservations delivered.
- Domain [run #43854639992](https://github.com/surumeneco/SimpleCoins/actions/runs/43854639992) first attempt failed while fetching `io.papermc.paper:paper-api:26.2.build.134-stable` from official Maven (HTTP 503). The exact failed jobs were re-run without production or test code changes; attempt 2 completed success. Phase6 [#43854639936](https://github.com/surumeneco/SimpleCoins/actions/runs/43854639936) also succeeded.
- This **isolated verifier** deliberately operates on synthetic detached NMS `CraftPlayer` and real product ledger in a disposable Paper server. Production's login/physical delivery service remains *unwired*, admission default-deny, G0 settlement incomplete. This does NOT prove actual player concurrency, cross-process lock, arbitrary inventory drift, OS power-fail/fsync, or complete G3 exactly-once. CAT Process/PI/TCE and this Work remain Draft, formal normative Gates not-run; Work structural validation is not a gate pass.

## Crash boundary strengthened after initial clean-restart Green

The first 6-JVM success on #43854639964 proved correct partial cursor recovery across a clean Paper restart, not from an abruptly terminated partial-write session. The fifth JVM is now required to log its verified partial on-disk 640000 receipt and *still-pending* journal, remain running, signal `PAPER_A02_PARTIAL_SAVED_BEFORE_ACK_CRASH_POINT_READY`, then receive OS SIGKILL with observed process status 137 **before** any Paper shutdown hooks. The sixth JVM's existing assertions must still succeed using the same files. The exact crash-specific run #43859884378 completed success: the fifth process waited for the verified on-disk partial receipt and emitted `PAPER_A02_PARTIAL_SAVED_BEFORE_ACK_CRASH_POINT_READY`, received SIGKILL (observed process exit 137 and `PAPER_A02_PARTIAL_BEFORE_ACK_SIGKILL_INJECTED`), and a sixth independent process logged `PAPER_A02_PARTIAL_RESTART_FULL_STORAGE_NO_DUPLICATE_PASSED`, `PAPER_A02_PARTIAL_RESTART_FINAL_SAVED_PROOF_PASSED newValue=10000 completed=3`, and `PAPER_A02_PARTIAL_TWO_JVM_EXACT_STACK_PREFIX_ACK_PASSED delivered=3 pending=0`. The original clean-stop #43854639964 and strengthened SIGKILL versions are distinct evidence.
