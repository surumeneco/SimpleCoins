---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.slotted-entity-holder-domain
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: saved slotted Entity holder domain (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.


## 対象
| Ref | Aspect |
| --- | --- |
| saved Entity Items | 対応するvanilla holderの物理slot topology |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| holder.unsupported | 未対応Entityや架空chest_boat型がItemsを持つとき | 実在しない物理inventoryを推測で計上せず拒否する | SC-ENTITY-SLOT-HOLDER-01, SC-ENTITY-SLOT-HOLDER-02, SC-ENTITY-SLOT-HOLDER-05 |
| holder.supported | 既知chest/hopper minecart、chest boat、bamboo chest raftを読むとき | 確認済み物理slot上限を維持する | SC-ENTITY-SLOT-HOLDER-03, SC-ENTITY-SLOT-HOLDER-06 |
| holder.unslotted | villager/allayのInventoryまたはinventoryのないEntityを読むとき | 独立したunslotted契約と非対象の境界を維持する | SC-ENTITY-SLOT-HOLDER-04 |

## Process / PI

A saved Entity may expose an `Items` list. The list alone is not proof that the Entity has physical inventory slots. Before counting any currency, classify the vanilla Entity holder against its supported slot topology. An unsupported or unknown holder exposing `Items` is ambiguous saved economic evidence and must abort extraction, not silently create currency. Do not infer ownership or publish a settlement from this extractor.

Supported slotted holders for the current adapter: `minecraft:chest_minecart` (27 slots), `minecraft:hopper_minecart` (5 slots), and the **known vanilla chest-boat variants** `oak`, `spruce`, `birch`, `jungle`, `acacia`, `dark_oak`, `mangrove`, `cherry`, `pale_oak` (27 slots), plus `minecraft:bamboo_chest_raft` (27 slots). An arbitrary `minecraft:*_chest_boat` spelling is not authority to create inventory slots. The 1.21.2 boat-type split is documented in the official Minecraft 1.21.2 release notes; later variants require their own verified contract. Villagers and allays retain their separate unslotted `Inventory` contract. Unknown custom/plugin inventory formats require an independently verified adapter rather than broad acceptance.

## TCE / acceptance vectors

- SC-ENTITY-SLOT-HOLDER-01 (Red): `minecraft:cow` with an `Items` list containing a valid coin fails closed instead of counting a fictitious inventory.
- SC-ENTITY-SLOT-HOLDER-02 (Red): an unknown namespaced custom Entity with an `Items` list fails closed.
- SC-ENTITY-SLOT-HOLDER-03 (Regression): chest minecart, hopper minecart, chest boat and bamboo chest raft retain supported slots and existing capacity guards.
- SC-ENTITY-SLOT-HOLDER-04 (Regression): villager/allay unslotted inventories remain countable; a holder without any inventory tag remains outside this extractor.
- SC-ENTITY-SLOT-HOLDER-05 (Red): invented `minecraft:fictional_chest_boat` must fail closed even though its suffix resembles a vanilla holder.
- SC-ENTITY-SLOT-HOLDER-06 (Regression): known `minecraft:pale_oak_chest_boat` remains countable with its physical 27-slot topology.

## Gate and publication status

This document is a CAT **Draft**, not an approved/Confirmed specification. Formal CAT machine gates and live Paper tests are **not-run**. This isolated fail-closed check does not resolve loaded/saved snapshot reconciliation, ownership or authoritative settlement publication.
