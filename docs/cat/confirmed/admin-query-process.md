---
cat_version: '0.3'
kind: process
id: simplecoins.accounting.admin-query
status: confirmed
process: simplecoins.accounting.admin-query
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#発行・照会・配布予約
refs: 
  - simplecoins.accounting.admin-query.pi.read
semantic_contract: markdown-v2
language: ja
---

# Process: 管理資産照会

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.accounting.admin-query.pi.read |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 管理照会 | simplecoins.accounting.admin-query.pi.read/read |

管理権限のある送信者だけが公開済み決算の統計・個人保有額・死蔵所在を照会できる。未決算は未確定と表示し、未登録をゼロ登録と混同しない。個人照会でのみ登録の有無が結果を分ける。通常の統計T=P+FでDは別項、対象範囲と集計時点を表示する。照会は資産・登録・物理世界を変更しない。表示・対象解決はPaperアダプタが担当する。
