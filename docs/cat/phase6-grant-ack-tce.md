---
cat_version: '0.3'
kind: tce
id: simplecoins.grant.ack.tce.target
status: draft
process: simplecoins.grant.ack
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs:
  - simplecoins.grant.ack.pi.request
semantic_contract: markdown-v2
language: ja
---

# TCE: grant acknowledgement durable-target guard

網羅要求: partial

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| ack.target.missing | 承認要求 | `状態.健全 == 真 かつ 観測.対象durable == "missing"` | 1 | `出力.acknowledged = 偽; 出力.error = 真; 状態.健全 = 偽` |
| ack.target.corrupt | 承認要求 | `状態.健全 == 真 かつ 観測.対象durable == "corrupt"` | 1 | `出力.acknowledged = 偽; 出力.error = 真; 状態.健全 = 偽` |
| ack.target.mismatch | 承認要求 | `状態.健全 == 真 かつ 観測.対象durable == "mismatch"` | 1 | `出力.acknowledged = 偽; 出力.error = 真; 状態.健全 = 偽` |
| ack.target.valid | 承認要求 | `状態.健全 == 真 かつ 観測.対象durable == "intact" かつ 観測.receipt適合 == 真` | 1 | `作用.承認記録 = 真; 出力.acknowledged = 真; 出力.error = 偽` |
| ack.receipt.invalid | 承認要求 | `状態.健全 == 真 かつ 観測.対象durable == "intact" かつ 観測.receipt適合 == 偽` | 1 | `出力.acknowledged = 偽; 出力.error = 真` |
| ack.unhealthy | 承認要求 | `状態.健全 == 偽` | 1 | `出力.acknowledged = 偽; 出力.error = 真` |

## 網羅と未決定
既存SC-JOURNAL-ACK-01〜03をDraftで表した部分仕様。durable write failure、initial health、grant_refの解決失敗、receipt整合の詳細、二重承認の全組合せは未閉包であり、網羅要求をclosedとしない。古い対象recordを再読込しない現行実装はこのTCEの達成証拠ではない。正式CAT machine Gatesはnot-run、Confirmed・実装許可・マージを意味しない。
