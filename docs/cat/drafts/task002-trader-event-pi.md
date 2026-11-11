---
cat_version: '0.3'
kind: pi
id: simplecoins.paper.pi.trader-event
status: draft
process: simplecoins.paper.trader-event
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: ''
refs:
  - simplecoins.paper.trader-event
semantic_contract: markdown-v2
language: ja
---

# PI: 行商人発行イベント (Draft)

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.trader-drop-selection | 双方向 |
| simplecoins.paper.trader-stack-materialization | 双方向 |
| simplecoins.paper.journal-settlement | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.UUID | 受信 | 行商人固有ID | 必須 |
| 入力.死亡イベント | 受信 | EntityDeathEvent | 必須 |
| 出力.発行レコード | 送信 | 永続発行処理 | 条件付き |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| onDeath | 受付 | 行商人死亡 |

## 制約
| 式 |
| --- |

死亡イベントはPaperメインスレッドで処理する。金銭処理が保護不能な場合はfail-closed。出力レコードと実ワールドの保存の同時確定は追加検証まで未成立。
