---
cat_version: '0.3'
kind: process
id: simplecoins.grant.ack
status: draft
process: simplecoins.grant.ack
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs:
  - simplecoins.grant.ack.pi.request
semantic_contract: markdown-v2
language: ja
---

# Process: simplecoins.grant.ack

## 契約
閉世界: 偽

発行済み予約のacknowledgementを、保存済みreceiptと対象durable recordの検証が成立した場合だけ認める境界。tail検証だけで古い対象recordを認証しない。

## 境界
| PI |
| --- |
| simplecoins.grant.ack.pi.request |

## 仕様状態
| ID | Domain |
| --- | --- |
| 状態.健全 | 真偽 |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |
| 観測.対象durable | 有界・checksummed journal readerによる対象sequenceの再読込 | 列挙{intact,missing,corrupt,mismatch} |
| 観測.receipt適合 | 対象grantと保存済みplayerdata receiptの照合 | 真偽 |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |
| 作用.承認記録 | 対象sequenceのdurable delivered record | 真偽 |

## 起点
| ID | 由来 |
| --- | --- |
| 承認要求 | simplecoins.grant.ack.pi.request/ack |

## 未決定・境界
このProcessは既存SC-JOURNAL-ACK-01〜03のCAT Draft。初期状態とdurable write failureの全組合せ、playerdata保存完了、全履歴の検査は未閉包。正式CAT Gateはnot-run。Confirmedへの昇格や実装着手の許可ではない。
