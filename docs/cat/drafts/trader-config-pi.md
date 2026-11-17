---
cat_version: '0.3'
kind: pi
id: simplecoins.paper.pi.trader-config
status: draft
process: simplecoins.paper.trader-drop-config
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: ドロップ設定読込（Draft）

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.trader-drop-selection | 送信 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.有効 | 受信 | 真偽値 | 必須 |
| 入力.確率 | 受信 | 実数 | 必須 |
| 入力.重み | 受信 | 額面別重み | 必須 |
| 出力.設定 | 送信 | 読込済み設定 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| load | 受付 | 設定読込 |

## 制約
| 式 |
| --- |

数値文字列の暗黙変換は行わず、異常設定を黙って運用に投入しない。
