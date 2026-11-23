---
cat_version: '0.2'
kind: work
id: simplecoins.a01.real-hopper-native-writer-boundary
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/610
  - https://github.com/surumeneco/SimpleCoins/pull/631
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a01.paper-clear-slot-limited-fence-gap
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0 — actual automatic Hopper transfer under a sealed limited writer epoch

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/610 |
| https://github.com/surumeneco/SimpleCoins/pull/631 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.settlement.writer-epoch-publication | simplecoins.a01.real-hopper-native-writer-boundary | simplecoins.a01.paper-clear-slot-limited-fence-gap |

## Technologies
| Technology |
| --- |
| Official Paper26.2 build134; Java25 Agent; genuine shipped WWC/WWW/WCL/WGL; real Chest/Hopper/NMS tick transfer; authentic 5000C coins; deny-by-default admission |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.real-hopper-native-writer-boundary.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryHopperTransferClosureProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Acceptance, safety boundary and stop rule

The configured G0 guarantees a coherent LIVE/SAVED monetary account across the publication generation. Previous actual Paper tests guarded direct Bukkit API `CraftInventory.setItem`, `addItem`, `removeItem`, `clear()` and `clear(int)`. **Automatic Hopper movement during server ticks is not a Bukkit Inventory method call by assumption.** It may directly mutate underlying NMS slots. If so, the location/owner of an authentic item can change while the seven-family product writer fence remains sealed even if the total money value remains constant. This is a much higher-priority independent entry than enumerating further Bukkit aliases. Neither missing evidence nor prior static JAR callrefs proves the hopper's behavior.

Use a fresh disposable official Paper26.2 JVM with all four pinned WWC/WWW/WCL/WGL jars, SimpleCoins, an independently default-deny Guard, zero online players, and loopback-only binding. In a forcibly loaded test chunk, build a live real Chest immediately above a real Hopper with no output container; seed two authentic 5000C coins into the Chest and verify Hopper empty before test-only arm. Resolve and confirm actual block inventories and Hopper enabled state. Require exactly the seven known transformed Agent targets, PASSIVE, zero active entries and DISARMED product. Test-only arm the product and begin the limited family proof, seal and require drained. Then allow normal server ticks (without player interaction or direct inventory mutation) to transfer items from the chest.

After a bounded 60 ticks, count genuine physical money in both actual containers. Validate total coins preserved, and classify only a genuine movement: (a) Chest count dropped and Hopper count increased while product still SEALED and hook still LIMITED_TRACKING (real unobserved native writer); or (b) same physical movement occurred before a later covered hook invalidated both fences (partially guarded but too late); or (c) no movement and the normal Hopper tick path did not reach the transfer, **inconclusive and a failed probe**, not a safe writer guarantee. For an unobserved transfer, call `fence.requireDrained(seal)` before exercising a same-inventory `setItem` positive-control refusal that faults both fences without changing observed money; this rules out a missing Agent. Any unexpected noncurrency items, changed total, not running on main thread, unexpected loss, missing/disabled plugin, or faulty probe state fails the CI. The probe JVM must exit without a normal Paper shutdown save.

This verification performs no production writer interception and cannot authorize G0/G3, release or Admission. If it proves a bypass, record the exact **NMS** writer type and decide a bounded complete strategy under the existing approved A01 contract before adding any generic hooks; repeating a method-at-a-time Bukkit exploration is explicitly excluded. If no transfer is observed under an otherwise correctly ticking Hopper, stop with unproven rather than manufacture a negative or positive result.
