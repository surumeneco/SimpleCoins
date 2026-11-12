---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-accounting-source-ingestion
status: candidate
process: simplecoins.settlement.hybrid-source-ingestion
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/342
refs: []
semantic_contract: markdown-v2
language: ja
---

# ロード済みChunk・オンラインPlayer貨幣の決算入力統合 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| live-snapshot | 選択したLIVE source IDと不変金額エントリの集合 |
| source-partition | `SavedLiveSourcePartition` の必須LIVEソースID |
| owner-lookup | BlockLocker/WWC/WWW/現在BANとplayerdata存否を確認してAssetへ分類する責務 |
| staging-transaction | `SettlementAccumulator` と保存側からの未確定集計値 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| live-stage-01 | 全LIVEソースの不変スナップショットと帰属判断が提供された | ソース別に物理slotを一意にステージし、すべての完了を記録する | 2Chunk/1Playerの金額集計 |
| live-stage-02 | 選択したLIVE Chunk/Playerのスナップショットが欠落 | 0Cを代入せずIOExceptionで失敗しcommitを拒否する | 未提出ソース |
| live-stage-03 | ソースIDが計画外・重複、保存側のIDを偽装 | 承認せずエラーとして扱う | unknown/duplicate検証 |
| live-stage-04 | 帰属解決がnull/一致しないsource/physicalKey/world/金額を返す | 部分結果を使用せずIOException | 無効分類の回帰 |
| live-stage-05 | 物理slotが2ソースで重複、または負の金額等を受け取る | セット全体を失敗として扱い確定しない | duplicate/corruption |
| live-stage-06 | 空の保管先が存在する | 実読取された空保管先は完了を記録できるが、保管先未読取を空とはみなさない | 空ソースと欠落ソースの差 |
| live-stage-07 | コードがLiveソースの完了を記録した | 保存の同一時点性・登録/Owner/BAN正当性・仕訳との同期・運用開始を保証しない | admissionを開かない |

## Boundary

実データの列挙と金額評価はPaper main-thread adapter/保存NBT readerが別途実行する。所有者は本クラスで推定せず、明示のauthoritative resolverが提供する必要がある。すべてのSourceが読まれたという偽の報告は外部から完全に防げないため、production coordinatorの世代検証が必須。CAT CandidateでありConfirmedではない。
