---
cat_version: '0.2'
kind: work
id: simplecoins.a05.loaded-wcl-entity-discovery
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

# A05 — 捕捉済みLIVEチャンクの全個体列挙とWWC/WWW収納の自動候補選択

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
| simplecoins.external-storage-provider-integration | simplecoins.a05.loaded-wcl-entity-discovery | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.loaded-wcl-entity-discovery.md;src/main/java/co/surumene/simplecoins/domain/WclLiveMarker.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveEntityDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/test/java/co/surumene/simplecoins/domain/WclLiveMarkerTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/WclLiveMarker.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveEntityDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java | src/test/java/co/surumene/simplecoins/domain/WclLiveMarkerTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Approved A05 source and bounded acceptance
- Normative basis is the approved 02A/02B and Confirmed A05 Process/PI/TCE; only an **unpublished** source discovery + overlay handoff is in scope. This Work does not revise any user-approved classification, generation, deduplication or publish rule.
- In the existing `PaperCapturedHybridSettlement.capture` path, remember the exact selected Paper World objects/UUID identities and the already bounded LIVE scan resource budget. Never use a world name alone as proof of identity. Test-only `fromObserved` lacks this runtime identity and must not be able to assert full Paper discovery.
- On Paper primary thread, enumerate **all entities in exactly the selected currently loaded Chunks** without causing a new Chunk load. Independently compare the loaded Chunk coordinate sets with the captured ChunkSource set before and after the enumeration. Enforce a total entity budget including untagged entities, not only discovered WCL holders. Reject duplicate Entity UUIDs, invalid/moving Entity identity, wrong World and missing selected Chunk; do not treat a scan limit as an empty source.
- Use the same PDC **namespace-prefix + `type` + canonical species** identity rule as the existing SAVED WWC/WWW Entity candidate collector. A partially marked, mistyped or simultaneously WWC/WWW marked Entity must fail closed, even if a corresponding provider returns NotFound. Untagged vanilla Entity is outside this specific WCL marker set, not a globally proven WCL absence.
- Feed each positively recognized holder through the existing single-read `PaperWonderfulLiveCandidates.collect` and `PaperCapturedHybridSettlement.includeLoadedWcl` while reusing the **same shared wallet UUID set**. Re-scan the selected loaded marker identities after all reads; added, missing, relocated or retagged WCL holders require the capture generation to be discarded. Return a bounded diagnostic count; `isCompleteSettlementSource` remains false.
- Explicit per-Entity enrollment and automatically enumerating all selected loaded chunks may **not mix** in one candidate generation. Any failing enumeration/read/owner/identity condition consumes and invalidates the entire pending capture. The existing explicit selected-Entity path must retain its current behavior.
- JUnit Red/Green: unique WWC and WWW markers, ordinary vanilla, wrong species/type, mixed namespaces, incomplete markers, unrelated prefix. Real pinned Paper 26.2: known loaded WWC with 10000C is detected without an explicit Entity argument, handoff actually stages the amount in the existing unpublished Hybrid candidate; the relevant positive marker is asserted in initial, persisted, restarted probe runs.
- This does **not** prove all-world selection, unseen/non-PDC WCL entities, LIVE vs SAVED epoch, source/writer freeze, changed monetary ItemStacks, complete BAN/playerdata, global slot identity, atomic publication, G3 ACK, login admission or 1.0.0 release.
- CAT Process Closure/Model Conformance only as actually executed; full Implementation Conformance remains inconclusive/not-run unless source-generation and publisher obligations are demonstrably satisfied.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-loaded-wcl-discovery-model.json | @work/.cat-flow/cache/a05-loaded-wcl-discovery-vectors.json | @work/.cat-flow/cache/a05-loaded-wcl-discovery-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05LoadedWclDiscoveryGeneratedTest.java | @work/.cat-flow/cache/a05-loaded-wcl-discovery-obligations.json | — | false |

## Formal boundary
Existing Confirmed s0–s7 describe local verdicts. This independent technical bridge cannot prove external WCL observation, all-writer epoch, global identity, ownership BAN/playerdata, or the implementation of the complete Process. A Java/JUnit Green must not be relabeled as formal implementation-conformance passed.
