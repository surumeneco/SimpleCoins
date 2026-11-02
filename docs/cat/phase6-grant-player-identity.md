---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.player-identity
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 発行予約の受取人UUIDとreceiptの対応

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.grant | 予約・照会の受取人UUID |
| simplecoins.journal | 永続発行予約のID・sequence・receipt |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| grant.pending.identity | pending(null) | 空一覧と解釈せず拒否する | SC-GRANT-IDENTITY-01 |
| grant.receipt.identity | validateReceipt(null, 0) | 有効なゼロreceiptと解釈せず拒否する | SC-GRANT-IDENTITY-02 |
| grant.restart.valid | 有効な受取人UUIDの予約を永続化し再起動したとき | 未配達予約を保持し、適正receiptにより一回だけ承認できる | SC-GRANT-IDENTITY-03 |
| grant.applied.binding | wasAppliedにgrantと保存済みreceiptが渡されたとき | sequence・grant UUID・受取人UUID・不変フィールドを現行journalと照合し、別人や偽造予約を適用済みとしない | SC-GRANT-IDENTITY-04 |

## 境界と状態

既存SC-GRANT-IDENTITY-01〜04の意味を変えないTASK-20261101-005のCAT Draft。局所的identity guardはplayerdata保存完了、Paper実機配達、クラッシュ後の完全な再送防止を保証しない。正式CAT Process/PI/TCE・機械Gate・実機検証はnot-run。Confirmed・マージ・リリースを意味しない。
