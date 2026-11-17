---
cat_version: '0.3'
kind: tce
id: simplecoins.paper.tce.coin
status: draft
process: simplecoins.paper.coin-identity
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.coin
semantic_contract: markdown-v2
language: ja
---

# TCE: Coin value encoding (Draft)

網羅要求: closed

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| c00 | 貨幣生成 | `入力.denomination == 1` | 1 | `出力.faceValue = 1` |
| c01 | 貨幣生成 | `入力.denomination == 5` | 1 | `出力.faceValue = 5` |
| c02 | 貨幣生成 | `入力.denomination == 10` | 1 | `出力.faceValue = 10` |
| c03 | 貨幣生成 | `入力.denomination == 50` | 1 | `出力.faceValue = 50` |
| c04 | 貨幣生成 | `入力.denomination == 100` | 1 | `出力.faceValue = 100` |
| c05 | 貨幣生成 | `入力.denomination == 500` | 1 | `出力.faceValue = 500` |
| c06 | 貨幣生成 | `入力.denomination == 1000` | 1 | `出力.faceValue = 1000` |
| c07 | 貨幣生成 | `入力.denomination == 5000` | 1 | `出力.faceValue = 5000` |
| c08 | 貨幣生成 | `入力.denomination == 10000` | 1 | `出力.faceValue = 10000` |

固定PDC識別とitem_modelのキー選択は技術候補であり、このDraftだけを理由に正規Confirmedとしない。
