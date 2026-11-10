---
cat_version: '0.3'
kind: tce
id: simplecoins.paper.tce.trader-drop
status: draft
process: simplecoins.paper.trader-drop-selection
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: 
  - simplecoins.paper.pi.trader-drop
semantic_contract: markdown-v2
language: ja
---

# TCE: Trader drop selection (Draft)

網羅要求: closed

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| td00 | 行商人死亡 | `入力.enabled == false` | 1 | `出力.issued = false` |
| td01 | 行商人死亡 | `入力.enabled == true` | 1 | `出力.issued = true` |

td01は発生確率1.0の場合にのみ必ずtrueとなる。それ以外の確率・重み・枚数は固定seedの独立テストで検証する。正式CAT網羅完了の主張ではない。
