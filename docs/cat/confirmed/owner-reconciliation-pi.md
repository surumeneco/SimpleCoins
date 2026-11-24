---
cat_version: '0.3'
kind: pi
id: simplecoins.accounting.owner-reconciliation.pi.replace
status: confirmed
process: simplecoins.accounting.owner-reconciliation
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 対象所有者の再分類結果

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.accounting | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.完全 | 受信 | 真偽 | 必須 |
| 入力.BAN | 受信 | 真偽 | 必須 |
| 入力.playerdata | 受信 | 真偽 | 必須 |
| 入力.登録根拠 | 受信 | 真偽 | 必須 |
| 入力.残存 | 受信 | 真偽 | 必須 |
| 出力.結果 | 送信 | 列挙{保留, 個人登録, 個人未登録, 死蔵BAN, 死蔵欠損} | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| replace | 受付 | 再分類 |

## 制約
| 式 |
| --- |

登録根拠は既登録履歴または現存する登録対象の所有根拠。残存は対象UUIDの現存資産の合計が正であること。完全観測では全所在と額を確定し、零額も区別する。結果の個人登録・個人未登録は新Pの登録状態、死蔵BAN・死蔵欠損は新D所在の分類（零なら所在なし）。BAN優先は既存AssetOwner契約。保留ではキャッシュを変更しない。所在・金額の正しさは別途実体観測と複数所在の回帰で検証する。
