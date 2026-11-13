---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-entity-equipment
status: candidate
process: simplecoins.settlement.loaded-chunk-assets
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/330
refs: []
semantic_contract: markdown-v2
language: ja
---

# PaperライブLivingEntityの装備スロット抽出 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| living-entity | ロード済みChunk内のPlayer以外のLivingEntity |
| equipment | 主手、副手、頭、胸、脚、足の6スロット |
| physical-source | World・Entity UUID・装備部位からなるキー |
| shared-wallet | 他のContainer・Item・Displayと共有する財布UUID集合 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| equipment-01 | Mob等の装備に貨幣・財布がある | 装備部位ごとに一度だけ価値を読み取る | 実PaperArmorStandの装備プローブ |
| equipment-02 | 同じ財布UUIDが別ソースにもある | 二重加算を行わず全体走査を拒否 | 同一財布を装備とContainerに置く |
| equipment-03 | 空装備または非貨幣 | 金額0だが読取予算に6スロットを計上 | slot budgetテスト |
| equipment-04 | Playerの装備、NPC固有のInventory、body armor、saddle、外部プラグイン所有者 | 本実装だけで完全決算を宣言しない | coverage markerとfail-closed境界 |

## Observation

PR #330のライブ抽出対象は物理コンテナ、落下Item、ItemFrame、ItemDisplayであり、SavedEntityEquipmentAssetsが既に対象にするMob装備は含まない。現行のロード済みEntityの装備を放置すると保存/ライブ間の正本が不整合となり得る。Playerは別のplayerdata/ライブPlayer管理に属するためこの経路から除外する。

## Boundary

本Candidateは読取専用の実装選択であり、完全な正本取得・既存データとの同時点性・所有者帰属・BAN確認・通常仕訳・JAR運用承認・CAT Confirmedを意味しない。6標準装備以外のbody/saddle等は後続検証を要する。読み取れない装備、破損PDC、スロット予算不足は全体拒否とし、処理中に生成した部分集計は公開しない。
