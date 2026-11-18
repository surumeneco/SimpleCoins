---
cat_version: '0.3'
kind: pi
id: simplecoins.currency.pi.station-result-local
status: draft
process: simplecoins.currency.station-result-local-decision
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/529
decision_ref: pending-user-approval:a12-local-decision-process
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 正規化された結果取得判定の真偽入力

## 境界
| 相手Process | 方向 |
| --- | --- |
| paper.merchant-cartography-result-event-adapter | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.対象結果操作 | 受信 | 真偽 | 必須 |
| 入力.入力読取可能 | 受信 | 真偽 | 必須 |
| 入力.貨幣マーカーあり | 受信 | 真偽 | 必須 |
| 出力.許可 | 送信 | 真偽 | 必須 |
| 出力.取消 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| evaluate | 受付 | 判定 |

## 制約
| 式 |
| --- |
| `出力.許可 == 偽 または 出力.取消 == 偽` |

## 正規化の定義と未証明の境界

- `対象結果操作`：実入力がMERCHANT/CARTOGRAPHYの`InventoryClickEvent`で、結果raw slot 2の操作とアダプターが判定済みなら真。それ以外は偽。この分類精度のPaper技術証明は別責務。
- `入力読取可能`：対象イベント時点で上段2入力枠のItemStack/PDCを読めたこと。失敗時は偽。非対象では出力に影響しない。
- `貨幣マーカーあり`：いずれかの入力に`simplecoins:coin_denomination`が存在すること。不正型のPDCマーカーでも真。表示名だけでの識別は禁止。
- `出力.取消` 真は追加取消の要求で、実Paperで取消できた証拠でも後続Pluginを拘束した証拠でもない。偽は新規取消なしであり、先行キャンセルの解除を意味しない。
- `出力.許可`は本判定の追加拒否なしを表し、決算公開/貨幣発行の許可ではない。
