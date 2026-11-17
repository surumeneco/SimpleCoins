---
cat_version: '0.3'
kind: pi
id: simplecoins.currency.pi.station-result
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
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: Merchant・Cartographyの結果取得判定（Draft）

## 境界
| 相手Process | 方向 |
| --- | --- |
| paper.merchant-cartography-inventory-events | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.対象ステーション | 受信 | 列挙{merchant,cartography} | 必須 |
| 入力.結果操作 | 受信 | 真偽 | 必須 |
| 入力.入力読取可能 | 受信 | 真偽 | 必須 |
| 入力.貨幣マーカーあり | 受信 | 真偽 | 必須 |
| 出力.許可 | 送信 | 真偽 | 必須 |
| 出力.取消 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| evaluate | 受付 | 結果操作 |

## 制約
| 式 |
| --- |
| `入力.結果操作 == 真 かつ 入力.貨幣マーカーあり == 真 ならば 出力.許可 == 偽` |
| `入力.結果操作 == 真 かつ 入力.入力読取可能 == 偽 ならば 出力.取消 == 真` |

## 項目の意味・設計上の留保
- `結果操作`: ネイティブMerchant/Cartographyの結果raw slot 2から受領する操作。Minecraft本体の製図台は、実地図slot0＋正規貨幣PAPER slot1で結果`FILLED_MAP`を生成し、実`onTake`と直接`clicked`で貨幣を1枚減らすことをPR #492〜#494で検証済み。**ただしネットワークからの通常/Shift/数字キー等とPaperイベントの対応は未証明**。それらを同一起点にまとめてよいかは今後の技術確認事項。
- `貨幣マーカーあり`: 正規ItemStackだけでなく不正値/不正型でも`simplecoins:coin_denomination`マーカーが存在する入力。表示名や画像からの推定は禁止。純粋マーカー確認と`CoinItemCodec.identify`の有効額面照合は区別する。
- `入力読取可能`: 対象インベントリの上段入力2スロットを両方とも実物から読めること。Cartographyの実レシピで紙を消費するのはslot1だが、両スロットの読取を候補とする。異常取得時はfail-closed。読取結果が操作確定まで不変である保証はこのBoolean入力だけからは導けない。
- `出力.取消`はPaperが該当結果取得を**実際に取り消す**という仕様候補であり、現行製品コードにそのlistenerが存在することを意味しない。`CartographyTableMenu.clicked`をNMS経由で直接呼んだPR #494ではBukkit`InventoryClickEvent`/`CartographyItemEvent`の観測数は双方0件だった。これを通常のネットワーククリックにも適用してイベントが発火しないと推定しない。
- イベント監視後に別プラグインが状態を変えないこと、NMS APIの直接呼出しを経由しても保護されること、保存・仕訳とG0/G3が同一世代であることは、このPIの現行Draftに対する**未証明の外部契約**である。
