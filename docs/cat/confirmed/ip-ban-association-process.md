---
cat_version: '0.3'
kind: process
id: simplecoins.accounting.ip-ban-association
status: confirmed
process: simplecoins.accounting.ip-ban-association
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view#IP-BANのUUID帰属（2026-11-25確定）
refs: []
  - simplecoins.accounting.ip-ban-association.pi.resolve
semantic_contract: markdown-v2
language: ja
---

# Process: 現在IP BANのUUID対応

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.accounting.ip-ban-association.pi.resolve |

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
| 照合 | simplecoins.accounting.ip-ban-association.pi.resolve/resolve |

02Aの確定したIP BAN対応のみを表す。UUIDごとに最終接続IP、手動補正、現在IP BANリストから一致を判定する。プロフィールBAN/外部BAN/データ欠損は既存の独立した判定で合成する。接続・永続化・BANリスト取得はこの局所判定の外部観測であり、実Paper受入で検証する。
