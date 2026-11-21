---
cat_version: '0.2'
kind: work
id: simplecoins.a04.sparse-terrain-capture-refactor
status: draft
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-20:A04-saved-BlockLocker-CAT-approved
refs: []
entry: refactor
mode: implementation
flow: refactor
work_kind: refactor
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A04 — SAVED BlockLocker terrain sparse-memory capture refactor

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/Lifecycle/Works/simplecoins.a04.saved-terrain-observation-producer.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a04-saved-blocklocker-owner-process.md |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.a04.sparse-terrain-capture-refactor | simplecoins.blocklocker.saved-owner.pi.classify;simplecoins.blocklocker.saved-owner.tce.classify;simplecoins.settlement |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../../../.. | develop | feature/* | Lifecycle/Works/InProgress/simplecoins.a04.sparse-terrain-capture-refactor/Work.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservations.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservations.java | — |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a04-sparse-unchanged-baseline | refactor-baseline | junit | @repo:SimpleCoins | bash -lc 'gradle --no-daemon test --tests co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservationsTest && cp build/test-results/test/TEST-co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservationsTest.xml {report}' | evenAbsentAdjacentSignRequiresNeighborRegionProvenance() | — | — | 300 |
| a04-sparse-unchanged-regression | regression | junit | @repo:SimpleCoins | bash -lc 'gradle --no-daemon test --tests co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservationsTest && cp build/test-results/test/TEST-co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservationsTest.xml {report}' | evenAbsentAdjacentSignRequiresNeighborRegionProvenance() | — | — | 300 |

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

## Immutable refactor boundary

- **Known baseline**: `develop@3d9291aa32e8e2070b28148eace514b432f16d7c` and successful exact-head `Domain foundation` CI run 44907245638 (Gradle JUnit incl. SavedBlockLockerTerrainObservationsTest). This is external test evidence, not a CAT formal refactor-baseline Gate.
- Preserve the Confirmed A04 source/ownership semantics and exactly the same immutable `Snapshot`/`ObservedChest` public contract. No new permit, independent protection proof, private owner derivation, writer-epoch authorization, P/F/D publication or G3 ACK.
- Preserve original complete selected `Manifest` traversal and budget checks, strict malformed/duplicate/unknown saved chest refusal, actual selected terrain palette checks, all five adjacent blocks including missing-neighbor failure, source Region provenance, double-chest pairing, same-witness readback and file-set closure. Never silently remove unrelated positive or zero-valued sources.
- **Refactor internal allocation only**: a first **complete, verified** traversal records checked physical chest positions and every selected saved Chunk/source identity, without retaining unrelated inflated terrain NBT. Re-read **only selected Anvil Region files containing a chest or its one-block sign neighborhood** with the bounded canonical `AnvilRegionReader.scan`; verify each selected Region's complete Chunk/source identity against the first pass and retain only needed Chunk NBT. The original first traversal's **full-source content witness** (all other Regions, sidecars and playerdata) must remain valid before and after local classification. A verified chestless scan returns after the first pass. Additional read I/O is limited to relevant Region files; `maxSavedTerrainChunks` and fail-closed coverage remain unchanged.
- Unchanged-oracle regression: existing `SavedBlockLockerTerrainObservationsTest`, complete Domain JUnit, Phase6 startup tests and current real Paper26.2 BlockLocker/WCL restored-source probe. The latter remains a local unpublished candidate with no global writer freeze. The permanent Work runner uses the repository's real Gradle/JUnit environment and its actual XML; Chat-only direct-JUnit transport scripts and local absolute paths are not prerequisites for future runs. Inspect actual CI exact HEAD before integrating feature into develop.
- Stop/refuse if any witness, selected chunk identity, neighbor source, physical block/BlockEntity or independent proof differs; never substitute a guessed FREE owner or downgrade source checks. Do not change main, VPS, admission guard, any Confirmed Artifact or test oracle.
