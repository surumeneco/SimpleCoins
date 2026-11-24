---
cat_version: '0.3'
kind: pi
id: simplecoins.accounting.ground-item.pi.event
status: confirmed
process: simplecoins.accounting.ground-item
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: simplecoins.accounting.ground-item

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.events | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.種別 | 受信 | 列挙{生成, 消失, プレイヤー投棄, プレイヤー回収} | 必須 |
| 入力.対象ワールド | 受信 | 真偽 | 必須 |
| 入力.確定 | 受信 | 真偽 | 必須 |
| 出力.フリー | 送信 | 列挙{増, 減, 不変} | 必須 |
| 出力.個人 | 送信 | 列挙{増, 減, 不変} | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| event | 受付 | イベント |

## 制約
| 式 |
| --- |

地面アイテム生成は集計対象ワールドのF増、消失はF減。プレイヤー投棄はP減（生成は別に観測し同tickの差分を一括適用）、回収はP増と対象ワールドならF減。非対象ワールドの地面は集計しないがプレイヤーは常時対象。キャンセル・未確定は反映しない。入れ子と財布を含む実価値、実回収枚数を用いる。Mob・ホッパー回収、死亡、容器、所有変更はこの部分契約の外側で、全体の追跡が完成するまでは入室を解除しない。
