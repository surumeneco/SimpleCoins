---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.ack-durable-target
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 旧発行予約の承認前durable検証

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.grant | 既存の未配達発行予約の承認・durable record |
| simplecoins.journal | checksummedな発行ジャーナルと同一sequenceの永続レコード |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| grant.ack.target-missing | 二つ以上の予約が存在し、承認対象の古い `<sequence>.grant` が欠損したとき | 承認を拒否し、欠損レコードを再作成・上書きしない | SC-JOURNAL-ACK-01 |
| grant.ack.target-corrupt | 古い承認対象の永続レコードが破損・不一致のとき | 承認を拒否し、破損レコードを上書きしない | SC-JOURNAL-ACK-02 |
| grant.ack.target-valid | 承認対象の永続レコードが有界・checksummed readerで検証され、メモリ上の予約と全不変フィールドが一致し、保存済みreceiptも対応するとき | 対象予約を承認可能とする | SC-JOURNAL-ACK-03 |
| grant.ack.health | 承認対象のdurable recordが欠損・破損・不一致と判明したとき | ledgerをunhealthyに固定し、再読込・復旧前の後続操作を許可しない | SC-JOURNAL-ACK-01, SC-JOURNAL-ACK-02 |
| grant.ack.scope | live ledgerに承認対象より新しい予約があるとき | tailだけの健全性確認を古い承認対象レコードの証明として使わず、対象sequence自身を読み戻す | SC-JOURNAL-ACK-01, SC-JOURNAL-ACK-03 |
| grant.ack.idempotent-durable | 既にdeliveredの非tail予約へ再度acknowledgeし、対象の永続レコードが後から欠損・破損したとき | メモリ上のdeliveredだけで成功扱いせず、対象durableを再検証して不正ならunhealthyへ遷移し、上書き修復しない | SC-JOURNAL-ACK-07 |

## 境界と状態

これはTASK-20261101-005の既存CAT **Draft** を意味変更せず構造化したもの。旧予約の対象レコード検証は、全履歴の完全監査、playerdataの保存durability、Paper実機クラッシュ復旧の証明ではない。現行のGrantLedgerはtail健全性を検査するが、古い承認対象のdurable再読込は未実装。正式CAT Process/PI/TCEの規範確認と機械Gateはnot-runであり、Confirmed・実装完了・決算公開・マージ・デプロイを意味しない。
