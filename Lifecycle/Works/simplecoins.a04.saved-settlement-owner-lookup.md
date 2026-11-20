---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-settlement-owner-lookup
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

# A04 SAVED BlockLocker — BAN-aware candidate-to-settlement owner lookup

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
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-settlement-owner-lookup | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.blocklocker.live-protection |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-settlement-owner-lookup.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookup.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookupTest.java | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookup.java | src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookupTest.java |

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

- Adapt the existing verified, unpublished A04 physical owner index to the BlockEntitySettlementImport.Ownership value type expected by SettlementScanRunner.BlockOwnerLookup. Do not silently assume every scanned block is a chest or absent index means free.
- Require a real discovered WorldStorageDiscovery.Manifest with non-null discovery origin, and complete BanSourceCoverage for every UUID needed by an OWNER lookup. Use manifest.playerDataFiles().containsKey(uuid) and banSources.currentlyBanned(uuid) rather than caller-supplied current-BAN/hasPlayerdata booleans. Map a positive OWNER to its UUID and verified statuses; map effective Everyone FREE to owner=null, everyoneProtected=true; map verified non-protection FREE to owner=null, everyoneProtected=false.
- Unknown/missing BAN provider coverage for a referenced owner, absent index position, invalid world/position, missing discovery provenance must throw IOException, not invent Free or a registered player. Immutable staging-only callback: no generation/writer epoch freeze, no world or player edits, no asset publication or Admission.
- Tests use actual bounded WorldStorageDiscovery discover with temp playerdata path, positive and negative BanSourceCoverage, and unproven missing-source behavior. No requirement to wire the callback into the global settlement coordinator until all source kinds and same-epoch Proof are established.
- Existing Confirmed A04 Process/PI/TCE semantics unchanged; prior CAT Closure/Model passed, full Implementation Conformance still inconclusive.
