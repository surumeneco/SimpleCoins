---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-terrain-palette
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

# A04 SAVED BlockLocker — persisted block palette read-only observer

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
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-terrain-palette | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.blocklocker.live-protection |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-terrain-palette.md;src/main/java/co/surumene/simplecoins/domain/SavedTerrainBlockPalette.java;src/test/java/co/surumene/simplecoins/domain/SavedTerrainBlockPaletteTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;.github/workflows/paper-blocklocker-115-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/SavedTerrainBlockPalette.java | src/test/java/co/surumene/simplecoins/domain/SavedTerrainBlockPaletteTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java |

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

- Move the existing test-only Paper 26.2 palette-indexed terrain block-state reader into a **read-only production domain observer**. The caller supplies the chosen saved terrain Chunk and a world position; the observer returns only verified block ID and block properties, never owner, valid protection, all-sign closure or a published settlement result.
- Check selected chunk coordinates, section Y, palette entries, packed long array, offsets, property types and malformed/missing data. Abort on uncertainty rather than treat unknown storage as air or Free. World-source epoch and writer exclusion are proven elsewhere.
- Test first: synthetic one-block palette and packed multi-palette states including x=15/16 cross-Chunk edge, malformed palette/indices/coordinates and unsupported missing sections. Use the real Paper 26.2 stopped Anvil in an independent JUnit to verify five protection wall signs and seven actual chest states at known positions, after the previous real-sign/profile checks. This is a bounded fixture, not an all-world production source set.
- This Work reuses the approved A04 Process observation capability; formal Process/model semantics and existing ownership verdict are unchanged. No CAT-generated PI-only Queue item is claimed as this observer's own Green. All adapter/real-Paper tests are supplementary execution evidence; CAT implementation conformance remains **inconclusive** until the whole observation/action and source-gathering path is proved. Admission and settlement remain blocked.
