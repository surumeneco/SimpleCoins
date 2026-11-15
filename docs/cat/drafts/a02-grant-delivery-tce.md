---
cat_version: '0.3'
kind: tce
id: simplecoins.grants.tce.delivery
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/pull/434
  - https://github.com/surumeneco/SimpleCoins/pull/437
decision_ref: ''
refs:
  - simplecoins.grants.pi.delivery
semantic_contract: markdown-v2
language: ja
---

# TCE: 保存直前・直後の配布中断と回復 (Draft)

網羅要求: open

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| p0 | 配布検査 | `入力.予約あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 偽; 出力.異常遮断 = 偽` |
| p1 | 配布検査 | `入力.予約あり == 真 かつ 入力.未ack == 真 かつ 入力.矛盾あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 偽` |
| p2 | 配布検査 | `入力.予約あり == 真 かつ 入力.矛盾あり == 真` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 真` |
| v0 | 保存後照合 | `入力.予約あり == 真 かつ 入力.未ack == 真 かつ 入力.保存済み現物受領対 == 真 かつ 入力.保存世代証拠有効 == 真 かつ 入力.他収納保全 == 真 かつ 入力.矛盾あり == 偽` | 1 | `出力.ack許可 = 真; 出力.予約維持 = 偽; 出力.異常遮断 = 偽` |
| v1 | 保存後照合 | `入力.予約あり == 真 かつ 入力.未ack == 真 かつ 入力.保存済み現物受領対 == 偽 かつ 入力.矛盾あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 偽` |
| v2 | 保存後照合 | `入力.予約あり == 真 かつ (入力.保存世代証拠有効 == 偽 または 入力.他収納保全 == 偽 または 入力.矛盾あり == 真)` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 真` |
| r0 | クラッシュ復旧 | `入力.予約あり == 真 かつ 入力.未ack == 真 かつ 入力.保存済み現物受領対 == 真 かつ 入力.保存世代証拠有効 == 真 かつ 入力.他収納保全 == 真 かつ 入力.矛盾あり == 偽` | 1 | `出力.ack許可 = 真; 出力.予約維持 = 偽; 出力.異常遮断 = 偽` |
| r1 | クラッシュ復旧 | `入力.予約あり == 真 かつ 入力.未ack == 真 かつ 入力.保存済み現物受領対 == 偽 かつ 入力.矛盾あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 偽` |
| r2 | クラッシュ復旧 | `入力.予約あり == 真 かつ (入力.保存世代証拠有効 == 偽 または 入力.他収納保全 == 偽 または 入力.矛盾あり == 真)` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 真` |

## 反例・未決
- 実Paperの合成NMS Player保存、保存無効とIOException黙殺、物理貨幣/PDCの単一保存、在庫満杯・他収納保全、未ack SIGKILL後の次起動読み戻しとack回復は [#434](https://github.com/surumeneco/SimpleCoins/pull/434) / [#437](https://github.com/surumeneco/SimpleCoins/pull/437) に証拠がある。実ネットワークログイン・切断・OS電源断ではない。
- 満杯、切断直前、書込み失敗と不完全な部分保全、ACK直後の再中断・同時操作などの細部は、このDraftの`網羅要求:open`の外側に残る。機械的Model Gateは未通過。認可済み仕様への昇格時には適切な時点・関連Processと状態同値類を正式に決める。
- `GrantLedger.acknowledge(grant, matchingSequence)`は引数の数値を信頼して永続化できるため、真の保存済現物は呼出し側で証明する必要がある。レジャー独立の単体成功を安全な本配布の保証にしない。
