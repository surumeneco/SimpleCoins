---
cat_version: '0.2'
kind: work
id: simplecoins.a05.live-equipment-mirror-once
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

# A05 — LIVE装備のスキャン同時証拠とWCL一回読取からの同一現物・一回計上

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
| simplecoins.external-storage-provider-integration | simplecoins.a05.live-equipment-mirror-once | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.live-equipment-mirror-once.md;src/main/java/co/surumene/simplecoins/paper/PaperLoadedChunkAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperLoadedWorldAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperLiveUniverseAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidates.java;src/main/java/co/surumene/simplecoins/domain/WclHybridLiveOverlay.java;src/main/java/co/surumene/simplecoins/domain/WclLiveEquipmentMirrorProof.java;src/test/java/co/surumene/simplecoins/domain/WclHybridLiveOverlayTest.java;src/test/java/co/surumene/simplecoins/domain/WclLiveEquipmentMirrorProofTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperLoadedChunkAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperLoadedWorldAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperLiveUniverseAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidates.java;src/main/java/co/surumene/simplecoins/domain/WclHybridLiveOverlay.java;src/main/java/co/surumene/simplecoins/domain/WclLiveEquipmentMirrorProof.java | src/test/java/co/surumene/simplecoins/domain/WclHybridLiveOverlayTest.java;src/test/java/co/surumene/simplecoins/domain/WclLiveEquipmentMirrorProofTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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
- User-approved 02A/02B and Confirmed A05 Process/PI/TCE require each physical monetary item to belong to exactly one source and one P/F/D owner. This technical LIVE equipment proof does not change the normative product behavior, global settlement/admission or formally promote its own Draft Work.
- Existing `PaperLoadedChunkAssets.scan` is the authority for loaded Entity equipment monetary `Value`s. Capture defensive serialized bytes *at the exact same original scan*, for positive-value non-Player Entity equipment slots. Propagate the bytes through the existing World/Universe scan to the one-shot `PaperCapturedHybridSettlement` without re-valuing the item, creating a second source, reusing a separate wallet UUID set or loading a chunk. The existing old scanner interfaces and test seams may remain available but cannot fabricate production item evidence.
- For a WCL `readLoaded` Success on an independently selected same-Entity same-World same-Chunk holder, retain the exact immutable WCL StoredItem bytes from this **one** provider read alongside slot values and owner verdict. Positive WWW weapon (`mainhand`) and WWC head (`head`) may mirror vanilla equipment only if the preexisting native slot has positive monetary value, the original scan's exact bytes, the Entity still has an equivalent current slot, and full root ItemStack NBT structural equality holds for original/current/provider payloads, including exact `DataVersion` and components. No amount-only matching, no synthetic claim of proof, no second provider read. If any source vanishes or mutates, reject the whole captured candidate.
- A genuinely proven positive mirror reuses the *existing* `HybridLiveSourceStage.Value`, never adds a new `Value`. `WclHybridLiveOverlay.attach` must reject unproven mirrored content, missing/duplicate/other-Chunk keys, wrong or zero-value positive proof, and mismatched native/WCL amounts. Each identity is a per-Chunk collision key, not a new economic source.
- For a WCL mirror that shares the native slot, the LIVE classification callback must run on the original native Value and produce identical WCL source owner and current BAN/playerdata/everyone classification. In particular no ownerless/free vanilla value may silently replace a WCL commander/owner: fail on disagreement. This is a local classification check, NOT a proof of all-source completion or writer freeze. WCL independent cargo/shulker still uses its distinct WCL classification.
- Wallet same-ID in the WCL mirror and earlier vanilla inventory remains fail-closed (shared Set duplicate), not auto-coalesced. This Work covers ordinary ItemStack coins, not wallet mirror automatic deduplication, all-WCL discovery, every Entity writer exclusion, or atomic publication.
- Unit tests: valid WWW/WWC positive mirrored amount and matching keys only once; absent/forged proof, value mismatch, wrong chunk/provider/slot, owners disagree, zero-valued proof, duplicate source fail. Verify bounded full-NBT equality for serialized bytes including components and nested map/list/arrays; altered count/PDC/data version fails.
- Official Paper 26.2 integration with shipped pinned WCL/WWC/WWW: spawn disposable genuine loaded WWW Wolf with both a real Paper mainhand coin and WCL weapon ItemStack for that same coin plus 5,000C independent cargo, valid owner/commander; run the real public `capture → includeLoadedWcl → stageCandidate` with the same shared wallet generation and verify exactly 15,000C and P/F/D classification. Native vs WCL mismatch must fail, never be a separate asset. No deployment.
- Formal A05 vectors/Queue/generated Tests/Implementation Conformance, G0 writer exclusion, all Worlds, atomic public cache, G3 durable ACK, JE/BE user acceptance and Admission remain NOT guaranteed and fail-closed.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-live-equipment-mirror-model.json | @work/.cat-flow/cache/a05-live-equipment-mirror-vectors.json | @work/.cat-flow/cache/a05-live-equipment-mirror-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05LiveMirrorGeneratedTest.java | @work/.cat-flow/cache/a05-live-equipment-mirror-obligations.json | — | false |

## Formal boundary
Existing Confirmed s0–s7 describe local verdicts. This independent technical bridge cannot prove external WCL observation, all-writer epoch, global identity, ownership BAN/playerdata, or the implementation of the complete Process. A Java/JUnit Green must not be relabeled as formal implementation-conformance passed.
