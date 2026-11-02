---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.rejected-reservation-observation
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: GrantLedger rejected reservation non-persistence

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.grant | 既存GrantLedgerの入力拒否時のディスク状態・次回再起動境界 |

## 観測・検証対象
| ID | 条件 | 観測する既存実装の振る舞い | 検証 |
| --- | --- | --- | --- |
| grant.invalid.overflow | typed coin count×denominationでlong overflow | 予約前に例外となりgrantファイルを作らず、後続の正常予約はsequence 1 | SC-GRANT-INVALID-01 |
| grant.invalid.arguments | amount=0または空白issuer | 入力検査で拒否し、空journalの再起動後も正常なsequence 1から予約できる | SC-GRANT-INVALID-02 |
| grant.invalid.recovery | 入力拒否後の正常予約と再起動 | pendingに同一grantが1件復元される | SC-GRANT-INVALID-01 |

## 状態・境界

これは既存コードを対象とする**code-to-spec観測Draft**であり、例外型（現行ArithmeticException/IllegalArgumentException）を将来の規範仕様として確定しない。PR #131の**SCG2 replay時の算術overflow**とは異なり、予約前の入力拒否がjournalへ副作用を残さないかを検証する。正常な入力の予約永続化を変更するものではない。

本テストは実際のPaper playerdata受渡し・inventory・receipt・再接続・JE/BE・Nexoの動作、全CAT意味論Gateを検証しない。新規本番実装やConfirmed昇格は行わない。
