---
cat_version: '0.3'
kind: process
id: simplecoins.paper.trader-drop-config
status: draft
process: simplecoins.paper.trader-drop-config
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.trader-config
semantic_contract: markdown-v2
language: ja
---

# Process: 行商人ドロップ設定の読込（Draft）

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.paper.pi.trader-config |

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
| 設定読込 | simplecoins.paper.pi.trader-config/load |

## 仕様と技術候補
確定仕様: 行商人の貨幣ドロップはenabled、発生確率、額面別重み、対象額面、枚数範囲をconfigから変更できる。正常な額面/PDCの同定は表示文字列に依存しない。利用者の2026-11-10の明示決定により**初期の候補は9額面全部、低額からの重みは256/128/64/32/16/8/4/2/1（総重み511、最高額10000が1）**とする。死亡時の確率0.001、最小・最大枚数各1、enabled=trueは従来初期設定を維持する。運営configの明示的なカスタム設定は自動で上書きしない。

実装候補: `trader-drops.enabled`/`probability`/`min-count`/`max-count`/`max-stacks-per-event`/`weights` を厳密に型検証する。設定節が存在しなければ無効化、存在する場合はenabledが明示必須。異常値・未知額面・重み重複・処理上限超過は設定を受理しない。旧候補実装の `chance`/`minimum-count`/`maximum-count` と新しい `probability`/`min-count`/`max-count` の両方を読める移行アダプターを用意し、同じ意味のキーが同時指定された場合は設定ミスとして拒否する。旧キーのみに基づく設定では `max-stacks-per-event` 未指定を1スタックの安全制約として扱い、64枚超を無制限に発行しない。キー名・最大処理上限は**技術候補**であり、承認済みの9額面・重み初期値とは区別する。発行・仕訳/死亡イベントの実装とは独立。
