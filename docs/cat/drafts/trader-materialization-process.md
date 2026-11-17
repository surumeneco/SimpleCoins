---
cat_version: '0.3'
kind: process
id: simplecoins.paper.trader-stack-materialization
status: draft
process: simplecoins.paper.trader-stack-materialization
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.trader-stack
semantic_contract: markdown-v2
language: ja
---

# Process: 行商人ドロップの安全なスタック準備（Draft）

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.paper.pi.trader-stack |

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
| 抽選結果の確定 | simplecoins.paper.pi.trader-stack/materialize |

## 仕様・実装上の境界
確定仕様: 貨幣は9額面のPAPER ItemStackで、1スタック最大64個。行商人ドロップはconfigによる枚数範囲に従う。発行価値の二重計上/消失を防ぐ。

技術候補: 正の抽選枚数を64個ずつ分割する。**発行先への公開とjournal記録より前に**全スタック数と運用上限を評価し、準備中に失敗した場合は結果を外部へ渡さない。数量と価値を保存し、表示名は識別子にしない。運用上限の最終設定値、Paperイベントと永続仕訳への結合は別途受入を要する。このDraftをConfirmedとみなさない。
