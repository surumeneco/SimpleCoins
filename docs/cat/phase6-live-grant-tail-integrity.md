---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.live-tail-integrity
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 稼働中発行ジャーナルの最新レコード整合性

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.grant | 稼働中の予約・pending・承認 |
| simplecoins.journal | durable headと最新sequenceのchecksummed record |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| grant.tail.missing | 正常なreserve後、headを保持したまま最新の .grant を削除したとき | pending等の公開操作をfail-closedにし、欠損レコードを復元したふりをしない | SC-JOURNAL-TAIL-01 |
| grant.tail.corrupt | 最新 .grant が破損・不一致だがheadは残っているとき | 有界・checksummed readerによる検証失敗を検出し、ledgerをunhealthyにする | SC-JOURNAL-TAIL-02 |
| grant.tail.valid | 最新のdurable recordとheadが現行メモリ状態に一致するとき | 通常のpendingと正当なacknowledgementを許可する | SC-JOURNAL-TAIL-03 |
| grant.tail.scope | live ledgerの健全性確認を行うとき | 最新レコードだけの検査は全履歴検証・権威的クラッシュ復旧を意味しない | SC-JOURNAL-TAIL-04 |
| grant.tail.no-recreate | 最新レコードの消失・破損を検出したとき | メモリ上の予約だけで配達・receiptを推定せず、再読込・復旧まで後続操作を拒否する | SC-JOURNAL-TAIL-01, SC-JOURNAL-TAIL-02 |

## 境界と状態

既存SC-JOURNAL-TAIL-01〜04の意味を変えないTASK-20261101-005のCAT Draft。最新レコードの局所検証は完全な履歴監査、同一時点のワールドスナップショット、Paper実機のクラッシュ復旧を証明しない。正式CAT Process/PI/TCE・機械Gate・Paper実機故障注入はnot-run。Confirmed・マージ・デプロイを意味しない。
