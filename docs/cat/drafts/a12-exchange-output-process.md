---
cat_version: '0.3'
kind: process
id: simplecoins.currency.crafting-exclusion
status: draft
process: simplecoins.currency.crafting-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/pull/444
decision_ref: ''
refs:
  - simplecoins.currency.pi.exchange-result
semantic_contract: markdown-v2
language: ja
---

# Process: 正規貨幣の等価両替と通常クラフトからの保護 (Draft)

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.currency.pi.exchange-result |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- |
| 観測.現行登録レシピ | Paperのクラフトレシピ登録状態 | 真偽 |
| 観測.クラフト実結果 | Paperクラフトイベントと当該Inventoryの実出力 | 真偽 |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |
| 作用.準備結果無効化 | 準備結果スロット | 真偽 |
| 作用.クラフト中止 | 手動クラフト/自動クラフター | 真偽 |

## 起点
| ID | 由来 |
| --- | --- |
| 準備判定 | simplecoins.currency.pi.exchange-result/prepare |
| 手動確定判定 | simplecoins.currency.pi.exchange-result/craft |
| 自動確定判定 | simplecoins.currency.pi.exchange-result/crafter |

## 対象範囲
01で確定している「貨幣を材料にするクラフトは等価両替のみ」「投入と出力の価値一致」に限定する。バニラPAPERでの普通のクラフトは保全し、貨幣PDC/固定item_modelの実同一性を使用する。管理者や他プラグインが登録済みレシピIDだけを使い出力を置換しても、登録ID単独では承認しない。

## 未決の会計世代・他Plugin競合
他プラグインが本Processの判定後に結果スロットを上書きする場合、判定時の観測だけでは最終消費と価値不変を証明できない。時点固定・事後検出・取消/復旧・優先順位・協調の採否は正式設計レビュー事項であり、現状のDraftを実装保証/Confirmedと扱わない。
