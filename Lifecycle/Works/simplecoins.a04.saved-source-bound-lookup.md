---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-source-bound-lookup
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

# A04 SAVED BlockLocker — selected-source-bound ownership read

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/563 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-source-bound-lookup | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.blocklocker.live-protection |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-source-bound-lookup.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookup.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookupTest.java | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookup.java | src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookupTest.java |

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

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-process.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-tce.md | — | — | — | @work/.cat-flow/cache/a04-saved-owner-model.json | @work/.cat-flow/cache/a04-saved-owner-vectors.json | @work/.cat-flow/cache/a04-saved-owner-queue.json | @repo:SimpleCoins/tests/cat/a04-saved-owner-junit-binding.json | @work/.cat-flow/generated/A04SavedOwnerGeneratedTest.java | @work/.cat-flow/cache/a04-saved-owner-obligations.json | — | false |

## Acceptance

- Reuse user-Confirmed A04 Process/PI/TCE and already established physical-owner index. A positive SAVED chest owner attribution must bind to the exact WorldStorageDiscovery SourceFile and AnvilRegionReader.Chunk selected by the existing source-aware SettlementScanRunner; world/coordinate alone is insufficient for this stricter opt-in flow.
- Add opt-in `sourceScoped()` on the existing unpublished `SavedBlockLockerSettlementOwnerLookup` returning a `SettlementScanRunner.SourceBlockOwnerLookup` while **preserving** the legacy `resolve(world, position)` behavior. The opt-in callback must refuse unqualified invocation, unknown/unselected SourceFile, wrong Region kind/world, incorrect chunk coordinates, noncanonical region filename or wrong physical BlockEntity location/type.
- The exact decoded Chunk must contain **one matching saved physical chest/trapped_chest BlockEntity** at the requested x,y,z. Duplicate/missing/malformed block entities HOLD, not inferred non-protection. Validate that the selected Region filename corresponds to the decoded Chunk region coordinates, and that the block's chunk corresponds to the decoded Chunk.
- Only after those checks may the lookup consult the previously built physical owner index and independently verified BAN/playerdata. Do not claim that synthetic chunk test evidence proves a real source was parsed, writer quiescence, full source completeness, or any final publication.
- No change to Paper runtime, global SettlementScanRunner wiring, Admission, G0 all-writer generation, G3 ACK, main or VPS. Test source-bound positive/negative, preservation of legacy call, and fail-closed cases. Formal whole-A04 implementation conformance remains inconclusive.
