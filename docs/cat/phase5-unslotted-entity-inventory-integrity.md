---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.unslotted-entity-inventory-integrity
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: saved Entity unslotted inventory identity (CAT Draft)

Status: **Draft** (not Confirmed). Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.


## 対象
| Ref | Aspect |
| --- | --- |
| saved Entity Inventory | villager/allayのunslotted保存資産と非対象Entity |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| unslotted.unsupported | chest minecartやitem EntityがInventoryを持つとき | 別の物理資産源とみなさずIOExceptionで拒否する | SC-ENTITY-UNSLOTTED-01, SC-ENTITY-UNSLOTTED-02 |
| unslotted.supported | villager/allayのInventoryを読むとき | それぞれの既存物理slot数に従って計上する | SC-ENTITY-UNSLOTTED-03, SC-ENTITY-UNSLOTTED-04 |
| unslotted.slotted | chest minecartのItemsを読むとき | 独立したslotted契約の既存計上を維持する | SC-ENTITY-UNSLOTTED-05 |

## Process / PI

The saved Entity inventory scanner accepts the unslotted `Inventory` representation only for vanilla `minecraft:villager` (eight physical positions) and `minecraft:allay` (one physical position). Vanilla container vehicles use slotted `Items`; their fabricated unslotted `Inventory` must not become an additional countable asset source. An unsupported unslotted representation aborts the read-only extraction with `IOException` before any value can be published. This does not determine ownership, loaded/disk identity, or snapshot consistency.

## TCE / acceptance vectors

- SC-ENTITY-UNSLOTTED-01 (Red): `minecraft:chest_minecart` with `Inventory` containing a coin rejects instead of returning a balance.
- SC-ENTITY-UNSLOTTED-02 (Red): `minecraft:item` with an extra `Inventory` rejects instead of exposing a second physical source.
- SC-ENTITY-UNSLOTTED-03 (Regression): villager `Inventory` still counts one valid coin stack at position 0.
- SC-ENTITY-UNSLOTTED-04 (Regression): allay `Inventory` still counts one valid coin stack at position 0.
- SC-ENTITY-UNSLOTTED-05 (Regression): chest minecart `Items` continues to count a valid slot.

## Gate status

Formal CAT machine gates: **not-run**. This Draft is not Confirmed. No settlement publication, merge, or deployment is authorized by these vectors.
