---
cat_version: '0.2'
kind: work
id: simplecoins.a05.saved-wcl-source-stage
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A05 — 非LIVEのSAVED Entityを同一走査コールバックでWCL収納候補化する

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-pi.md |
| https://github.com/surumeneco/SimpleCoins/pull/548 |
| https://github.com/surumeneco/SimpleCoins/pull/549 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.saved-wcl-source-stage | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.saved-wcl-source-stage.md;src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java;src/main/java/co/surumene/simplecoins/domain/WclSavedSlotSupplement.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/test/java/co/surumene/simplecoins/domain/WclSavedSlotSupplementTest.java;src/test/java/co/surumene/simplecoins/domain/SettlementScanRunnerSavedSupplementTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java;src/main/java/co/surumene/simplecoins/domain/WclSavedSlotSupplement.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java | src/test/java/co/surumene/simplecoins/domain/WclSavedSlotSupplementTest.java;src/test/java/co/surumene/simplecoins/domain/SettlementScanRunnerSavedSupplementTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Confirmed A05 source and bounded acceptance
- 02A/02B and the user-confirmed A05 Process/PI/TCE remain the only normative sources. This Work adds a local technical staging adapter; it does NOT change user-approved ownership, accounting, completeness or publication semantics, and no Draft is promoted to Confirmed.
- The existing SettlementScanRunner MUST read all saved Anvil source data and use SavedLiveSourcePartition to select LIVE vs SAVED **before** invoking saved Entity WCL supplementation. A selected SAVED Entity chunk supplement executes in the **same chunk callback**, before source completion, and shares exactly the existing mutable wallet UUID context and pending SettlementAccumulator; no second monetary read or new saved source ID is invented.
- The extra SAVED Entity callback must be optional and preserve all existing stage overloads and no-WCL behavior. Explicit duplicate/malformed/unreadable WCL saved source must invalidate the entire pending generation; a saved chunk shadowed by captured LIVE may never feed a SAVED callback. Existing SavedContentWitness retains source-drift detection.
- For each genuine WCL readStored Success, accept only exact source world/Entity UUID/provider/Chunk identities; reuse the existing PaperWonderfulSavedChunkCandidates source/read code and its saved vanilla tame-owner evidence, with one shared seenEntityIds set across all saved chunks and one shared wallet set across live+saved. The caller provides an explicitly verified real World UUID mapping; missing or conflicting mapping is a hard failure, not global proof of every world identity.
- Attach strictly positive independent saved WCL slot values into the existing saved ENTITY Anvil region file source using the canonical WclPhysicalSlotIdentity alias. Reject a positive WWW weapon / WWC head item because it can mirror vanilla mainhand/head unless item-level equivalence is independently proven; a duplicate physical key including an already staged entity/display item rejects the accumulator. Zero slots are not inferred absent without successful WCL read.
- Owner decision HOLD is a hard source failure; recognized OWNER maps to personal P if current non-BAN and playerdata exists, otherwise D; FREE goes to F. Continue to use current BAN coverage or the caller-supplied explicit ban set and the SAME manifest playerdata list; do not treat unspecified BAN providers as evidence of complete global BAN.
- The saved provider read must follow its existing Paper primary-thread contract; this work does not introduce asynchronous Bukkit API access. It is not an all-world traversal invocation, no source/writer freeze or local commit/publish.
- JUnit: saved WCL shulker=10,000C staged once under existing source; positive weapon/head fail, same physical key duplicate fails, unknown owner holds; the saved Entity callback only sees SAVED not shadowed LIVE chunks. Pinned real Paper source fixture: independently selected saved WWC 10,000C is handled, and saved WWW 10,000C weapon is rejected rather than counted twice. Run the existing full workflow.
- Exclusions: global G0 generation/writer static, source owner mapping for unloaded worlds, saved WCL complete release, global JE/BE use, G3 ACK, Admission; retain all default-deny and no-publication conditions. Formally passing Process Closure/Model Conformance does not satisfy A05 Implementation Conformance.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-saved-wcl-source-model.json | @work/.cat-flow/cache/a05-saved-wcl-source-vectors.json | @work/.cat-flow/cache/a05-saved-wcl-source-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05SavedWclSourceGeneratedTest.java | @work/.cat-flow/cache/a05-saved-wcl-source-obligations.json | — | false |

## Formal boundary
Existing Confirmed s0–s7 describe local verdicts. This independent technical bridge cannot prove external WCL observation, all-writer epoch, global identity, ownership BAN/playerdata, or the implementation of the complete Process. A Java/JUnit Green must not be relabeled as formal implementation-conformance passed.
