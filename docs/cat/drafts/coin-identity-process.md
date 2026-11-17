---
cat_version: '0.3'
kind: process
id: simplecoins.paper.coin-identity
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

# Process: Paper currency ItemStack identity (Draft)

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.paper.pi.coin |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 貨幣生成 | simplecoins.paper.pi.coin/create |

## 実装境界
9額面のPAPER ItemStackを作るとき、固定PDCが正の額面を保持し、固定item_modelが額面によって決定する。表示名は設定変更可能で、表示名の変更でPDCと額面を変更しない。通常の紙に貨幣識別子を付けない。製品コードの識別キーとモデル名は移行時の互換性を要するため、技術候補としてレビューし、Confirmedにする際は個別の決定証跡で承認する。クラフト/行商人/財布/ジャーナルは別Work。

## 現行の位置付け

このDraftは、現行`CoinItemCodec`が使用する額面PDCと`item_model`の識別境界を定義するための技術候補であり、実Paper全経路・保存世代・正式CAT Confirmedを証明しない。

由来: 旧[PR #315](https://github.com/surumeneco/SimpleCoins/pull/315)の未統合Draftを、現行`develop@e28f2a8d`のコードと照合して保存。ソース由来の観測は規範承認ではない。
