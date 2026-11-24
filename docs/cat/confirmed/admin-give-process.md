---
cat_version: '0.3'
kind: process
id: simplecoins.give.admin-request
status: confirmed
process: simplecoins.give.admin-request
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#発行・照会・配布予約
refs: 
  - simplecoins.give.admin-request.pi.give
semantic_contract: markdown-v2
language: ja
---

# Process: 管理者の貨幣発行受付

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.give.admin-request.pi.give |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 発行受付 | simplecoins.give.admin-request.pi.give/give |

発行権限を持つ送信者のみ、決算確定後、対象UUIDと正の整数による発行指定が有効な場合に発行受付を試行する。金額形式は最小枚数に分解し、既知オフラインUUIDには予約として記録する。アイテム名と個数の形式はオンラインのみ。額面を固定し64枚以下のスタックへ分割する。各対象へ全指定額または全指定個数を渡す。受付可否は実物の生成・配布・保存完了を保証しない。実体化と保存確認はA02、通常統計の更新は確定仕訳が担当する。
