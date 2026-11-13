---
cat_version: '0.3'
kind: tce
id: simplecoins.paper.tce.exchange
status: draft
process: simplecoins.paper.coin-exchange
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.exchange
semantic_contract: markdown-v2
language: ja
---

# TCE: 20 value-equivalent exchanges (Draft)

網羅要求: closed

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| x00 | 等価両替 | `入力.ruleId == "n1-f1"` | 1 | `出力.inValue = 5; 出力.outValue = 5` |
| x01 | 等価両替 | `入力.ruleId == "n1-f2"` | 1 | `出力.inValue = 5; 出力.outValue = 5` |
| x02 | 等価両替 | `入力.ruleId == "n1-f3"` | 1 | `出力.inValue = 10; 出力.outValue = 10` |
| x03 | 等価両替 | `入力.ruleId == "n1-f4"` | 1 | `出力.inValue = 10; 出力.outValue = 10` |
| x04 | 等価両替 | `入力.ruleId == "n1-f5"` | 1 | `出力.inValue = 20; 出力.outValue = 20` |
| x05 | 等価両替 | `入力.ruleId == "n10-f1"` | 1 | `出力.inValue = 50; 出力.outValue = 50` |
| x06 | 等価両替 | `入力.ruleId == "n10-f2"` | 1 | `出力.inValue = 50; 出力.outValue = 50` |
| x07 | 等価両替 | `入力.ruleId == "n10-f3"` | 1 | `出力.inValue = 100; 出力.outValue = 100` |
| x08 | 等価両替 | `入力.ruleId == "n10-f4"` | 1 | `出力.inValue = 100; 出力.outValue = 100` |
| x09 | 等価両替 | `入力.ruleId == "n10-f5"` | 1 | `出力.inValue = 200; 出力.outValue = 200` |
| x10 | 等価両替 | `入力.ruleId == "n100-f1"` | 1 | `出力.inValue = 500; 出力.outValue = 500` |
| x11 | 等価両替 | `入力.ruleId == "n100-f2"` | 1 | `出力.inValue = 500; 出力.outValue = 500` |
| x12 | 等価両替 | `入力.ruleId == "n100-f3"` | 1 | `出力.inValue = 1000; 出力.outValue = 1000` |
| x13 | 等価両替 | `入力.ruleId == "n100-f4"` | 1 | `出力.inValue = 1000; 出力.outValue = 1000` |
| x14 | 等価両替 | `入力.ruleId == "n100-f5"` | 1 | `出力.inValue = 2000; 出力.outValue = 2000` |
| x15 | 等価両替 | `入力.ruleId == "n1000-f1"` | 1 | `出力.inValue = 5000; 出力.outValue = 5000` |
| x16 | 等価両替 | `入力.ruleId == "n1000-f2"` | 1 | `出力.inValue = 5000; 出力.outValue = 5000` |
| x17 | 等価両替 | `入力.ruleId == "n1000-f3"` | 1 | `出力.inValue = 10000; 出力.outValue = 10000` |
| x18 | 等価両替 | `入力.ruleId == "n1000-f4"` | 1 | `出力.inValue = 10000; 出力.outValue = 10000` |
| x19 | 等価両替 | `入力.ruleId == "n1000-f5"` | 1 | `出力.inValue = 20000; 出力.outValue = 20000` |

このDraftは完全なレシピ実装の受入を示すものではない。
