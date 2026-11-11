---
cat_version: '0.3'
kind: pi
id: simplecoins.paper.pi.trader-stack
status: draft
process: simplecoins.paper.trader-stack-materialization
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 行商人ドロップのスタック準備（Draft）

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.trader-drop-selection | 受信 |
| simplecoins.paper.trader-journal-commit | 送信 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.額面 | 受信 | 貨幣額面 | 必須 |
| 入力.枚数 | 受信 | 正整数 | 必須 |
| 入力.許容スタック数 | 受信 | 正整数 | 必須 |
| 出力.スタック | 送信 | スタック列 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| materialize | 受付 | 抽選結果の確定 |

## 制約
| 式 |
| --- |

全スタックを準備後、成功時のみ呼び出し側へ渡す。メモリ確保失敗、予算超過、PDC生成失敗は外部への部分発行を禁じる。journal確定処理は別工程。
