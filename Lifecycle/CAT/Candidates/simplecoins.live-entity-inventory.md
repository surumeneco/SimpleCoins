---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-entity-inventory
status: candidate
process: simplecoins.settlement.loaded-chunk-assets
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/332
refs: []
semantic_contract: markdown-v2
language: ja
---

# ロード済み運搬EntityのInventory貨幣抽出 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| inventory-holder | 非LivingEntityかつInventoryHolderである運搬体（例 StorageMinecart） |
| physical-source | World名・Entity UUID・Inventory slot番号 |
| storage-value | 実ItemStackの安定PDC ID／残高、入れ子を含む価値 |
| identity | チャンク全体で共有する財布・Entity UUID・物理slotの一意性 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| holder-01 | ロード済み収納運搬Entityが貨幣・財布を保持 | InventoryHolder#getInventoryから全スロットを一度だけ抽出する | 公式Paper StorageMinecart fixture |
| holder-02 | 同じ財布UUIDが別EntityまたはContainerにある | 共有UUIDで拒否し二重加算しない | コピー財布fixture |
| holder-03 | Inventoryが欠損、内容とsize不一致、slot予算超過 | 決算を失敗させ、部分結果を確定しない | 欠損/予算検査 |
| holder-04 | PlayerやLivingEntityがInventoryHolderも実装している | Player/装備との別名集計を避けるためこの経路で再集計しない | 型境界検査 |
| holder-05 | 未ロードデータ・保護／Mob固有保管／所有者帰属等 | 全件決算として確定しない | isCompleteSettlementSource=false |

## Observation

PR #331はItem Entity、ItemFrame/ItemDisplay、LivingEntity装備を含むが、非LivingEntityのInventoryHolder（収納トロッコ等）は`unhandledOtherEntities`として計数されるのみ。02A/02Bは実在する貨幣を保管者種類にかかわらず全件走査する仕様を定める。従来`PaperLoadedChunkAssets`のEntity UUID/財布識別集合を共有したまま所持Inventoryを読取る技術候補。

## Boundary

本Candidateは読取専用・非同期不可のPaper実装。LivingEntity内部収納、Player、Body/Saddle装備、未ロード保存・保存/ライブ二重計上、BlockLocker/BAN/WWC/WWW帰属、ジャーナル・同一時点性・入室解禁を対象外とする。CAT CandidateからConfirmedへは自動昇格させない。
