---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wcl-physical-slot-collisions
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
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

# A05 — WCL武器・バニラ装備の物理スロット衝突を保留可能な識別子へ統一

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/44458244727 |
| https://github.com/surumeneco/SimpleCoins/pull/543 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wcl-physical-slot-collisions | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| Paper |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wcl-physical-slot-collisions.md;src/main/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentity.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;src/test/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentityTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentity.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java | src/test/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentityTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Accepted intent / isolated technical implementation

- 02B requires one physical item to be counted once across storage providers, normal Entity equipment and saved/live sources. The A05 Confirmed Process/PI/TCE permits only evidence-backed unpublished candidates; it never permits G0 publication or admission.
- Exact shipped WWW 1.1.2 WonderfulWolfStorageReader.readLoaded reads WonderfulWolfIndividual.weapon and emits WCL slot weapon; WonderfulWolfWeaponRuntime.syncWeapon maps the same individual weapon to the wolf Bukkit main hand as a rendered copy, changing the display metadata first. Therefore www/weapon and entity/UUID/equipment/mainhand must occupy the same collision key when preparing a merged physical account.
- The key is a collision guard, NOT proof that two item views have identical monetary bytes or that one may be silently removed. Until exact value, aliasing semantics, provenance and world/writer epoch are independently proven, an attempted double addition via SettlementAccumulator must HOLD.
- Pinned WWC shulker is kept separately under entity/UUID/wcl/wwc/shulker; no unverified equivalence is inferred from a chicken head display. WWW cargo:non-negative-integer remains separate.
- Add source-local physicalSlotKeys to saved WCL candidates, enforce recognized shipped provider layouts. Preserve monetary values and shared wallet UUID context; never authorize publish/admission.
- Isolated Java21 driver produced a genuine missing-class Red and A05_PHYSICAL_ALIAS_GREEN after implementation. JUnit must prove alias collision and unknown-layout rejection; real Paper smoke must retain the genuine saved two-provider 25000C proof and un-loaded far chunk.
- This unit does not prove coalescing equivalent views, global source/writer quiescence, BAN/owner, atomic publication, G3 receipts, or formal A05 Implementation Conformance.
