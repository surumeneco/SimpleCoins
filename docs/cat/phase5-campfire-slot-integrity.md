---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.campfire-slot-integrity
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: saved campfire inventory slot integrity (CAT Draft)

Status: **Draft** (not Confirmed). Parent TASK-20261031-SIMPLECOINS-100; leaf TASK-20261101-003.


## 対象
| Ref | Aspect |
| --- | --- |
| saved campfire BlockEntity | 4つの物理調理slotの境界 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| campfire.out-of-range | campfire/soul_campfireのItemsにslot 4以上があるとき | 存在しない物理slotを資産計上せずIOExceptionで拒否する | SC-CAMPFIRE-01, SC-CAMPFIRE-02 |
| campfire.valid | slot 0〜3または空Itemsを読み取るとき | 既存の有効な4slotと空inventoryの挙動を維持する | SC-CAMPFIRE-03, SC-CAMPFIRE-04 |

## Process / PI

A saved vanilla campfire or soul campfire has four physical cooking item positions, indexed 0 through 3. The read-only settlement scanner must reject an `Items` entry at slot 4 or above rather than treating a fabricated fifth slot as additional saved currency. Slots 0 through 3 remain countable. Existing duplicate slot, item-value and chunk-coordinate checks remain in effect. The scanner does not publish settlement results.

## TCE / acceptance vectors

- SC-CAMPFIRE-01 (Red): `minecraft:campfire` slot 4 with a valid currency stack is rejected with IOException.
- SC-CAMPFIRE-02 (Red): `minecraft:soul_campfire` slot 4 is rejected.
- SC-CAMPFIRE-03 (Regression): both campfire variants at slot 3 are countable.
- SC-CAMPFIRE-04 (Regression): empty campfire inventory remains valid.

Formal CAT machine gates not-run; Draft is not Confirmed. No merge, deploy, settlement commit, or publication.
