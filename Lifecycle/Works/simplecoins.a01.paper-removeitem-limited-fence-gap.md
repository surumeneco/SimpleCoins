---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-removeitem-limited-fence-gap
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/626
  - https://github.com/surumeneco/SimpleCoins/pull/627
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a01.craftinventory-additem-prebody
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0 — 実Paper Chest Inventory.removeItemの閉鎖済み製品writer世代下での可観測性

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/626 |
| https://github.com/surumeneco/SimpleCoins/pull/627 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.settlement.writer-epoch-publication | simplecoins.a01.paper-removeitem-limited-fence-gap | simplecoins.a01.craftinventory-additem-prebody |

## Technologies
| Technology |
| --- |
| Official Paper 26.2 build134; Java25 Agent; actual shipped WWC/WWW/WCL/WGL; real SimpleCoins 5000C chest coin; default-deny admission |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-removeitem-limited-fence-gap.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryRemoveItemClosureProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Bounded investigation and decision boundary

Actual official Paper26.2 under a fully sealed **known-limited-family** product fence already demonstrated a LIVE 5000C chest coin Inventory.addItem mutation before lower-level hooked setItem refusal [#626]. The precise CraftInventory.addItem body has now been hooked at entry [#627]. A different Bukkit method Inventory.removeItem may or may not change a live currency holder before a covered writer; this is **not yet observed**. It shall not be patched on speculation.

In a fresh disposable isolated no-player Paper26.2 JVM with shipped WCL/WGL/WWC/WWW, product and independent admission guard, and genuine Java25 Agent, create one actual Chest with two authentic 5000C coins while the product fence is passive. Record runtime CraftInventory.removeItem owner and JVM descriptor. Require exactly the existing seven Agent target families to be transformed, verify zero in-flight, arm **only within this probe**, begin limited tracking, and seal the single product WriterEpochFence. Call real Chest removeItem requesting one 5000C coin while sealed.

Classify actual observable results independently: **intercepted before physical change** (two coins intact, Hook/product fault), **partial physical mutation before a later hook refused** (one coin remains, Hook/product fault), or **uncovered no-throw write** (one coin remains with Hook/product still sealed). A no-throw case must additionally prove the same inventory's known setItem is blocked and product becomes FAULTED after the physical change, not a missing Agent. Unexpected money counts, skipped transform, no expected fault, missing provider, removeItem mismatch, or an effect unsupported by verified currency contents fails CI. Stop the temporary process immediately after the test without unrelated shutdown saves.

An actual partial/bypass result warrants a **separate** approved-A01 minimal pre-body entry implementation and exact Java25 + real Paper Red/Green. A clean no-effect interception warrants **no duplicate hook**. Neither classification certifies the 24-JAR universe, G0 public atomic settlement, G3 durable ACK, nor admission. Product startup never arms an epoch; no main, VPS or release changes.
