---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.untracked-record-set-closure
status: candidate
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/blob/ebeac1e508a17164c7431ea986ab1bdb89e5576b/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
  - https://github.com/surumeneco/SimpleCoins/blob/ebeac1e508a17164c7431ea986ab1bdb89e5576b/src/test/java/co/surumene/simplecoins/domain/GrantLedgerUntrackedNextRecordCandidateTest.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# GrantLedger 未追跡 `.grant` 集合の整合検査 Candidate

## 観測

稼働中の `checkHealthy()` は `head` とインメモリで把握した各 `.grant` の内容を検査するが、ディレクトリに後から追加された未追跡 `.grant` の存在を列挙しない。次の採番と一致する場合に限り `writeNew` が上書きを拒否する既存テストがある。未追跡の遠い将来の採番・不正ファイル名がある場合、現行の `pending` や `reserve` はそれらのファイルを検査しない。

## 対象

| Ref | Aspect |
| --- | --- |
| journal-in-memory | 稼働中GrantLedgerが保持する連番の記録集合 |
| journal-directory | 同じディレクトリにある実際の `.grant` ファイル集合 |

## 規則

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| GRANT-SET-01 | 起動後に未追跡 `99.grant` が追加された | `pending` と新規予約は整合性異常として拒否。元ファイルは上書きしない | unexpected-future-record |
| GRANT-SET-02 | 起動後に非正規名 `unexpected.grant` が追加された | 同様に fail-closed | noncanonical-record |
| GRANT-SET-03 | 既知 `.grant` 集合と現在の記録が一致する | 既存の予約・照会・再起動の挙動を維持 | ordinary-replay |
| GRANT-SET-04 | 未追跡ファイルを検知した | 稼働中インスタンスを failed にし、再読み込み前の操作は拒否 | failure-sticky |

## 境界

検出は `.grant` ファイル名の存在集合に限定する。`head` や無関係な運用ファイルを新たな貨幣レコードとは認識しない。既存ファイル本文の SHA-256 検証・連番確認・次レコードの排他的作成は維持する。本修正だけでは複数JVM間の書込ロック、同一瞬間のファイル集合の原子的凍結、プレイヤーdata側の exactly-once まで保証しない。

この文書は現行コード由来の CAT Candidate であり、Confirmed への自動昇格や正式CATゲート通過を主張しない。
