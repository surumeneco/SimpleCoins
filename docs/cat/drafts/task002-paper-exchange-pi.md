---
cat_version: '0.3'
kind: pi
id: simplecoins.paper.pi.exchange
status: draft
process: simplecoins.paper.coin-exchange
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
decision_ref: ''
refs:
  - simplecoins.paper.tce.exchange
semantic_contract: markdown-v2
language: ja
---

# PI: Coin exchange formula (Draft)

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.currency-inventory | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.ruleId | 受信 | 20固定式 | 必須 |
| 出力.inValue | 送信 | 整数 | 必須 |
| 出力.outValue | 送信 | 整数 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| craft | 受付 | 等価両替 |

## 制約
| 式 |
| --- |

すべての式について出力と入力の価値が等しい。紙混用・他レシピへの通貨流出は禁止。独自PDC以外の表示名を値と解釈しない。
