---
cat_version: '0.3'
kind: process
id: simplecoins.currency.station-consumption-exclusion
status: draft
process: simplecoins.currency.station-consumption-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/pull/489
decision_ref: ''
refs:
  - simplecoins.currency.pi.station-result
semantic_contract: markdown-v2
language: ja
---

# Process: Merchant・Cartographyでの通常PAPER消費禁止（Draft）

## 契約
閉世界: 偽

## 境界
| PI |
| --- |
| simplecoins.currency.pi.station-result |

## 仕様状態
| ID | Domain |
| --- | --- |
| 処理結果 | 列挙{許可,拒否} |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |
| 観測.取引対象 | Paperの結果スロットとインベントリ種別 | 真偽 |
| 観測.貨幣入力 | 現時点の入力枠ItemStack/PDCと固定item_model | 真偽 |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |
| 作用.結果操作拒否 | Merchant・Cartography結果スロットの消費イベント | 真偽 |

## 起点
| ID | 由来 |
| --- | --- |
| 結果操作 | simplecoins.currency.pi.station-result/evaluate |

## 規範入力と境界
Drive 01/05の確定要件は、正規貨幣PAPERを**20種類の等価両替以外の素材として消費させない**ことである。Merchant/CartographyはCraftItemEventの保護対象と同一ではない。ユーザーが明示承認した禁止要件を具体的なPaperイベントへ写像するためのDraftであり、イベントの技術仕様を人間の追加承認なしに確定しない。

対象はMerchant/Cartographyの**結果スロットで、入力のいずれかがSimpleCoins貨幣PDCを保有する場合の消費拒否**。正常なPAPERや結果取得以外の操作、貨幣の単なる移動を禁止しない。PDC型が不正でも貨幣マーカーを持つ入力は許可しない。入力の同世代観測が不可能な場合に結果消費を承認しない。

## 未確定・未試験
実際の商人取引成立、CartographyItemEventのネイティブ操作、Shift/数字キー/ドラッグ等のクリック種別、他プラグインによるイベント後置換、実物の入力消費、JE/BE、仕訳および全writer整合は未確認。PR #489は実Paperで**模擬**CartographyItemEventがキャンセルされない観測と商人PAPERレシピ登録のみを実証した。純粋な`PaperCurrencyConsumption.blocksResult`が旧PR #316にあっても、製品listenerは存在しない。これをConfirmed/正式Gate/保護完了と扱わない。
