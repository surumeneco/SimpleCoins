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
- `結果操作`: 実際の出力取得が可能なraw slot 2への操作。クリック種別に依存せず取得・Shift等を検出する必要がある一方、ネイティブPaperのイベント分岐は実機検証前の技術候補。
- `貨幣マーカーあり`: 正規ItemStackだけでなく不正値/不正型でも`simplecoins:coin_denomination`マーカーが存在する入力。表示名や画像からの推定は禁止。
- `入力読取可能`: 対象インベントリの上段入力2スロットを両方とも実物から読めること。異常取得時はfail-closed。
- 出力判定は`InventoryClickEvent`のイベント検査結果であり、その後に別のプラグインが状態を変えないことやワールド保存時点の保証までは含めない。
