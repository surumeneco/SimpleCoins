---
cat_version: '0.3'
kind: process
id: simplecoins.currency.station-consumption-exclusion
status: confirmed
process: simplecoins.currency.station-consumption-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/pull/489
  - https://github.com/surumeneco/SimpleCoins/pull/491
  - https://github.com/surumeneco/SimpleCoins/pull/492
  - https://github.com/surumeneco/SimpleCoins/pull/493
  - https://github.com/surumeneco/SimpleCoins/pull/494
  - https://github.com/surumeneco/SimpleCoins/pull/518
  - https://github.com/surumeneco/SimpleCoins/pull/519
  - https://github.com/surumeneco/SimpleCoins/pull/520
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
refs:
  - simplecoins.currency.pi.station-result
semantic_contract: markdown-v2
language: ja
---

# Process: Merchant・Cartographyでの通常PAPER消費禁止

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
Drive 01/05の確定要件は、正規貨幣PAPERを**20種類の等価両替以外の素材として消費させない**ことである。Merchant/CartographyはCraftItemEventの保護対象と同一ではない。ユーザーが明示承認した禁止要件と本Processの結果操作時の許可・拒否の意味は承認済み。ただし利用するPaperイベントの選択・網羅性は技術検証事項であり、未検証のイベント経路を実装済みと扱わない。

対象はMerchant/Cartographyの**結果スロットで、入力のいずれかがSimpleCoins貨幣PDCを保有する場合の消費拒否**。正常なPAPERや結果取得以外の操作、貨幣の単なる移動を禁止しない。PDC型が不正でも貨幣マーカーを持つ入力は許可しない。入力の同世代観測が不可能な場合に結果消費を承認しない。

## 実NMS技術証拠と未証明のPaperイベント保証

- [PR #491](https://github.com/surumeneco/SimpleCoins/pull/491)：実Paper 26.2 `MerchantOffer.satisfiedBy/take` は、正規5000C PDC+model付きPAPERを通常PAPERとして受理し、**隔離NMS ItemStackを1→0に減算**した。これは実プレイヤー取引の完了ではない。
- [PR #492](https://github.com/surumeneco/SimpleCoins/pull/492)：本物の`CartographyTableMenu`は実地図+`PAPER`と実地図+正規5000C貨幣の双方から`FILLED_MAP`を結果生成した。地図用入力slot0に貨幣を入れても結果なし、紙を使う実役割は**slot1**。入力二枠のスキャンは安全側候補であり、両枠の通貨消費経路が実証されたわけではない。
- [PR #493](https://github.com/surumeneco/SimpleCoins/pull/493)：実製図台の結果`Slot.onTake`は隔離NMS貨幣を1→0に減算。通常紙の対照も1→0。
- [PR #494](https://github.com/surumeneco/SimpleCoins/pull/494)：`CartographyTableMenu.clicked`を**直接**呼ぶ場合も通常紙と貨幣が1→0、結果`FILLED_MAP`がcarriedへ移る。この直接NMS呼出しでは`InventoryClickEvent`も`CartographyItemEvent`も各0件。**ネットワーク側のクリック処理を呼んでいないためであり、通常のJE/BEクリックでイベントが回避されることは示していない。**

- [PR #518](https://github.com/surumeneco/SimpleCoins/pull/518)：公式Paper 26.2 build134の実`ServerGamePacketListenerImpl.handleContainerClick`を、実ネットワークに接続しない隔離`ServerPlayer`から直接実行。製図台結果raw slot2のクリックでは`InventoryClickEvent`と`CartographyItemEvent`が各1件。通常PAPERは1→0、正規5000Cは**試験専用**`InventoryClickEvent`取消時だけ1→1。`CartographyTableMenu.clicked`直接呼出しのイベント0件という#494と矛盾しない。
- [PR #519](https://github.com/surumeneco/SimpleCoins/pull/519)：実`MerchantMenu`結果raw slot2も同じPaperハンドラー経由で`InventoryClickEvent`が1件発火。通常PAPERは1→0、正規5000Cは**試験専用**取消で1→1。
- [PR #520](https://github.com/surumeneco/SimpleCoins/pull/520)：同じMerchant結果クリックで試験専用取消を除くと、SimpleCoinsをロードしたままでも正規5000Cが**1→0**になり、`InventoryClickEvent`は1件・取消0件。従って製品にMerchant用の実効的な保護listenerが未結線であることを隔離実Paperの結果で確認した。製図台の無保護実消費をこのPRで再実証したとは主張しない。

### 未確定な実装境界

これらは**実Paperのサーバー側packet-handlerをテスト内から直接起動**した証拠であり、認証済みクライアント、ソケット通信、JE/BE端末の実操作ではない。確認した通常クリック以外のShift・数字キー・ドラッグ・ダブルクリック等について、イベント発火・取消の保証は**未検証**。外部イベントでの保護だけでは、別プラグインがメニューのNMS APIを直接呼ぶ非協調経路を止められるとは限らない。現状の製品`CoinExchangeRecipes`はCraftItemEvent等のみを登録し、Merchant/Cartographyの専用listenerはない。旧PR #316の`PaperCurrencyConsumption.blocksResult`はイベント未配線の純粋関数であり、本Processの実装ではない。テスト専用listenerの取消成功は製品の価値保護成立を意味しない。

本書の許可・拒否の意味は利用者承認によりConfirmedとするが、ネットワークイベントの技術選択と防護境界は実証・CAT Gateを要し、**本番の貨幣保護完了・公開許可を主張しない**。
