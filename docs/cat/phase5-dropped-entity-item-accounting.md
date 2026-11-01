---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.dropped-entity-item-accounting
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: dropped-item Entity representation integrity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.


## 対象
| Ref | Aspect |
| --- | --- |
| saved minecraft:item Entity | 単一Itemと競合するItems/Inventoryの排除 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| drop.single | 正当なItemとUUIDを持つminecraft:itemを読み取るとき | 単一stackのみ計上しUUID一意性を維持する | SC-DROP-01 |
| drop.malformed | Itemが欠損または不正なとき | 保存資産走査をfail-closedで拒否する | SC-DROP-02 |
| drop.competing | minecraft:itemがItemsまたはInventoryも持つとき（空listを含む） | dropped extractorとinventory extractorの両方で競合表現を拒否する | SC-DROP-03, SC-DROP-04, SC-DROP-05 |
| drop.regression | 通常drop以外のEntityを評価するとき | 既存の非inventory mobとchest minecartの境界を維持する | SC-DROP-06 |

## Process / PI

The existing `StoredDroppedItemAssets` extractor already reads the single `Item` compound of a saved `minecraft:item` Entity, and `StoredEntityInventoryAssets` separately reads slotted `Items` and unslotted `Inventory` lists. The two independent extractors must never count competing representations on the same physical Entity.

A dropped Entity is valid only with one `Item` stack, a four-int UUID, and **neither** `Items` nor `Inventory`. Reject the conflicting representation even if one list is empty. The inventory extractor must independently reject `Items`/`Inventory` on `minecraft:item`, rather than treating them as an additional inventory. Preserve existing single-stack valuation and UUID identity; no ownership inference is authorized.

This only validates staged saved Entity data. Loaded-vs-disk reconciliation, complete source inventory, entity ownership, snapshot freeze and settlement publication remain independent gates.

## TCE / acceptance vectors

- SC-DROP-01 (Regression): a normal saved `minecraft:item` contributes one UUID-keyed physical location via `StoredDroppedItemAssets`.
- SC-DROP-02 (Regression): missing or malformed `Item` fails closed in the dropped-item extractor.
- SC-DROP-03 (Red): `minecraft:item` containing both `Item` and `Items` must fail in the dropped-item extractor, including an empty `Items` list.
- SC-DROP-04 (Red): `minecraft:item` containing both `Item` and `Inventory` must fail in the dropped-item extractor.
- SC-DROP-05 (Red): the independent Entity-inventory extractor rejects a fabricated slotted or unslotted `minecraft:item` inventory.
- SC-DROP-06 (Regression): ordinary dropped items, non-inventory mobs, and chest-minecart `Items` retain existing behavior.

## Gate status

Formal CAT machine gates: **not-run**. This Draft is not Confirmed and does not authorize settlement publication, merge or deploy. Local Java checks and JUnit evidence are independent of formal CAT confirmation.
