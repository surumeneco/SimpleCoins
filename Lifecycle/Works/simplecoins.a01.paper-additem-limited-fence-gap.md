---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-additem-limited-fence-gap
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/625
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

# A01/G0 — 実Paper Chest Inventory.addItemの既知7 writer制御境界

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/624 |
| https://github.com/surumeneco/SimpleCoins/pull/625 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.settlement.writer-epoch-publication | simplecoins.a01.paper-additem-limited-fence-gap | simplecoins.a01.mainhand-equipment-agent-writer;simplecoins.a01.writer-exception-failclosed |

## Technologies
| Technology |
| --- |
| Official Paper26.2 build134; Java25 startup Agent; exact shipped WCL/WGL/WWC/WWW; authentic CoinItemCodec; default-deny admission |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-additem-limited-fence-gap.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryAddItemClosureProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Technical hypothesis, method and stop criterion

The adopted 02B/G0 requirement is a coherent monetary accounting generation including loaded inventories. Existing 7-family product Agent is not the complete writer universe. `org.bukkit.inventory.Inventory.addItem` is an observed callable mutation API but **not itself** one of the Agent's exact methods; whether actual Paper implementation delegates to a covered `CraftInventory.setItem` is **unverified**. Do not infer an actual mutation bypass from API surface alone or add another Agent hook without proof.

Use a **single isolated no-player real Paper 26.2 JVM** with the exact packaged WWC/WWW/WCL/WGL JARs, existing SimpleCoins/independent default-deny guard and Java25 Agent, no VPS or production change. Prepare and validate one genuine 5000C Paper Chest coin and a second identical addItem stack **while Agent is PASSIVE**. Load the known seven exact target classes by their real class loaders and check `WriterEpochHook.instrumentedTargets` is exactly that set and no calls are in flight. Only inside the CI probe (never in SimpleCoinsPlugin), reflect the product's existing `WriterEpochFence`, arm it, enable `beginLimitedFamilyProof`, seal it, and immediately invoke the real Chest `Inventory.addItem` on Paper main thread. Determine whether the authentic coin inventory mutates from one to two while known-seven admission remains closed; if the call is intercepted and rejected, prove original count remains one. A call may also mutate LIVE inventory **before** delegating to an instrumented method that rejects; this partial-write-then-throw outcome is an independently significant uncovered entry. A no-throw bypass case requires a **positive control**: after the bypass, the same Chest's `Inventory.setItem` must be rejected by the already instrumented hook and fault the same product generation, without writing a third coin. This distinguishes absent instrumented entry from a non-running Agent.

Use a strictly bounded one-tick test; the probe emits an explicit `BYPASS_OBSERVED`, `PARTIAL_MUTATION_BEFORE_COVERED_HOOK_REFUSAL`, or `INTERCEPTED` marker based only on actual results, plus observable bootstrap/product states and guard conditions, then ends the disposable JVM without running unprotected shutdown saves. Missing class transform, unexpected exception/state, changed monetary content without the specified ordering or failed positive control is a test failure. The check may return either legitimate observed result; do not assert a bypass beforehand. A real **bypass or partial-before-refusal** justifies a subsequent Confirmed-A01 minimal hook Work to close the concrete pre-body entry before monetary mutation, not all additional inventory APIs; a fully **intercepted, unchanged** result means do not add a duplicate hook. Faulting a generation after a partial physical write is not a successful admission denial. Keep CAT as `issue-work`, not a false Process/Model/Implementation Gate. Neither outcome proves 24-JAR completeness, G0 publish, G3 ACK, admission or VPS behavior.
