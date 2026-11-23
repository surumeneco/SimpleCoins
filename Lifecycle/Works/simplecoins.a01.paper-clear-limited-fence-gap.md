---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-clear-limited-fence-gap
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/615
  - https://github.com/surumeneco/SimpleCoins/pull/629
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a01.craftinventory-removeitem-prebody
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0 — 実Paper CraftInventory.clear()の閉鎖済み限定writer世代下での貨幣変更検証

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/615 |
| https://github.com/surumeneco/SimpleCoins/pull/629 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.settlement.writer-epoch-publication | simplecoins.a01.paper-clear-limited-fence-gap | simplecoins.a01.craftinventory-removeitem-prebody |

## Technologies
| Technology |
| --- |
| Official Paper26.2 build134; Java25 Agent; actual four distributed WCL owner JARs; genuine 5000C chest coins; deny-by-default product admission |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-clear-limited-fence-gap.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryClearClosureProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Bounded observation before any production hook

At current `develop@c753def64bf68c0fc30831979ca8ca48ec105df0`, actual Paper `CraftInventory.setContents`, `addItem` and `removeItem` have been separately classified and their relevant tested entry paths intercepted. An unrelated Bukkit Inventory method, whole-chest `clear()`, has **not** been tested under the same fence. Its internal delegation and mutation ordering are not assumed.

Within a fresh isolated loopback, no-player official Paper26.2 JVM containing the actual WWC/WWW/WCL/WGL JARs, SimpleCoins and independently default-deny Guard, materialize a real chest with two authentic 5000C coins before the fence is armed. Record the runtime owner and exact Java method descriptor of the real `Inventory.clear()` call. Load all seven known instrumented writer families, require PASSIVE, no active entry, test-only arm both product and bootstrap, then seal the shared product `WriterEpochFence`. Invoke the actual zero-argument `chest.clear()` once.

Observe **physical** 5000C coin count and catch only the expected fail-closed `IllegalStateException`: (a) intercepted without mutation, two coins intact; (b) mutation before later hooked refusal, 0–1 coins with both fences FAULTED; or (c) no-throw uncovered mutation, zero coins under unchanged SEALED/LIMITED_TRACKING generation, followed by a same-chest positive-control `setItem` denial and both fences FAULTED. Any missing method, unsafe unexpected inventory contents, wrong count, untransformed Agent, unexpected exception, insufficient fault/positive control, or token mismatch fails CI. Print a disjoint exact classification and stop the disposable JVM without allowing unrelated shutdown saves.

A positive mutation counterexample warrants **separate** confirmed-A01 minimal exact pre-body repair and its own real Paper Red/Green. A no-change interception warrants **no redundant hook**. This classification cannot certify all plugin/Paper/NMS/async/direct-file writers, all 24 JARs, G0 atomic snapshot, G3 durable ACK or Admission. No release, main or VPS change.
