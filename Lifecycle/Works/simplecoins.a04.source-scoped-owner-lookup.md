---
cat_version: '0.2'
kind: work
id: simplecoins.a04.source-scoped-owner-lookup
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

# A04 / G0前段 — 保存されたBlockOwner問い合わせへの正本source/Chunk文脈の維持

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
| simplecoins.blocklocker.saved-owner | simplecoins.a04.source-scoped-owner-lookup | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.blocklocker.live-protection |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.source-scoped-owner-lookup.md;src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java;src/test/java/co/surumene/simplecoins/domain/SettlementScanRunnerTest.java | src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java | src/test/java/co/surumene/simplecoins/domain/SettlementScanRunnerTest.java |

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

- Reuse the already Confirmed A04 SAVED owner observation requirement and existing 02B accounting source selection: the exact `WorldStorageDiscovery.SourceFile` and `AnvilRegionReader.Chunk` that supplied a block entity must remain available when its owner callback is invoked, not reconstructed from the unqualified world/position alone.
- Add an **opt-in** source-scoped block owner callback to `SettlementScanRunner` while retaining legacy `BlockOwnerLookup(world, position)` behavior and existing call sites. The new callback is invoked only for authoritative SAVED REGION chunks that actually reached the visitor after LIVE/SAVED partition selection. Preserve fail-stop IOExceptions; no alternate owner Free fallback.
- The `stageWithBanCoverage` wrapper must preserve the same source-scoped callback capability and must **still** replace caller-provided BAN/playerdata flags with independently observed `BanSourceCoverage` and Manifest playerdata. It must never call a source-aware callback using the legacy unqualified method, even though older callers retain that method.
- Test a real discovered temporary REGION holding a chest, exact source identity and actual decoded Chunk, legacy callback compatibility, source-aware missing BAN coverage rejection, and LIVE-shadowed chunk not invoking the SAVED scoped callback.
- The returned Staged object remains **unpublished**. Correctly supplying a source and Chunk establishes neither global source completeness, all-writer exclusion, a retained protected epoch, final P/F/D nor publication/Admission. This Work changes no Confirmed PI/TCE or runtime plugin wiring and does not promote A01/G0 Draft.
