---
cat_version: '0.3'
kind: process
id: simplecoins.settlement.writer-epoch-publication
status: confirmed
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-24:approved-revised-simplecoins-scope
refs:
  - simplecoins.settlement.writer-epoch-publication.pi.authorize
semantic_contract: markdown-v2
language: ja
---

# Process: G0全件決算の公開判定

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.settlement.writer-epoch-publication.pi.authorize |

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
| 公開判定 | simplecoins.settlement.writer-epoch-publication.pi.authorize/evaluate |
| 失効通知 | simplecoins.settlement.writer-epoch-publication.pi.authorize/revoke |

## 保証境界

現物資産を正本とし、対象全件の読取と重複のないP/F/D分類、実構成の通常運用で生じる競合の解消、通常仕訳への引継、決算成功前の全員入室拒否を要求する。完成した候補のみ一括公開し、実公開成功後に入室を許可する。統計は二次情報であり再起動時に再構築する。全JVM writer閉包、全writer排他、永続WAL、Agentは独立した要件ではない。方式A（管理された起動時静止区間）を第一試行とし、成立しない場合は04のB/Cを比較する。本判定は公開試行の可否であり、実際のキャッシュ切替や入室許可を証明しない。
