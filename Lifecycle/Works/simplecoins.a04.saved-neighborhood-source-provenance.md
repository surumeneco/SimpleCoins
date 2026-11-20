---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-neighborhood-source-provenance
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

# A04 — 出典はチェストだけでなく検査した看板近傍の全保存Regionを含める

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/579 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-neighborhood-source-provenance | simplecoins.blocklocker.saved-owner;simplecoins.world-source-discovery;simplecoins.settlement.writer-epoch-publication |

## Technologies
| Technology |
| --- |
| Java25; JUnit; bounded Anvil Region NBT read; canonical CAT-TDD |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-neighborhood-source-provenance.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservations.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservationsTest.java | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservations.java | src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservationsTest.java |

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

## Acceptance and technical boundary

The current **confirmed A04 physical BlockLocker** rules require all read SAVED chest and attachment evidence to be source-backed; this independent Work corrects the producer's existing `ObservedChest.sources` provenance, not any world/owner classification rule. The current `SavedBlockLockerTerrainObservations.capture` already parses the full selected source set and obtains a global `ContentWitness`. However its *per-observation* source list currently collects only storage member Chunks; it omits adjacent Chunks actually read to decide whether a protection sign **exists or is absent**. A single chest at x=511 can be influenced by a sign at x=512 in the neighboring `r.1.0.mca` although the observation advertises only `r.0.0.mca`. This is an **incomplete local provenance representation** even when the whole-world witness is separately present; do not claim a realized G0 false publication.

1. Preserve the existing genuine discovered `WorldStorageDiscovery.Manifest`, complete source traversal, `SavedBlockLockerChestSigns.observe`, physical chest/double-chest classification, `maxSavedTerrainChunks` limit, and `ContentWitness`. Do not modify runtime admission/ACK/G0 publication.
2. Build a compact per-Chunk `SourceFile` identity map from the already selected traversal. For every observed physical member, include its own Chunk and the five coordinates that `observe` actually checks for protection signs (north/east/south/west/up). Resolve each of these positions to the selected Chunk; fail closed on a missing/ambiguous/mismatched source or coordinate overflow. Deduplicate source file identities and order the list deterministically. Include neighbor source even when there is **no** sign there, since sign absence is a source-dependent negative observation.
3. Red/Green: create real compressed Anvil fixtures with one chest at x=511, y=65, z=4 in `r.0.0.mca`, and the adjacent x=512 Chunk in `r.1.0.mca`. Assert that the one valid observation lists **both** Region source identities when the neighbor is air, and also when an actual valid attached wall sign lives in the adjacent Region. Missing neighbor must reject the entire candidate, not silently infer unprotected.
4. Require same-HEAD JUnit Domain and Phase6 regression as well as any existing A04 Paper fixture workflows triggered by this file. Distinguish a correct local source list from a globally stable source/writer epoch.

## Formal status limits

This Work is based on the already user-Confirmed A04 Process/PI/TCE; it does **not** promote its own Draft, create a new product decision, grant a `SavedBlockLockerOwnerCandidate.ExternalProof`, or publish P/F/D statistics. Its source-local correction can only be included as a verified implementation unit subject to the actual CAT source flow and independent CI. Source-list completeness is not all-Paper writer closure, original G0 settlement, G3 durable grant, login permission, or live server proof.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-process.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-tce.md | — | — | — | @work/.cat-flow/cache/a04-saved-owner-model.json | @work/.cat-flow/cache/a04-saved-owner-vectors.json | @work/.cat-flow/cache/a04-saved-owner-queue.json | @repo:SimpleCoins/tests/cat/a04-saved-owner-junit-binding.json | @work/.cat-flow/generated/A04SavedOwnerGeneratedTest.java | @work/.cat-flow/cache/a04-saved-owner-obligations.json | — | false |
