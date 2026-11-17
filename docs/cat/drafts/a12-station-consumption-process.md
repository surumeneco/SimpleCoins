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
  - https://github.com/surumeneco/SimpleCoins/pull/491
  - https://github.com/surumeneco/SimpleCoins/pull/492
  - https://github.com/surumeneco/SimpleCoins/pull/493
  - https://github.com/surumeneco/SimpleCoins/pull/494
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

## 実NMS技術証拠と未証明のPaperイベント保証

- [PR #491](https://github.com/surumeneco/SimpleCoins/pull/491)：実Paper 26.2 `MerchantOffer.satisfiedBy/take` は、正規5000C PDC+model付きPAPERを通常PAPERとして受理し、**隔離NMS ItemStackを1→0に減算**した。これは実プレイヤー取引の完了ではない。
- [PR #492](https://github.com/surumeneco/SimpleCoins/pull/492)：本物の`CartographyTableMenu`は実地図+`PAPER`と実地図+正規5000C貨幣の双方から`FILLED_MAP`を結果生成した。地図用入力slot0に貨幣を入れても結果なし、紙を使う実役割は**slot1**。入力二枠のスキャンは安全側候補であり、両枠の通貨消費経路が実証されたわけではない。
- [PR #493](https://github.com/surumeneco/SimpleCoins/pull/493)：実製図台の結果`Slot.onTake`は隔離NMS貨幣を1→0に減算。通常紙の対照も1→0。
- [PR #494](https://github.com/surumeneco/SimpleCoins/pull/494)：`CartographyTableMenu.clicked`を**直接**呼ぶ場合も通常紙と貨幣が1→0、結果`FILLED_MAP`がcarriedへ移る。この直接NMS呼出しでは`InventoryClickEvent`も`CartographyItemEvent`も各0件。**ネットワーク側のクリック処理を呼んでいないためであり、通常のJE/BEクリックでイベントが回避されることは示していない。**

### 未確定な実装境界

Paperネットワーク操作での各クリック種別（通常・Shift・数字キー・ドラッグ）から結果の受領・入力減算に至るイベント通知順、キャンセルの有効性、監視後の他プラグイン変更、実プレイヤー資産、JE/BE、仕訳／同一世代保存は**未検証**。外部イベントでの保護だけでは、別プラグインがメニューのNMS APIを直接呼ぶ非協調経路を止められるとは限らない。現状の製品`CoinExchangeRecipes`はCraftItemEvent等のみを登録し、Merchant/Cartographyの専用listenerはない。旧PR #316の`PaperCurrencyConsumption.blocksResult`はイベント未配線の純粋関数であり、本Processの実装ではない。

本書は既存の貨幣価値保存要件を精密化する`Draft`。ネットワークイベントの技術選択と防護境界は正当な設計確認・CAT Confirmed・正式Gateを経るまで未承認であり、**本番の貨幣保護完了・公開許可を主張しない**。
