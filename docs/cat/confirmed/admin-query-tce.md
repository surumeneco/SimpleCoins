---
cat_version: '0.3'
kind: tce
id: simplecoins.accounting.admin-query.tce.read
status: confirmed
process: simplecoins.accounting.admin-query
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#発行・照会・配布予約
refs: 
  - simplecoins.accounting.admin-query.pi.read
semantic_contract: markdown-v2
language: ja
---

# TCE: 管理照会

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| deny | 管理照会 | `入力.管理権限 == 偽` | 1 | `出力.結果 = "拒否"` |
| unavailable | 管理照会 | `入力.管理権限 == 真 かつ 入力.確定集計 == 偽` | 1 | `出力.結果 = "未確定"` |
| unregistered | 管理照会 | `入力.管理権限 == 真 かつ 入力.確定集計 == 真 かつ 入力.照会種別 == "個人" かつ 入力.登録済 == 偽` | 1 | `出力.結果 = "未登録"` |
| display | 管理照会 | `入力.管理権限 == 真 かつ 入力.確定集計 == 真 かつ (入力.照会種別 != "個人" または 入力.登録済 == 真)` | 1 | `出力.結果 = "表示"` |
