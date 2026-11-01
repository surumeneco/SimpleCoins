---
cat_version: '0.3'
kind: pi
id: simplecoins.grant.ack.pi.request
status: draft
process: simplecoins.grant.ack
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: simplecoins.grant.ack.pi.request

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.grant | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.grant_ref | 受信 | 文字列 | 必須 |
| 入力.saved_receipt | 受信 | 整数[0..] | 必須 |
| 出力.acknowledged | 送信 | 真偽 | 必須 |
| 出力.error | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| ack | 受付 | 承認要求 |

## 制約
| 式 |
| --- |
| `入力.grant_ref != ""` |

## 境界と未決定
grant_refは仕様上の不透明な予約識別子であり、JavaのGrant型やファイル名をPIの値領域として固定しない。対象sequenceと不変フィールドの照合はProcess側の観測能力で行う。receiptのplayerdata durability、全履歴の完全性は未証明。CAT Draft、正式機械Gateはnot-run。
