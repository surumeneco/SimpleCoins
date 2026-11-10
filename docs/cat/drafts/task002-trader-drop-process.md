---
cat_version: '0.3'
kind: process
id: simplecoins.paper.trader-drop-selection
status: draft
process: simplecoins.paper.trader-drop-selection
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: 
  - simplecoins.paper.pi.trader-drop
semantic_contract: markdown-v2
language: ja
---

# Process: Wandering trader coin drop selection (Draft)

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.paper.pi.trader-drop |

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
| 行商人死亡 | simplecoins.paper.pi.trader-drop/draw |

## 仕様と技術境界
行商人の死亡時、設定で有効化/無効化、発生確率、候補額面、額面別の重み、枚数範囲を管理できる。現物貨幣の価値は仕訳に一回だけ計上し、重複イベントを防ぐ。

このWorkは乱数注入による **抽選決定のみ**を独立実装する技術候補。閾値確率、正の重みの抽選、枚数の両端を含む整数分布、無効時に抽選しないこと、異常設定の拒否を先行検証する。実際のPaper死亡イベントからのItemStack生成、永続仕訳、冪等キー、YAML構造、デフォルト確率は未実装・未確定。DraftをConfirmedとみなさない。
