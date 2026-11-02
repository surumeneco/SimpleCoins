---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.issuance-journal-offline-verification
status: candidate
process: simplecoins.phase7
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/DurableFile.java
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: 発行台帳のオフライン読取検証（code-to-spec Candidate）

## 境界

既存の `DurableFile` / `GrantLedger` の固定ソースから復元したチェックサム・SCG1/SCG2・連番・headの検査条件を、**読み取り専用の診断器**に適用する。これは発行台帳の修復器でも、Paperの配布一回性を証明するテストでもない。診断器は台帳を変更しない。プレイヤーUUID、発行者、予約IDを標準出力に表示しない。

## 検証ベクトル

| ID | 入力 | 期待 |
| --- | --- | --- |
| ISS-DIAG-01 | SHA-256 prefix付き正規SCG1/SCG2、連続した予約連番とhead | 予約件数・未完了件数・headを返す |
| ISS-DIAG-02 | チェックサム不一致、切断、余分なバイト、不正スキーマ・額面 | エラー。件数を正常な発行結果と扱わない |
| ISS-DIAG-03 | 欠番・非正規ファイル名・重複予約ID | エラー。連番の重複・欠落を隠さない |
| ISS-DIAG-04 | headが記録末尾より進んでいる／headがない | エラー |
| ISS-DIAG-05 | headが記録末尾より遅れている | Java起動時の回復可能性を警告し、クリーンな状態とは報告しない |
| ISS-DIAG-06 | 台帳ディレクトリ・head・記録がシンボリックリンク | 外部パスを辿らず拒否 |
| ISS-DIAG-07 | 一時ファイルが残る／読み取り中に変更される | 一時ファイルは警告、変更検知はエラー。静的スナップショットを保証したと主張しない |

## 未検証・制限

診断器の成功はディスク上の記録形式の局所整合のみ。保存済みplayerdata受領PDC、インベントリ、稼働中のメモリ、同期書込の停電耐性、実際のPaper/Geyser/JE/BE配布、ユーザーの受入は**未検証**。稼働中のサーバーディレクトリを検査した場合の同時更新を排除できないため、停止済みサーバーのバックアップコピーで実行する。CAT CandidateはConfirmedではなく、正式semantic Gateも未実施。
