---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-attached-chest-signs
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

# A04 SAVED BlockLocker — attached chest-sign observation

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
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-attached-chest-signs | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.blocklocker.live-protection |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-attached-chest-signs.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerChestSigns.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerChestSignsTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;.github/workflows/paper-blocklocker-115-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerChestSigns.java | src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerChestSignsTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java |

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

- Reuse the approved SAVED BlockLocker 1.15 physical-topology observation capability without modifying the Confirmed PI/TCE. From externally supplied coherent saved terrain Chunks, observe a supported chest or reciprocal double chest and every applicable physical wall/standing sign on its 5 attachment faces (four sides and above) on both halves, including adjacent Chunks.
- Decode physical block properties using SavedTerrainBlockPalette; use BlockLockerSignAttachment, BlockLockerChestLinkage and BlockLockerSavedProfileResolution. For real signs, a missing/malformed saved BlockEntity is unknown and throws; signs facing away and ordinary signs with no protection header are excluded.
- Verify every queried neighbor Chunk exists; reject inconsistent/missing Chunk, mismatched or duplicate sign entity, unsupported storage or nonreciprocal double chest. Never silently equate missing source with air or 'no protection'.
- Return a read-only list of **applicable sign observations** and the physical member block positions. Do not yet derive OWNER/FREE, 'all signs of the world' or same writer-epoch guarantees. G0, BAN/playerdata, Snapshot admission, G3 and VPS remain unchanged. Read-only fixture is test evidence, not global settlement readiness.
- Test Red before source implementation; use locally runnable isolated tests and integrated Paper 26.2 + BlockLocker 1.15 Anvil fixture for the two-chunk double chest. CAT PI-only generated Queue remains owned by its earlier verdict Work, not by this new observer; CAT Implementation Conformance remains inconclusive until all capabilities are established.
