---
cat_version: '0.2'
kind: work
id: simplecoins.a01.craftinventory-additem-prebody
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/626
  - https://github.com/surumeneco/SimpleCoins/actions/runs/45059782742
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.paper-additem-limited-fence-gap
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 — CraftInventory.addItemの変更本体より前に同一writer世代へ入場させる

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/626 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/45059782742 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.craftinventory-additem-prebody | simplecoins.a01.writer-exception-failclosed;simplecoins.a01.paper-additem-limited-fence-gap |

## Technologies
| Technology |
| --- |
| Java25 ClassFile API; Paper26.2 build134; exact `CraftInventory.addItem` Java method; Java25 fixture and genuine Paper+WWC/WWW CI |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.craftinventory-additem-prebody.md;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/test/java/co/surumene/simplecoins/writeragent/AgentBridgeIsolationMain.java;writer-fence-agent/src/test/java/org/bukkit/craftbukkit/inventory/CraftInventory.java;.github/workflows/a01-native-save-agent-bridge.yml | writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java | writer-fence-agent/src/test/java/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Confirmed contract and exact observed technical gap

Existing confirmed A01 Process/PI/TCE require that protected accounting generations have an actual no-new-writer interval. **Positive external evidence**, not hypothetical risk: official Paper26.2 build134 in the #626 real no-player test took an authentic 5000C Chest coin from **1 to 2** after the existing seven-family product fence was sealed. `CraftInventory.addItem` threw when it later reached the already-intercepted `CraftInventory.setItem` (stack frame CraftInventory.java:332), and both bootstrap/product fences correctly became FAULTED, but the earlier **physical coin change persisted**. The actual method owner and descriptor were recorded via reflection:
`org.bukkit.craftbukkit.inventory.CraftInventory.addItem([Lorg/bukkit/inventory/ItemStack;)Ljava/util/HashMap;`.

This is a concrete violation of the intended **pre-body writer exclusion** if someone treated the currently limited fence as a complete barrier. The required correction is an exact ClassFile rewrite for that `addItem` signature within the **existing INVENTORY family**, with the same bootstrap and same product `WriterEpochFence`, both before body and on normal/exceptional return. **Do not** add a separate coordinator or apply source-level catch after mutation. A successful non-void `HashMap` return must be preserved verbatim; an original thrown Throwable must remain its original exception object and invalidate the product epoch. At a sealed epoch, calling `addItem` must fail **before any inventory change**, leaving one real coin. This must be proven with the real Paper test after the earlier negative test, not inferred from synthetic fixture only. Do not register new source completeness, G0 or G3 success from local entry coverage.

TDD: Add a Java25 JVM fixture `CraftInventory.addItem` with a method-body execution counter to the already loaded writer class. First demand a **sealed epoch refusal without entering its method body** while the old Agent only instruments `setItem`, and observe expected-reason Red. Then add exactly one identified `addItem` descriptor to the INVENTORY target (expect exactly two methods in that class), rerun fixture Green including open tracked `addItem` normal-return drain and original return type. Finally use [#626]'s actual Paper `addItem` same-fence probe and require `INTERCEPTED count=1` instead of the original `PARTIAL_MUTATION` outcome. Existing WWC/WWW, Paper native, PDC, mainhand, abnormal-exit and guard regressions must stay Green. Main/VPS/release/Admission unchanged, no new spec semantics.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-additem-model.json | @work/.cat-flow/cache/a01-additem-vectors.json | @work/.cat-flow/cache/a01-additem-queue.json | — | — | @work/.cat-flow/cache/a01-additem-obligations.json | — | false |
