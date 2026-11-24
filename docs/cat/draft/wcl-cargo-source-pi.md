---
cat_version: '0.3'
kind: pi
id: simplecoins.accounting.wcl-cargo-source.pi.capture
status: draft
process: simplecoins.accounting.wcl-cargo-source
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/681
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 通常時の収納読取源

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.accounting | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.GUI状態 | 受信 | 列挙{開, 閉, 不明} | 必須 |
| 入力.正本確認 | 受信 | 真偽 | 必須 |
| 出力.読取源 | 送信 | 列挙{GUI優先, WCL, 保留} | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| capture | 受付 | 収納読取 |

## 制約
| 式 |
| --- |

正本確認とは実個体と提供者の同一性及び状態に対応した読取源が確認できること。GUI優先はGUIの利用可能な貨物スロットが対応する保存済み貨物を置換することを表し、GUI表示の装備・装飾を追加計上しない。WCLは既存の実個体のWCL読取を用いる。保留では未確定の差分を正常記帳しない。
