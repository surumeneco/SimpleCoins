---
cat_version: '0.3'
kind: process
id: simplecoins.accounting.ground-item
status: confirmed
process: simplecoins.accounting.ground-item
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
refs:
  - simplecoins.accounting.ground-item.pi.event
semantic_contract: markdown-v2
language: ja
---

# Process: simplecoins.accounting.ground-item

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.accounting.ground-item.pi.event |

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
| イベント | simplecoins.accounting.ground-item.pi.event/event |

地面アイテム生成は集計対象ワールドのF増、消失はF減。プレイヤー投棄はP減（生成は別に観測し同tickの差分を一括適用）、回収はP増と対象ワールドならF減。非対象ワールドの地面は集計しないがプレイヤーは常時対象。キャンセル・未確定は反映しない。入れ子と財布を含む実価値、実回収枚数を用いる。Mob・ホッパー回収、死亡、容器、所有変更はこの部分契約の外側で、全体の追跡が完成するまでは入室を解除しない。
