---
cat_version: '0.3'
kind: pi
id: simplecoins.give.admin-request.pi.give
status: confirmed
process: simplecoins.give.admin-request
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#発行・照会・配布予約
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 発行受付可否

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.commands | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.発行権限 | 受信 | 真偽 | 必須 |
| 入力.決算確定 | 受信 | 真偽 | 必須 |
| 入力.対象確定 | 受信 | 真偽 | 必須 |
| 入力.指定有効 | 受信 | 真偽 | 必須 |
| 出力.結果 | 送信 | 列挙{拒否, 未確定, 不正, 受付可} | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| give | 受付 | 発行受付 |

## 制約
| 式 |
| --- |

対象確定は一意の既知UUID（セレクタの展開結果は複数UUIDでもよい）、アイテム形式なら全対象オンラインであることを指す。指定有効は正の整数、既知のアイテム名、スタック分割と全対象に対する実行可能な配布計画を指す。
