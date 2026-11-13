---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-cross-world-source-recheck
status: candidate
process: simplecoins.settlement.paper-live-source-coverage
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/348
refs: []
semantic_contract: markdown-v2
language: ja
---

# 複数World・Player横断後のロード済みChunk集合再照合 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| world-sources | 選択済み各Paper Worldのロード済みChunk座標集合 |
| universe-capture | すべてのWorldとオンラインPlayerのLIVE走査結果 |

## 観測

一つのWorldの走査終了時点でそのWorldのロード済みChunk座標集合が変わっていないことは、PR #348で検査される。しかし次のWorldやオンラインPlayerのインベントリを走査している間に、先行Worldのロード済みChunk集合が変化すると、同一のPaper LIVE capture全体では不完全な世代になり得る。

## 規則

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| universe-set-01 | すべての選択Worldが完全走査後にも同じChunk座標集合を持つ | LIVE capture候補を返してよい | 複数Worldの不変集合 |
| universe-set-02 | 後続World/Playerの走査中に先行WorldのChunkが増減・同数で差し替わる | LIVE capture候補をIOExceptionで拒否 | 先行Worldの遅延変更 |
| universe-set-03 | 再照合でnull、未ロード、別World、重複座標を観測する | 金額0のWorldと推定せず拒否 | 不正な再照合結果 |
| universe-set-04 | WorldScopeによって全Worldが除外され、Playerだけ走査する | 空のWorld集合も候補として許容 | 空集合 |
| universe-set-05 | 再照合が成功する | source時刻一致、Chunkの中身の不変、保存NBT・外部保護/BAN/WWC/WWW、通常仕訳の整合まで証明したとは扱わない | admission閉維持 |

## 実装境界

各WorldのPaper scanの同一パスで取得したChunk座標を記録し、最後のオンラインPlayer取得後に全Worldの現在のロード済み座標集合と照合する。追加Chunkロードや保存を引き起こさず、メインスレッドの読取だけで実施する。現在のスキャン処理、貨幣の価値、所有者、Nexo/JE/BEなどの仕様を変更しない。

本書はcode-to-specのCAT Candidateであり、AIによるConfirmedへの昇格、正式Gate通過の宣言、公開決算/参加許可の有効化は行わない。
