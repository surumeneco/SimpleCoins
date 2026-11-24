---
cat_version: '0.3'
kind: pi
id: simplecoins.paper.trader-live-issuance.pi.event
status: confirmed
process: simplecoins.paper.trader-live-issuance
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: simplecoins.paper.trader-live-issuance

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.events | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.準備済 | 受信 | 真偽 | 必須 |
| 入力.行商人 | 受信 | 真偽 | 必須 |
| 入力.キャンセル | 受信 | 真偽 | 必須 |
| 入力.抽選成功 | 受信 | 真偽 | 必須 |
| 出力.発行 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| event | 受付 | イベント |

## 制約
| 式 |
| --- |

決算及び通常仕訳が有効な状態で、キャンセルされていない行商人死亡だけを対象とする。抽選は03の設定を使用し、成功時のみ貨幣をドロップへ追加する。通常のItemSpawnEventで実際の貨幣発生を仕訳へ反映する。集計対象外ワールドでもゲーム上の抽選を止めない。全件決算で復旧可能な通常仕訳のため、死亡イベント毎の永続発行WALを必須としない。
