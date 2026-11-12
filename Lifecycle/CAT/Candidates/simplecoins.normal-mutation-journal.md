---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.normal-mutation-journal
status: candidate
process: simplecoins.normal-mutation-journal
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/blob/51e134d860de8f937f731832d97db24b15f16b15/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
  - https://github.com/surumeneco/SimpleCoins/blob/51e134d860de8f937f731832d97db24b15f16b15/src/main/java/co/surumene/simplecoins/domain/DurableFile.java
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# 通常仕訳の永続記録：独立技術Candidate

本Artifactは02Bの「確定した資産変化を重複なく仕訳」「障害時に誤って集計を確定しない」に対する技術選択を試す。**CAT Confirmed、決算本番受入、技術方式の最終承認ではない。** 外部イベントの真のコミット時点、統合先、チェックポイントの方式は未確定であり、Paperアプリ本体にはまだ接続しない。

## 対象

| Ref | Aspect |
| --- | --- |
| normal-journal | 保存された単一操作の送金・発行・消失・所有者再分類 |
| durablefile | 既存の単一レコードチェックサム・同期書込・排他作成 |
| admission | 起動時全件決算と永続仕訳照合が完了するまでは参加禁止 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| journal-01 | 正額、異なる二つの口座 | PREPAREDを先に永続化し、連番に隙間がない | 受付後の再起動と連番検証 |
| journal-02 | 同じ操作ID・同一内容を再受付 | 元の仕訳を返し、二重記録しない | 重複入力でファイル数不変 |
| journal-03 | 同じIDに異なる内容を再受付 | 衝突を拒否する | 不一致IDの試験 |
| journal-04 | Paperが現物の変更を報告 | EVENT_APPLIEDへ更新し、それでも現物永続化済みとは見なさない | 再起動後の未照合記録 |
| journal-05 | 記録破損・抜け・head不整合・競合書込 | fail-closedで書込停止する | 欠損、チェックサム、単一writer lock |
| journal-06 | 発行・消失・保有者移転 | 仮想EXTERNAL口座を識別し、資産変化を正額で記録する | positive/negative/zero supply delta |

## Observation

`develop@51e134d8`の`DurableFile`と`GrantLedger`は発行予約向けで、移動・消失・帰属変更に横断的に適用する通常仕訳エンジンはない。技術Candidateは、先にPREPAREDを永続化し、Paperが変更を報告したらEVENT_APPLIEDへ更新する。**どちらの状態も次の起動時全件決算で現物照合が必要**である。

## 検証境界・未決定

- EVENT_APPLIEDは実ワールドに保存済みである証明ではない。正常停止・クラッシュ・他プラグインによる変更を含む正本走査は別Work。
- ログレコードに含める現物証拠、チェックポイント・圧縮、全イベントのoperation ID生成、Paperとjournalの原子性は未確定。
- この単位ではDomain実装とJUnitに限定し、入室再開・Traderドロップ・Give・決算の解禁を行わない。
- 正規CAT Process Closure / Model Conformance / TDD Queue / Implementation Conformanceの合否は未実施。