---
cat_version: '0.2'
kind: work
id: simplecoins.a01.native-hopper-pull-prebody
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/632
  - https://github.com/surumeneco/SimpleCoins/pull/633
  - https://github.com/surumeneco/SimpleCoins/pull/634
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.hopper-native-entry-trace
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01/G0 — protect actual NMS Hopper pull before source debit

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/632 |
| https://github.com/surumeneco/SimpleCoins/pull/633 |
| https://github.com/surumeneco/SimpleCoins/pull/634 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.native-hopper-pull-prebody | simplecoins.a01.hopper-native-entry-trace;simplecoins.a01.craftinventory-removeitem-prebody |

## Technologies
| Technology |
| --- |
| Java25 ClassFile Agent; official Paper26.2 build134 NMS HopperBlockEntity; real Chest/Hopper; pinned WWC/WWW/WCL/WGL |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.native-hopper-pull-prebody.md;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/test/java/**;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryHopperTransferClosureProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java | src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/test/java/**;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Confirmed A01 meaning and measured source-dependent implementation

The already Confirmed A01 Process/PI/TCE forbid any new writer entering a sealed G0 snapshot epoch and require all in-flight monetary modifications to drain. Actual official Paper26.2 build134 [#632] proved native automatic Hopper transfer of authentic two 5000C Chest coins into the Hopper under a falsely intact seven-family epoch (Chest 2→0, Hopper 0→2). [#633] showed cancellation of `InventoryMoveItemEvent` eventually restores the source, but at event entry the source was already decremented 2→1. [#634] captured the genuine call stack: `HopperBlockEntity.suckInItems(Level,Hopper)Z → tryTakeInItemFromSlot → hopperPull(Level,Hopper,Container,ItemStack,int)Z → callPullMoveEvent`. Select the narrower actual `hopperPull` entry as the first candidate instead of instrumentation of idle Hopper scans. An entry-stage physical-money test must prove no source debit occurred before this call; if it fails, keep the method unguarded and select an earlier NMS boundary based on that counterexample. The current seven target set is therefore materially incomplete for this measured native path.

**Scope of this independent implementation:** Add one exact NMS `HopperBlockEntity.hopperPull(Level,Hopper,Container,ItemStack,int)Z` entry family for that observed descriptor only. The hook must intercept before the first physical source mutation: if the real Paper test disproves that, this narrow method cannot be merged as a supposed pre-body fix. Use the existing bootstrap-to-product writer-fence binding (same `WriterEpochFence`), counted and auditable as its own exact class/descriptor; preserve all seven old families, their return/throw/in-flight handling and passive startup. Because this NMS method is called from an ordinary server tick, a denied, sealed root must fail closed **without throwing an uncaught exception through the Paper tick loop**: invalidate the current product and Agent epoch, skip the original Hopper source-debit body and return `false`. This is *not* permission to publish G0, open Admission, or silently accept any interrupted write: the epoch is faulted and its published candidate invalid.

TDD: First change only the actual Paper test probe and CI oracle for a separate opt-in native-hook mode, observing an actual enabled Hopper above which a real Chest holds two genuine 5000C coins. Before Agent production change, the test must fail because money still moves Chest 2→0 / Hopper 0→2 under a sealed epoch (**expected Red**). The test must check a known seven-target baseline before the new instrumentation is available only for the Red stage, then require eight exact targets after implementation, the Agent's HOPPER transform marker, and the correct product/bootstrap FAULTED state before/after 60 normal ticks, with physical Chest2/Hopper0 and original no-veto event untouched. For Green additionally check original unrestricted real Hopper movement with Agent PASSIVE, no unexpected server tick crash, original native/PDC/WWC/WWW callbacks, both normal returns and escaped errors in isolated fixture as feasible; all current A01/Domain/Phase6/WCL CI must pass. Update the named seven-family probe roster only as needed to distinguish the historical known-seven test boundary from eight-family running Agent — do not rewrite old negative evidence.

**Known remaining limits:** The hook covers the single observed NMS pull path. Hopper push/eject and NMS direct block-container mutation, plugin direct file writes, asynchronous sources, 24-JAR writer roster, atomic G0 publish and G3 durable ACK remain unproven. Do not arm the product in normal startup; default-deny Guard stays active. No release, VPS, main or U decision. Do not pursue additional method-by-method aliases without identifying a concrete still-reachable monetary loss path.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-hopper-native-model.json | @work/.cat-flow/cache/a01-hopper-native-vectors.json | @work/.cat-flow/cache/a01-hopper-native-queue.json | — | — | @work/.cat-flow/cache/a01-hopper-native-obligations.json | — | false |
