---
cat_version: '0.3'
kind: process
id: simplecoins.paper.trader-live-issuance
status: confirmed
process: simplecoins.paper.trader-live-issuance
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
refs:
  - simplecoins.paper.trader-live-issuance.pi.event
semantic_contract: markdown-v2
language: ja
---

# Process: simplecoins.paper.trader-live-issuance

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.paper.trader-live-issuance.pi.event |

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
| イベント | simplecoins.paper.trader-live-issuance.pi.event/event |

決算及び通常仕訳が有効な状態で、キャンセルされていない行商人死亡だけを対象とする。抽選は03の設定を使用し、成功時のみ貨幣をドロップへ追加する。通常のItemSpawnEventで実際の貨幣発生を仕訳へ反映する。集計対象外ワールドでもゲーム上の抽選を止めない。全件決算で復旧可能な通常仕訳のため、死亡イベント毎の永続発行WALを必須としない。
