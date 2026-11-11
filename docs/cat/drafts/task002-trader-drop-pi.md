---
cat_version: '0.3'
kind: pi
id: simplecoins.paper.pi.trader-drop
status: draft
process: simplecoins.paper.trader-drop-selection
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: Wandering trader drop draw (Draft)

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.wandering-trader-event | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.enabled | 受信 | 真偽値 | 必須 |
| 入力.probability | 受信 | 実数 | 必須 |
| 出力.issued | 送信 | 真偽値 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| draw | 受付 | 行商人死亡 |

## 制約
| 式 |
| --- |

重みが正の候補額面だけを抽選する。抽選結果は無発生または1種類の額面と正の枚数。乱数を注入し再現可能にする。現物生成と仕訳実行は別Work。
