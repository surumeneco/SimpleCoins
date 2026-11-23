---
cat_version: '0.2'
kind: work
id: simplecoins.a01.hopper-event-veto-reachability
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/632
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a01.real-hopper-native-writer-boundary
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0 — actual Paper InventoryMoveItemEvent veto against NMS Hopper bypass

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/632 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.settlement.writer-epoch-publication | simplecoins.a01.hopper-event-veto-reachability | simplecoins.a01.real-hopper-native-writer-boundary |

## Technologies
| Technology |
| --- |
| Paper26.2 build134; InventoryMoveItemEvent; Java25 Agent; actual shipped WWC/WWW/WCL/WGL; real Chest and Hopper; authentic 5000C coins |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.hopper-event-veto-reachability.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryHopperTransferClosureProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Bounded evidence contract and exclusions

PR #632 proves the exact real Paper Hopper tick changes authentic money source from Chest 2/Hopper 0 to Chest 0/Hopper 2 while the seven-known-class product `WriterEpochFence` remains SEALED and the Agent remains LIMITED_TRACKING. This does not identify every native writer. A plausible **local** technical countermeasure is a cancellable pre-move `InventoryMoveItemEvent` for automatic Hopper transfer, but the event's actual timing, cancellation semantics, and delivery with this real Paper config must be demonstrated, not inferred from API names.

Add an independent **test-only**, opt-in JVM mode on the same genuine Paper/Plugin/Guard/Agent and two authentic 5000C Chest coins above an enabled Hopper, isolated loopback with no online players. Before test-only arm+seal, register an event listener at LOWEST that matches only the test Chest→Hopper pair, counts legitimate events, reads source/destination physical money *at the event*, records the event coin identity, and cancels. After 60 real server ticks, require that matching move event fired, record counts/bridge states, and classify:

- Source still 2, Hopper 0, both epoch states unchanged: event cancellation successfully prevented the final physical money transfer for this exact path. If the observed source was already debited at event time but restored by cancellation, report this distinction rather than claiming a pre-body veto.
- Source or Hopper money changed despite cancellation: the event did not prevent the monetary write, and an independent strategy or deeper native entry is required.
- No matching event, no tick effect, unexpected items, missing loaded Agent/Guard, total mismatch, or unrelated callback: fail the proof, never convert to a success.

A positive control must exercise the existing protected setter after the test and confirm the product and bootstrap fences fault without changing the physical amounts. No production listener or generalized guarantee is authorized by this test-only event, and even a successful local veto does not cover direct NMS calls, other Plugin writers, hopper event suppression configs, asynchronous/file writers, G0 atomic publication, G3 durable ACK, main/VPS or release. Keep product passive and Admission default-deny.
