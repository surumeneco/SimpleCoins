---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.receipt-replay-observation
status: candidate
process: simplecoins.grant
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/f7ebdc33227eb279d44461e9d7e6bf2d86598b23/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
  - https://github.com/surumeneco/SimpleCoins/blob/f7ebdc33227eb279d44461e9d7e6bf2d86598b23/src/test/java/co/surumene/simplecoins/domain/GrantLedgerTest.java
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# GrantLedger receipt / replay — code-to-spec Candidate

## 観測対象と権限

本書は `TASK-20261101-005` の未達AC（二重計上・障害復旧のfail-closed）に関する、**既存コードからの観測**である。対象は `surumeneco/SimpleCoins@f7ebdc33227eb279d44461e9d7e6bf2d86598b23` の `GrantLedger` に限定する。実装されている事実と、その振る舞いを将来採用すべきかの判断を分離する。

- **Observation**: 下表はソースの分岐条件・永続書込み順と既存テスト名を照合したもの。既存テストの全件を本作業で実行したとの主張ではない。
- **Candidate**: 受入仕様に使う場合、receiptと実Inventoryの保存順序、外部プロセス間のwriter権限、クラッシュ後の再試行方針を規範的に決め直す必要がある。
- **Authority**: `status: candidate`。Confirmed Process/PI/TCE、Process Closure、Model Conformance、Implementation Conformance、Paper実機やJE/BEの合格を意味しない。`production`と既存JUnitは本工程では変更しない。

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.grant.receipt-validation | 現行 `validateReceipt` / `wasApplied` の保存済み証跡照合 |
| simplecoins.grant.acknowledgement | 現行 `acknowledge` の永続化と再起動時の適用判定 |

## 規則

以下は既存コードの観測Candidateであり、承認済み要求を意味しない。

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| GR-REPLAY-01 | `validateReceipt(player, receipt)` で負数、または正のreceiptが他プレイヤーのgrantか存在しない番号を指す | `IllegalStateException` を投げ、他人の保存receiptを配布済みの根拠にしない | `validateReceipt`、`incompleteOrOtherPlayersReceiptCannotAcknowledgeGrant` |
| GR-REPLAY-02 | 指定playerに `delivered=true` のgrantがあり、そのsequenceが保存receiptを超える | rollbackとして `IllegalStateException`。過去の配布完了を無かったことにしない | `validateReceipt`、`acknowledgedPlayerdataRollbackFailsClosed` |
| GR-REPLAY-03 | 保存receiptより小さいsequenceに未配布grantが存在する | 未配布を飛び越えたreceiptを `IllegalStateException` で拒否する | `validateReceipt`、`aLaterReceiptCannotSkipAnEarlierUndeliveredGrant` |
| GR-REPLAY-04 | 保存receiptと未配布grantのsequenceが等しい | `wasApplied` は真正なreceipt照合後にtrueになり得る。クラッシュ直前のInventory保存済み・ack未実施の場合を区別する | `wasApplied`、`crashAfterPlayerSaveBeforeAcknowledgementDoesNotRequireReissuance` |
| GR-REPLAY-05 | `acknowledge` の引数が現在のsequence/UUID、または有効な保存receiptと一致しない | `IllegalStateException`、永続配布完了書込みに進まない | `acknowledge`、`incompleteOrOtherPlayersReceiptCannotAcknowledgeGrant` |
| GR-REPLAY-06 | 正しく確認できたgrantをacknowledgeした後、再度同じgrantをacknowledgeする | 二回目は完了済みレコードとして早期returnし、複製のgrantを発行しない | `acknowledge`、`reservationSurvivesRestartUntilPlayerReceiptConfirmsDelivery` |
| GR-REPLAY-07 | 未配布grantをacknowledgeする | 先に `DurableFile.write` で完了レコードを永続化し、その後メモリMapの値を置換する。書込み失敗はhealthy状態を無効化する | `persist(complete)` と `grants.put` の順序、`checkHealthy` |
| GR-REPLAY-08 | `pending`、`wasApplied`、`validateReceipt`、`acknowledge` などの実操作 | 先行する `checkHealthy` がdurable headと全既存grantレコードを検査。過去レコードの欠損・変更を検知すると以後の操作を拒否する | `checkHealthy`、`GrantLedgerNonTailIntegrityTest` |

## 未決定・明確に証明されないこと

| Question ID | 規範判断・実機での独立検証が必要な境界 |
| --- | --- |
| GR-Q-01 | Inventory保存とplayerdata receipt保存の原子性・順序・中断位置。単なるjournal APIの単体テストを現実の物理貨幣のexactly-once達成とみなさない。 |
| GR-Q-02 | 同一台帳rootを複数JVM/プロセスが開く場合の排他権限・復旧。既存 `docs/cat/phase6-grant-multiwriter-exclusivity.md` の独立Draftに委ね、本候補で自動確定しない。 |
| GR-Q-03 | 一つのreceiptしか持たないplayerの複数grant・異常時に、未配布・配布済・保存済みの順序をどこまで外部状態で証明できるか。既存成功JUnitは外部Playerdata同時保存の完全証明ではない。 |
| GR-Q-04 | `checkHealthy` が各操作で全ファイルを読むO(n)設計のTPS/I/O・故障時停止方針。性能最適化や書込み方式変更は別の仕様判断・CAT工程へ渡す。 |

## このCandidateの検証境界

- ソースとJUnitテストの内容は、GitHubコネクタで指定HEADの原文を取得して照合。別HEADへ適用する際は現行GitHub差分を再取得する。
- 本工程で実行するのは固定CAT-TDDルーティング・GuardとArtifactの構文lintであり、JUnit/実機テスト・規範CAT Gate・develop統合の代替にはしない。
- 特に「既存実装の振る舞い」を「ユーザーが承認した仕様」へ昇格しない。承認済みの意味と食い違えばNormative Authorityへの確認・Issue化が必要。
