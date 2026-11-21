---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-terrain-observation-producer
status: draft
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-20:A04-saved-BlockLocker-CAT-approved
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

# A04 — 選択されたSAVED地形全体からのBlockLocker収納観測候補生成

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/562 |
| https://github.com/surumeneco/SimpleCoins/pull/574 |
| https://github.com/surumeneco/SimpleCoins/pull/577 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-terrain-observation-producer | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit; official Paper 26.2 + BlockLocker 1.15 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-terrain-observation-producer.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservations.java;src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerPhysicalOwnerIndex.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservationsTest.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerPhysicalOwnerIndexTest.java | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservations.java;src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerPhysicalOwnerIndex.java | src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservationsTest.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerPhysicalOwnerIndexTest.java |

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

## Acceptance and bounded scope
- Implement a **read-only producer** that uses a real discovered Manifest and the existing complete `WorldSnapshotTraversal.traverseVerified` source traversal. Collect selected SAVED REGION Chunks by world, find *all* chest/trapped_chest BlockEntities in those selected Chunks and use existing `SavedBlockLockerChestSigns.observe` to identify attached PRIVATE/MORE_USERS/[Everyone] signs, including crossing Chunk boundaries and reciprocal double chests. Deduplicate each physical double-chest group; no skipped candidate or invented Free owner.
- Reject malformed physical chest BlockEntities, inconsistent Chunk/world/source or missing neighboring SAVED Chunk needed for a complete local observation, multiple copies, and any explicit memory budget overrun with IOException. Preserve provenance of original selected source files and the verified saved-content witness. Never mutate the real Paper world.
- Return only immutable *unpublished observations* with their original selected sources and `ContentWitness`. Do not mint `ExternalProof(true,...)`, local owner index, P/F/D balances, writer exclusion, G0 publication/Admission or G3 ACK. No evidence of a source generation after the witness's limited observation window is implied. A witnessed saved file may still differ from Paper LIVE.
- Use the existing real Paper 26.2+BlockLocker 1.15 stopped-world fixture: expected five protected chests + one nonprotected chest, seven physical chest blocks, double chest across two chunks, and no synthesized protected UUID when proof is unknown. The saved producer must report the same effective attached sign profiles as the single-chest observer. Preserve all existing JUnit/Domain/Phase6 regression and real Paper fixture markers.
- Memory bound: the initial producer has an explicit `maxSavedTerrainChunks` cap; exceeding it stops the entire candidate rather than spilling/inventing completeness. This bounded staging unit is **not** a production-scale G0 integration. A future windowed/streaming design must be selected with actual data volume and writer-freeze evidence.
- The normative A04 Process/PI/TCE remain unchanged. Formal CAT status, Green/Red, feature integration, and G0 global writer-epoch state must be reported distinctly.

## Same-source local owner-index assembly (still unpublished)
- Reject the **forged-observation loophole**: `Snapshot` is a completed capture receipt and must be constructible only by `SavedBlockLockerTerrainObservations.capture`, never by external callers attaching hand-authored chest/sign claims to a real `ContentWitness`. Keep public immutable read-only accessors and unchanged validation of the genuine captured source. This does not make saved source immutably frozen.
- Permit an actually completed, genuinely discovered SAVED world with **zero chests** to produce an empty physical owner index. Every subsequent lookup of a chest not physically observed must still reject; unknown/incomplete proof for a nonempty observation or failed builder must still reject. A zero-chest result is not evidence that whole-world G0 is ready.
- Bind an actually completed `SavedBlockLockerTerrainObservations.Snapshot` to its **identical discovered Manifest**, using the stored `WorldSnapshotTraversal.ContentWitness`; reject a different/hand-created source Manifest, unselected or duplicate physical Region provenance, and stale on-disk bytes before and after assembling the index.
- For every saved chest observation, call the existing `SavedBlockLockerPhysicalOwnerIndex.Builder.accept` exactly once with a **per-observation proof provided by the caller**. This stage must never synthesize positive `SavedBlockLockerOwnerCandidate.ExternalProof` flags; unknown proof, conflicting signs, duplicate storage members and partial failure leave no constructed index.
- A test-only independently supplied positive proof can validate local OWNER/FREE representation after authentic compressed Anvil read; it is not a G0 writer epoch or production authorization. Preserve the existing global admission, snapshot publisher, G3 ACK and real-VPS settings unchanged.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-process.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-tce.md | — | — | — | @work/.cat-flow/cache/a04-saved-owner-model.json | @work/.cat-flow/cache/a04-saved-owner-vectors.json | @work/.cat-flow/cache/a04-saved-owner-queue.json | @repo:SimpleCoins/tests/cat/a04-saved-owner-junit-binding.json | @work/.cat-flow/generated/A04SavedOwnerGeneratedTest.java | @work/.cat-flow/cache/a04-saved-owner-obligations.json | — | false |
