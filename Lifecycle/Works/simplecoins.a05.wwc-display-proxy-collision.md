---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wwc-display-proxy-collision
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/546
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

# A05 — WWC表示ArmorStandの通貨複製を元WCLスロットと衝突させる

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/546 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/44463648959 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wwc-display-proxy-collision | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wwc-display-proxy-collision.md;src/main/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentity.java;src/main/java/co/surumene/simplecoins/domain/SettlementAccumulator.java;src/main/java/co/surumene/simplecoins/paper/PaperLoadedChunkAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperWwcDisplayPhysicalAlias.java;src/test/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentityTest.java;src/test/java/co/surumene/simplecoins/domain/LiveWclPhysicalAliasTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentity.java;src/main/java/co/surumene/simplecoins/domain/SettlementAccumulator.java;src/main/java/co/surumene/simplecoins/paper/PaperLoadedChunkAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperWwcDisplayPhysicalAlias.java | src/test/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentityTest.java;src/test/java/co/surumene/simplecoins/domain/LiveWclPhysicalAliasTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Approved meaning and source-level boundary

- Existing 02A/02B/A05 approved no-double-count semantics only. The exact shipped WWC 2.1.1 DisplayService creates temporary nonpersistent, invisible, marker, invulnerable ArmorStands. It writes `whatawonderfulchicken:display_owner` = Chicken UUID and `whatawonderfulchicken:display_role` = `CARPET` or `SHULKER_BOX`, and copies the corresponding gear ItemStack to the ArmorStand helmet for rendering. In contrast, `WonderfulChickenStorageReader` exposes the original gear as WCL `carpet` and `shulker` slots.
- Current `PaperLoadedChunkAssets.scan` treats this ephemeral visual stand as another normal LivingEntity helmet and may see monetary content duplicated from the same source item. The stand has a *different Entity UUID*, so #544–#546's shared wallet UUID and owner-Entity collision mechanisms do not detect ordinary coin denominations mirrored inside a non-wallet shulker.
- Only for a real Paper ArmorStand bearing **both** official WWC display PDC tags, with canonical owner UUID, valid same-World loaded Chicken with WWC type marker, and expected ephemeral display flags, map its *head equipment's physical accounting collision identity* to `world/<world-name>/entity/<chicken-UUID>/wcl/wwc/<carpet|shulker>`. Keep the original observed ItemStack monetary value and world source; do not skip/drop display items or claim content equality.
- Unknown role, malformed provenance, foreign parent world, incompatible flags, or monetary items on other equipment slots must fail closed, not count as independent value. Untagged vanilla ArmorStands keep their existing independent-item handling.
- The accumulator collides the new world-qualified candidate identity with the unqualified WCL `carpet/shulker` source and rejects the whole generation. A later same-generation, same-item equivalence proof is required to **coalesce** a temporary display with its WCL original. This Work does not itself make G0 publicable or alter player admission, provider installation, G3 grants or the normative CAT s0–s7 outputs.
- Direct Java21 test first demonstrates that WCL `shulker` and its WWC projected `SHULKER_BOX` do not yet share a physical alias. JUnit then verifies CARPET and SHULKER_BOX mapping, rejected unknown roles, duplicate refusal and plain equipment preservation; isolated real Paper 26.2 tests verify an ephemeral ArmorStand with shipped role/owner PDC and a monetary helmet is represented as the same WCL slot.
