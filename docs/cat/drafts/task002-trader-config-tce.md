---
cat_version: '0.3'
kind: tce
id: simplecoins.paper.tce.trader-config
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

# TCE: ドロップ設定読込（Draft）

網羅要求: partial

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| absent | 設定読込 | 節が存在しない | 1 | ドロップ無効 |
| enabled | 設定読込 | 正常な有効設定 | 1 | 抽選仕様へ反映 |
| malformed | 設定読込 | 数値文字列や未知の額面 | 0 | 設定を拒否 |

実装候補のテスト計画であり、正式CATの仕様確定、網羅判定、発行実機テストではない。
