---
cat_version: '0.3'
kind: tce
id: simplecoins.give.admin-request.tce.give
status: confirmed
process: simplecoins.give.admin-request
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#発行・照会・配布予約
refs: 
  - simplecoins.give.admin-request.pi.give
semantic_contract: markdown-v2
language: ja
---

# TCE: 発行受付

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| deny | 発行受付 | `入力.発行権限 == 偽` | 1 | `出力.結果 = "拒否"` |
| unavailable | 発行受付 | `入力.発行権限 == 真 かつ 入力.決算確定 == 偽` | 1 | `出力.結果 = "未確定"` |
| invalid | 発行受付 | `入力.発行権限 == 真 かつ 入力.決算確定 == 真 かつ (入力.対象確定 == 偽 または 入力.指定有効 == 偽)` | 1 | `出力.結果 = "不正"` |
| accept | 発行受付 | `入力.発行権限 == 真 かつ 入力.決算確定 == 真 かつ 入力.対象確定 == 真 かつ 入力.指定有効 == 真` | 1 | `出力.結果 = "受付可"` |
