---
cat_version: '0.2'
kind: work
id: simplecoins.a04.live-blocklocker-owner-stage
status: draft
process: simplecoins.settlement.ownership-evidence
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A04 — LIVE BlockLocker 1.15の実コンテナ貨幣の帰属を元Chunk会計源へ採用

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/4 |
| https://github.com/surumeneco/SimpleCoins/pull/402 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.ownership-evidence | simplecoins.a04.live-blocklocker-owner-stage | simplecoins.o0.blocklocker-115-live-owner-evidence;simplecoins.a04.blocklocker-saved-restart-evidence;simplecoins.ban-source-coverage |

## Technologies
| Technology |
| --- |
| Java 25; Paper 26.2 build 134; BlockLocker 1.15 read-only API; JUnit; actual Paper source-valued Chunk monetary scan; strict per-owner BAN coverage |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.live-blocklocker-owner-stage.md;src/main/java/co/surumene/simplecoins/domain/LiveContainerPhysicalSlot.java;src/main/java/co/surumene/simplecoins/paper/PaperBlockLockerLiveClassifier.java;src/test/java/co/surumene/simplecoins/domain/LiveContainerPhysicalSlotTest.java;src/test/java/co/surumene/simplecoins/paper/PaperBlockLockerLiveClassifierTest.java;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;.github/workflows/paper-blocklocker-115-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/LiveContainerPhysicalSlot.java;src/main/java/co/surumene/simplecoins/paper/PaperBlockLockerLiveClassifier.java | src/test/java/co/surumene/simplecoins/domain/LiveContainerPhysicalSlotTest.java;src/test/java/co/surumene/simplecoins/paper/PaperBlockLockerLiveClassifierTest.java;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;.github/workflows/paper-blocklocker-115-live-smoke.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a04-live-protected-value | regression | junit | @repo:SimpleCoins | gradle test | block-slot-identity;protected-owner;everyone-free;ban-dead;missing-ban-hold | src/test/java/co/surumene/simplecoins/domain/LiveContainerPhysicalSlotTest.java;src/test/java/co/surumene/simplecoins/paper/PaperBlockLockerLiveClassifierTest.java | JUnit XML;GitHub Actions | 480 |

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

## Source-grounded acceptance

- [02A](https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view) confirms effective BlockLocker `[Everyone]` means Free, otherwise UUID-resolved primary owner is Personal if complete current BAN CLEAR and playerdata exists, Dead otherwise. The existing `BlockLockerLiveEvidence.observe` pins real v1.15 provider identity, main-thread API and external owner reading, already tested on actual Paper 26.2. This Work **reuses** that read-only primitive, not new BlockLocker semantics or guessed name-to-UUID.
- The product's `PaperLoadedChunkAssets.scan` emits `world/<world>/<x>,<y>,<z>:<slot>` for positive currency held by real Block containers. Parse only this canonical source physical key; require source to be the matching `HybridLiveSourceStage.ChunkSource`, same world, chunk, observed world object/UUID and bounded actual loaded block. Reject Entity/Display/Player keys, malformed coords, extra suffix, off-chunk positions, swapped world names and unloaded chunks. Never cause Chunk or World loading.
- The classifier must require `BlockState instanceof Container`, the selected slot's exact position in that source, and verified `BlockLockerLiveEvidence.observe(Block)`. Return a `SettlementAccumulator.Asset` with source ID, value's original key, world and amount **unchanged**, plus owner/everyone/BAN/playerdata classification. A null, missing, UNKNOWN BAN provider/subject must fail closed; a confirmed active ban or absent playerdata with known owner goes to Dead, not Free; effective Everyone always Free regardless of unknown displayed owner.
- Distinguish provider observation at the time of classification from the original money value capture. This Work cannot prove all-world G0 source generation/writer freeze and never publishes or completes admission. It does not resolve SAVED unloaded signs or all Entity owners. Do not alter 02A/02B meaning.
- JUnit Red then production Green for physical slot source and owner priority. Run existing JUnit and Phase6. Use the existing official Paper 26.2 + SHA pinned BlockLocker 1.15 CI test: real saved and reloaded protected chest signs and actual container's canonical currency. Assert that the classifier's original source value and physical owner results are preserved and correct. Keep independent admission guard default-deny. Current CI Work structural validate is not a normative CAT Process Closure or Implementation Conformance.
