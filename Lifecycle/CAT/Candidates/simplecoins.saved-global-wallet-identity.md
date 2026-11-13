---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.saved-global-wallet-identity
status: candidate
process: simplecoins.settlement.saved-sources
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/327
refs: []
semantic_contract: markdown-v2
language: ja
---

# 保存済み領域を横断した財布UUID唯一性 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| saved-player | Inventory・EnderItems・equipmentを含むplayerdata |
| saved-block | BlockEntityのコンテナ、飾り壺、入れ子アイテム |
| saved-entity | Item/drop、ItemFrame/Display、装備、その他のEntity Inventory |
| identity-context | 決算走査開始時に作られ、全保存ソースを横断するSet<UUID> |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| global-wallet-01 | 同じ財布UUIDがplayerdataと保護／非保護コンテナに複製 | 源別金額の合算を許可せずIOExceptionで全決算拒否 | GZIP playerdata＋MCA region fixture |
| global-wallet-02 | 異なる保存ワールド・Entity・額縁・コンテナ・playerdataに同じ財布UUIDがある | ただ一つのglobal identity集合を使用して検出 | 抽出器横断単体テスト |
| global-wallet-03 | 別の財布UUIDに同じ残高がある | 金額とIDを混同せず両方計上する | 正常fixture |
| global-wallet-04 | 抽出途中に例外、発見源不一致・未完了が発生 | 部分結果を公開せず新しい決算世代では集合を再生成 | SettlementScanRunnerの失敗境界 |
| global-wallet-05 | loaded Paper状態と保存ソースが重なっている | 本保存側一意性だけではライブ重複排除を証明しない | 後続同時点整合試験 |

## Observation

PR #327の`SettlementScanRunner`は複数playerdata間でのみ`seenPlayerWallets`集合を共有する。しかし`StoredBlockEntityAssets`、`StoredEntityEquipmentAssets`等の保存抽出器は各`StoredItemValue.value(item)`で別集合を生成し、異なる保存ソースへコピーされた財布を二重計上できてしまう。

## Boundary

本Candidateは保存済みNBTの決算入力に関する技術的不変条件であり、CAT Confirmedではない。ロード済みPaper実体との選択、BAN/BlockLocker/WWC/WWW帰属、通常仕訳、同一時点性を保証せず、入室許可の根拠にはしない。
