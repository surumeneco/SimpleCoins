---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-attached-chests
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

# A04 SAVED BlockLocker — physically attached chest-sign observation

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
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-attached-chests | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.blocklocker.live-protection |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-attached-chests.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerAttachedProtection.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerAttachedProtectionTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;.github/workflows/paper-blocklocker-115-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerAttachedProtection.java | src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerAttachedProtectionTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java |

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

- Under the existing user-Confirmed A04 Process observation contract, read a **chosen** coherent SAVED terrain Chunk set; for a chest or double-chest, check its saved block type/half/facing and the actual wall/standing sign blocks attached to every relevant chest half. Enumerate all five BlockLocker v1.15 supported adjacent sign positions (N/E/S/W/UP), including necessary neighboring Chunks; missing input, malformed NBT or invalid double-chest reciprocal evidence must throw IOException and be treated as HOLD by the caller.
- Interpret only physically attached sign BlockEntities with the previously verified BlockLocker v1.15 saved display/PDC reader. Do not infer owner or Everyone from raw sign strings alone; inactive/unsupported/physically unrelated signs do not protect this chest. Recognize all applicable signs across a two-half chest once, with a stable position set. Explicitly report no valid protection signs only after all relevant source positions were readable. Multiple competing PRIVATE main signs are an ambiguous HOLD rather than an invented owner.
- Output is a **local unpublished observation** (physical chest parts, applied sign profiles, one verified primary UUID if any, Everyone if any, and whether any protection sign exists). This is not proof of playerdata/BAN, world coverage, a common writer epoch, source ownership, final P/F/D classification or atomic settlement publication.
- Start with ordinary CHEST/TRAPPED_CHEST only; unrelated containers are not misclassified as Free. The isolated Paper 26.2 + BlockLocker 1.15 fixture checks one unprotected chest, two owner-signed chests (including the cross-Chunk double), front/backsided Everyone and unresolved profile, while Java unit cases cover missing neighbors, sign block entities and wrong orientations. Maintain existing admission/ledger/snapshot fail-close.
- No new PI-only JUnit Queue item is claimed for observer test suites, and CAT Implementation Conformance remains inconclusive until all Process capabilities are independently demonstrated. Source-derived tests follow the existing Confirmed contract without self-promoting specifications.
