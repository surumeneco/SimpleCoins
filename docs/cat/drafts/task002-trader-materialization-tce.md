---
cat_version: '0.3'
kind: tce
id: simplecoins.paper.tce.trader-stack
status: draft
process: simplecoins.paper.trader-stack-materialization
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.trader-stack
semantic_contract: markdown-v2
language: ja
---

# TCE: 行商人スタック分割（Draft）

網羅要求: partial

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| split-64 | 抽選結果の確定 | 枚数64かつ予算1 | 1 | 64枚入り1スタック |
| split-65 | 抽選結果の確定 | 枚数65かつ予算2 | 1 | 64枚＋1枚 |
| over-budget | 抽選結果の確定 | 必要スタック数が予算超過 | 0 | スタックを公開しない |

これは技術試験ケースのDraft。正式な網羅証明、仕訳/イベント受入には使用しない。
