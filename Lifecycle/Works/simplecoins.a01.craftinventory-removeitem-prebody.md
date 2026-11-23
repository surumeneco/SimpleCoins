---
cat_version: '0.2'
kind: work
id: simplecoins.a01.craftinventory-removeitem-prebody
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/628
  - https://github.com/surumeneco/SimpleCoins/actions/runs/45061845790
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.paper-removeitem-limited-fence-gap
  - simplecoins.a01.craftinventory-additem-prebody
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 — CraftInventory.removeItemの変更本体より前に同一writer世代へ入場させる

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/628 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/45061845790 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.craftinventory-removeitem-prebody | simplecoins.a01.craftinventory-additem-prebody;simplecoins.a01.paper-removeitem-limited-fence-gap |

## Technologies
| Technology |
| --- |
| Java25 ClassFile API; Paper26.2 build134; exact `CraftInventory.removeItem` method; genuine Paper+WWC/WWW CI |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.craftinventory-removeitem-prebody.md;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/test/java/co/surumene/simplecoins/writeragent/AgentBridgeIsolationMain.java;writer-fence-agent/src/test/java/org/bukkit/craftbukkit/inventory/CraftInventory.java;.github/workflows/a01-native-save-agent-bridge.yml | writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java | writer-fence-agent/src/test/java/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Confirmed contract and measured breach

Confirmed A01 Process/PI/TCE require that a sealed protected accounting generation admit no new writer and remain coherent while writers drain. PR #628 ran the authentic Paper26.2 build134 method `org.bukkit.craftbukkit.inventory.CraftInventory.removeItem([Lorg/bukkit/inventory/ItemStack;)Ljava/util/HashMap;` in an isolated, seven-family instrumented Java25+WWC/WWW/WCL/WGL JVM. Genuine 5000C money in the chest physically changed from **two coins to one** after the product fence was sealed, before a lower-level `CraftInventory.setItem` hook rejected a later write. The bootstrap and product fences then became FAULTED. The existing hook is therefore too late to prevent the first monetary side effect. This is observed, not a hypothetical method name.

Within the already user-approved A01 boundary, instrument exactly that `removeItem` descriptor **before body entry**, reusing the current INVENTORY bootstrap callbacks and same product `WriterEpochFence`. The existing `setItem` and `addItem` instrumentation, nested synchronous operation handling, original non-void `HashMap` return and original escaping Throwable identity/fail-closed behavior must remain intact. An additional coordinator, opening admission, a source-coverage claim or changed G0/G3 semantics is excluded.

TDD: First change the real Paper CI oracle to demand `PAPER_A01_REAL_CHEST_REMOVEITEM_INTERCEPTED count=2` and to fail on `PARTIAL_MUTATION` or `BYPASS`, observing expected-reason Red against the current production Agent. Then add only the exact `CraftInventory.removeItem` signature to the existing INVENTORY family (expected three exact transformed methods), rerun that **same** real Paper probe Green with two coins unmodified and confirmed Agent/product fault, plus the existing Java25 synthetic, Paper native, owner writer, Domain and Phase6 regressions. The product must remain PASSIVE; all seven known families are only a bounded proof, never a 24-JAR closure proof. CAT Implementation Conformance, G0 atomic publication, G3 durable ACK and Admission remain separately unproven. No main, VPS, production release or new normative decision.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-removeitem-model.json | @work/.cat-flow/cache/a01-removeitem-vectors.json | @work/.cat-flow/cache/a01-removeitem-queue.json | — | — | @work/.cat-flow/cache/a01-removeitem-obligations.json | — | false |
