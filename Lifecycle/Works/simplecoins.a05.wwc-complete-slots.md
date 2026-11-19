---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wwc-complete-slots
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/544
  - https://github.com/surumeneco/SimpleCoins/pull/545
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

# A05 — 実配布WWCの3スロット全件と実体の二重計上を安全に識別

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/544 |
| https://github.com/surumeneco/SimpleCoins/pull/545 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wwc-complete-slots | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wwc-complete-slots.md;src/main/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentity.java;src/main/java/co/surumene/simplecoins/domain/SettlementAccumulator.java;src/test/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentityTest.java;src/test/java/co/surumene/simplecoins/domain/LiveWclPhysicalAliasTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentity.java;src/main/java/co/surumene/simplecoins/domain/SettlementAccumulator.java | src/test/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentityTest.java;src/test/java/co/surumene/simplecoins/domain/LiveWclPhysicalAliasTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Existing normative contract, shipped source and regression

- Accepted 02A/02B/A05 semantics require no physical asset to be summed twice and a positive unknown not to be turned into an empty source. This fixes a concrete read bridge incompatibility without changing the Confirmed Process/PI/TCE.
- The exact shipped WhatAWonderfulChicken 2.1.1 `WonderfulChickenStorageReader` reads three optional, independently named serialized PDC items: `equipment_carpet` as WCL slot `carpet`, `equipment_shulker_box` as `shulker`, and `equipment_head` as `head`. Existing `WclPhysicalSlotIdentity.keys` accepted only `shulker`, so any genuine WCL snapshot containing `carpet` or `head` caused `PaperWonderfulSavedChunkCandidates.collect` to fail despite a successful valid reader response.
- The shipped `WonderfulChickenService` synchronizes `WonderfulChickenData.headItem()` to the same Chicken's `EntityEquipment.setHelmet`. Therefore WWC WCL `head` and this Chicken's vanilla `equipment/head` must share one collision identity, just as WWW weapon and mainhand already do. Unlike the head, the WWC `shulker` and `carpet` have independent logical WCL identities. The shipped `DisplayService` additionally mirrors some gear through temporary ArmorStand visual entities; global alias resolution of those display proxies remains separate and unproven, not silently resolved by this Work.
- In scope: accept only the three actually published WCL chicken slots and reject unknown slots; map WCL head to Chicken vanilla head collision; expand LIVE world-qualified Entity head to the corresponding same-Entity saved key while preserving raw source keys. Both may not be independently committed; collisions invalidate the generation, not cancel or take an arbitrary copy.
- The original `WclServicesProbe` 25,000C genuine saved WWC shulker/WWW weapon test must remain unchanged. Add a separate isolated real Paper case invoking the shipped reader with detached WWC PDC including all three exact slots and verify the returned item bytes, positive monetary head and physical mapping. It is an API fixture, not proof that game-managed WWC display/head lifecycle is consistent in all cases.
- On direct Java21 runner, first reproduce failure of three-slot mapping and cross-view head duplication. Then change the smallest production sources and obtain Green, Java25 JUnit/Phase6 and real Paper WCL provider CI. No new published settlement, G0/G3/Admission release, or claim of formal capability conformance.
