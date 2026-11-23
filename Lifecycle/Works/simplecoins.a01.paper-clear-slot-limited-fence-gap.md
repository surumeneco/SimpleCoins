---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-clear-slot-limited-fence-gap
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/630
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a01.paper-clear-limited-fence-gap
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01 — Confirm actual Paper Chest clear(int) under a sealed limited writer epoch

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/630 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.settlement.writer-epoch-publication | simplecoins.a01.paper-clear-slot-limited-fence-gap | simplecoins.a01.paper-clear-limited-fence-gap |

## Technologies
| Technology |
| --- |
| Official Paper26.2 build134; Java25 native Agent; genuine shipped WWC/WWW/WCL/WGL JARs; 5000C real chest money; default-deny admission |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-clear-slot-limited-fence-gap.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryClearSlotClosureProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Bounded observation, not a speculative production hook

Confirmed A01 protects the same monetary writer epoch from ingress after seal until an atomic snapshot is published. The prior actual Paper test [PR #630] established that the zero-argument `CraftInventory.clear()V` delegates to the already-fenced `setItem` and cannot clear real 5000C money when sealed. It **did not test** the distinct `clear(int)` signature. That single-slot operation is a directly reachable physical inventory write that could alter a full coin stack before the known lower-level hook runs, or could be correctly blocked. Do not assume either result.

In one disposable official Paper26.2 server with the actual shipped WCL/WGL/WWC/WWW jars, independently default-deny Guard, no players and loopback binding, create a real Chest with two authentic 5000C coins in slot zero *before* test-only activation of the known seven writer families. Confirm actual `CraftInventory.clear(int)` owner and descriptor by reflection; require PASSIVE product Agent, zero in-flight and exactly seven transformed target families. Only in this isolated test arm and seal the product `WriterEpochFence`, then execute `chest.clear(0)` directly.

Read the **real physical** chest again. Distinguish (1) pre-mutation refusal with both genuine coins intact and both fences faulted, (2) a rejected operation that already changed physical coins (one or zero coins; both fences faulted), or (3) a mutation without a thrown exception under an apparently unchanged sealed fence, where a covered `setItem` positive control must be rejected and fault the same bridge. Unknown inventory contents, a non-faulted refusal, any different inventory count, absent method, incorrect loaded class or Agent, or untrusted plugin fail the test. The server must exit immediately without unrelated shutdown saves and preserve logs.

Only a **measured** breach justifies an independently scoped confirmed-A01 pre-body implementation and expected-reason Red→Green. A protected delegation requires no new interception; verify it and terminate this bounded investigation. Neither result establishes the complete 24-JAR writer universe, G0 atomic publication, G3 durable ACK or Admission. No main/VPS/release changes.
