---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.wallet.uuid-duplicate-detection
status: candidate
process: simplecoins.settlement.wallet-identity
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/325
  - https://github.com/surumeneco/SimpleCoins/blob/7fb79e102da2c8e4ecc872a33b36992696084c8d/src/main/java/co/surumene/simplecoins/domain/StoredItemValue.java
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# 財布UUIDの局所的重複検出 Candidate

02Bでは同一物理資産の二重計上禁止・正本から全件決算を要求する。一方、StoredItemValue.valueは呼び出し単位で財布残高を計算するため、同一UUIDで複製された財布を複数のスロットへ配置した時に同じ金額を加算し得る。これは実機前に防ぐべき技術欠落。**本Candidateは仕様の正式Confirmedではない**。

## 対象

| Ref | Aspect |
| --- | --- |
| saved.wallet-id | minecraft:custom_data.PublicBukkitValues.simplecoins:wallet_id |
| saved.playerdata | SavedPlayerAssets.extractのInventory/EnderItems/equipment |
| live.inventory | PaperLiveItemValue.inventoryと内部に含まれるItemStack |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| duplicate-01 | 同じUUIDの財布が同一の入れ子コンテナ中に2つ | 重複をIOExceptionとして中止し、二重計上しない | nested/shulker fixture |
| duplicate-02 | 同じUUIDが同一playerdataの通常InventoryとEnderItems・equipmentに出現 | 1つの保存ソース全体を失敗させる | saved player fixture |
| duplicate-03 | 同じUUIDが1つのライブInventoryの異なるスロットに出現 | Paper adapterを失敗させる | official Paper26.2 probe |
| duplicate-04 | 別UUIDの財布は独立資産 | 既存価値合計を変えない | 2財布と貨幣fixture |
| duplicate-05 | 全ワールドにまたがる複製財布・Saved/Live併存 | さらに高位の共通生成世代トラッカーが必要 | G0後段・未実装 |

## 境界

- 本変更は明示的に指定された1 inventory/1 playerdataソースと、その子コンテナの**局所的な重複検出**。外部の無関係なチャンク、全オンラインプレイヤー、saved/live統合世代の重複排除は別工程。
- 検査の重複集合に不正入力由来の途中状態が残った場合は呼出元が結果を破棄し、再利用しない。
- formal CAT Process Closure / Model Conformance / Implementation Conformanceと本番配備・JE/BE実操作は未実施。