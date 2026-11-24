---
cat_version: '0.3'
kind: pi
id: simplecoins.accounting.invsee-source.pi.capture
status: confirmed
process: simplecoins.accounting.invsee-source
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/Jannyboy11/InvSee-plus-plus/blob/v0.31.22/InvSee%2B%2B_Common/src/main/java/com/janboerman/invsee/spigot/api/InvseeAPI.java
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 対象収納の正本選択

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.accounting | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.オンライン | 受信 | 真偽 | 必須 |
| 入力.有効編集画面 | 受信 | 真偽 | 必須 |
| 出力.読取源 | 送信 | 列挙{実Player, 編集画面, 保存} | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| capture | 受付 | 収納読取 |

## 制約
| 式 |
| --- |

収納区画ごとに正本が確認済みの場合の選択のみを表す。画面を閉じた後は保存成功/取消の実データを反映し、死蔵再分類も同一正本を利用する。入力確認、読取成功、仕訳成功をこのPIだけで証明しない。
