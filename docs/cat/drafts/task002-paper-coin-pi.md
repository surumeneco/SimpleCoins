---
cat_version: '0.3'
kind: pi
id: simplecoins.paper.pi.coin
status: draft
process: simplecoins.paper.coin-identity
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: Coin denomination to ItemStack encoding (Draft)

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.currency-adapter | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.denomination | 受信 | 列挙{1, 5, 10, 50, 100, 500, 1000, 5000, 10000} | 必須 |
| 出力.faceValue | 送信 | 整数 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| create | 受付 | 貨幣生成 |

## 制約
| 式 |
| --- |

9額面以外を認めないこと。実装ではPDCとitem_modelの識別子は名前と区別し、ItemStackの量は1..64に制限する。
