---
cat_version: '0.3'
kind: pi
id: simplecoins.accounting.memory-journal.pi.record
status: confirmed
process: simplecoins.accounting.memory-journal
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 確定仕訳の記帳結果

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.accounting | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.順序 | 受信 | 列挙{記帳済, 次, 欠落} | 必須 |
| 入力.差額整合 | 受信 | 真偽 | 必須 |
| 出力.結果 | 送信 | 列挙{反映, 重複, 拒否} | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| record | 受付 | 確定仕訳 |

## 制約
| 式 |
| --- |

差額整合は一操作の全増減を適用しても個人・フリー・死蔵所在の残高が負にならない状態。次は現在の記帳順序+1。記帳済順序は反映しない。操作の物理的確定・帰属判定は呼出側の前提である。
