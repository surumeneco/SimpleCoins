---
cat_version: '0.2'
kind: work
id: simplecoins.a01.hopper-native-entry-trace
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/632
  - https://github.com/surumeneco/SimpleCoins/pull/633
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a01.hopper-event-veto-reachability
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01 — Locate actual native Hopper writer entry and source-debit ordering

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/632 |
| https://github.com/surumeneco/SimpleCoins/pull/633 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.settlement.writer-epoch-publication | simplecoins.a01.hopper-native-entry-trace | simplecoins.a01.hopper-event-veto-reachability |

## Technologies
| Technology |
| --- |
| Paper26.2 build134 genuine NMS hopper and event path; Java25 class-loading trace; exact shipped WWC/WWW/WCL/WGL |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.hopper-native-entry-trace.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryHopperTransferClosureProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Bounded native identification, no production hook

Actual Paper 26.2 tests [#632] measured a Hopper automatically transfer authentic 5000C Chest money **despite** a SEALED seven-family product epoch; [#633] showed that cancellation of an actual `InventoryMoveItemEvent` restores the Chest to its initial two coins, but `sourceAtFirstEvent=1`: the event occurs after a temporary debit from the Chest. This invalidates an assumption that the event is a pre-body writer entrance; it does not by itself establish any other writer entrance.

To identify a technically actionable **native** admission point without guessing method names, re-use the same opt-in no-player test's actual Chest/Hopper fixture and real cancellable event. Capture the synchronous Java stack at the **first matched real Chest→Hopper event** using Java 25 `StackWalker` with declaring class/method/line and record the HopperBlockEntity and related NMS code frames before normal event cancellation. Preserve event-time source/destination physical amount evidence and the previous 60-tick final no-transfer test. Require a non-empty relevant native Hopper frame in the stack as a source-derived candidate. This is an execution trace and cannot prove all alternate NMS methods, off-main-thread writers, or other plugins.

Deliver a bounded work item with actual candidate owners/method names and exception/return boundaries to help select one **pre-body writer admission**, not an unscoped transformer. Do not implement the guessed hook in this trace-only Work; a confirmed A01 scoped implementation must separately Red/Green a real sealed-Hopper money path and retain exception/finalization behavior. Product stays PASSIVE, Admission default-deny, G0/G3 publication/ACK and main/VPS release prohibited.
