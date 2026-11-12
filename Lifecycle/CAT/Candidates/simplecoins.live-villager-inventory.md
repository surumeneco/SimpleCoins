---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-villager-inventory
status: candidate
process: simplecoins.settlement.live-entity-inventory
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/336
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paperライブ村人内部Inventory抽出 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| abstract-villager | `AbstractVillager.getInventory()` の内部収納（交易GUIではない） |
| living-equipment | 装備・主手・副手の既存6スロット |
| holder-identity | Entity UUID・物理Inventory slot・財布UUID |
| source-budgets | メインスレッド・スロット・NBTデコード制限 |

## 観測

Paper 26.2の`AbstractVillager`は`InventoryHolder`を実装し、`getInventory()`で交易画面とは別の内部収納にアクセスできる。現行`PaperLoadedChunkAssets`は`LivingEntity`の装備を読んだ後`continue`するため、村人内部Inventoryの貨幣が欠落する。保存NBT側は`StoredEntityInventoryAssets`で`minecraft:villager`の`Inventory`に対応済み。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| villager-01 | ロード済み村人系Entityの内部Inventoryに貨幣がある | 装備と独立の物理slotとして読み取る | Paper26.2実Entity fixture |
| villager-02 | 同一Entityの装備と内部Inventoryに同じ財布UUID | 二重計上とせず全走査を拒否する | コピー財布再現 |
| villager-03 | Playerや無関係なMobがInventoryHolderでもある | 既存のPlayer専用／非生体保管体との区分を変えない | 型・JUnit回帰 |
| villager-04 | Inventory内容・size不一致、slotやNBT予算を超過 | 全体走査を拒否し部分結果を公開しない | 上限回帰 |
| villager-05 | 保存/ライブ、所有者、BAN、仕訳、他Mob内蔵収納が未統合 | `isCompleteSettlementSource()`を偽のまま維持する | Source coverage review |

## Boundary

本Candidateは保存側に既に存在する村人内部収納の**読取専用技術修正**であり、他の全LivingEntityのInventoryHolderを一律に追加するものではない。特にAllayの保持アイテムと装備の別名関係、馬・Piglinなどの保管先の同一性は別途確認する。現段階では決算が完成しておらず、参加受付を開放しない。CAT Confirmedおよび形式Gateの合格を主張しない。
