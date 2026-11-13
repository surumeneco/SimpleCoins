---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-player-holder-dedup
status: candidate
process: simplecoins.settlement.live-chunk-source-selection
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/335
  - https://github.com/surumeneco/SimpleCoins/pull/366
refs: []
semantic_contract: markdown-v2
language: ja
---

# オンラインPlayerのInventoryHolder二重読取を防ぐ技術Candidate

## 観測
現行developのPaperLoadedChunkAssetsは、PlayerをLivingEntityの装備走査から除外するが、次のInventoryHolder分岐でplayer.getInventory()を読み直し得る。PaperLivePlayerAssetsのPlayer専用正本と二重計上になり、LIVE同一世代の整合を失う。

## 規則
| ID | 条件 | 期待する振る舞い | 検証 |
| --- | --- | --- | --- |
| live-holder-01 | EntityがPlayerまたはその他のLivingEntity | Chunk InventoryHolder分岐では読まず、Player専用経路またはEntity装備経路のみ使う | 型判定JUnit |
| live-holder-02 | Entityが非LivingEntityのInventoryHolder（StorageMinecart等） | 既存の限定的な物理Inventory走査を維持する | 型判定JUnit |
| live-holder-03 | EntityがInventoryHolderではない | Inventory経路で資産を生成しない | 型判定JUnit |
| live-holder-04 | 全件決算との接続やPaper実機がまだない | 本修正を全件決算・参加許可の完成として扱わない | 境界レビュー |

## 境界
既存仕様から導いたcode-to-spec Candidateであり、ConfirmedやProcess/Model/Implementation Conformanceの達成を意味しない。オンラインPlayerのChunk Entityリストへの実際の露出とPaper実機の検証は別作業。
