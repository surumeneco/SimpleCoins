---
cat_version: '0.3'
kind: pi
id: simplecoins.accounting.admin-query.pi.read
status: confirmed
process: simplecoins.accounting.admin-query
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#発行・照会・配布予約
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 管理照会可否

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.accounting | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.管理権限 | 受信 | 真偽 | 必須 |
| 入力.確定集計 | 受信 | 真偽 | 必須 |
| 入力.照会種別 | 受信 | 列挙{統計, 個人, 死蔵} | 必須 |
| 入力.登録済 | 受信 | 真偽 | 必須 |
| 出力.結果 | 送信 | 列挙{拒否, 未確定, 未登録, 表示} | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| read | 受付 | 管理照会 |

## 制約
| 式 |
| --- |

登録済は確定集計内のUUIDの登録を指し、残高0でも真。統計・死蔵の場合は登録済の値は結果に影響しない。
