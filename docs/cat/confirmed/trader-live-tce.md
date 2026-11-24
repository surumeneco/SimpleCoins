---
cat_version: '0.3'
kind: tce
id: simplecoins.paper.trader-live-issuance.tce.event
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

# TCE: simplecoins.paper.trader-live-issuance

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| case0 | イベント | `入力.準備済 == 偽 かつ 入力.行商人 == 偽 かつ 入力.キャンセル == 偽 かつ 入力.抽選成功 == 偽` | 1 | `出力.発行 = 偽` |
| case1 | イベント | `入力.準備済 == 偽 かつ 入力.行商人 == 偽 かつ 入力.キャンセル == 偽 かつ 入力.抽選成功 == 真` | 1 | `出力.発行 = 偽` |
| case2 | イベント | `入力.準備済 == 偽 かつ 入力.行商人 == 偽 かつ 入力.キャンセル == 真 かつ 入力.抽選成功 == 偽` | 1 | `出力.発行 = 偽` |
| case3 | イベント | `入力.準備済 == 偽 かつ 入力.行商人 == 偽 かつ 入力.キャンセル == 真 かつ 入力.抽選成功 == 真` | 1 | `出力.発行 = 偽` |
| case4 | イベント | `入力.準備済 == 偽 かつ 入力.行商人 == 真 かつ 入力.キャンセル == 偽 かつ 入力.抽選成功 == 偽` | 1 | `出力.発行 = 偽` |
| case5 | イベント | `入力.準備済 == 偽 かつ 入力.行商人 == 真 かつ 入力.キャンセル == 偽 かつ 入力.抽選成功 == 真` | 1 | `出力.発行 = 偽` |
| case6 | イベント | `入力.準備済 == 偽 かつ 入力.行商人 == 真 かつ 入力.キャンセル == 真 かつ 入力.抽選成功 == 偽` | 1 | `出力.発行 = 偽` |
| case7 | イベント | `入力.準備済 == 偽 かつ 入力.行商人 == 真 かつ 入力.キャンセル == 真 かつ 入力.抽選成功 == 真` | 1 | `出力.発行 = 偽` |
| case8 | イベント | `入力.準備済 == 真 かつ 入力.行商人 == 偽 かつ 入力.キャンセル == 偽 かつ 入力.抽選成功 == 偽` | 1 | `出力.発行 = 偽` |
| case9 | イベント | `入力.準備済 == 真 かつ 入力.行商人 == 偽 かつ 入力.キャンセル == 偽 かつ 入力.抽選成功 == 真` | 1 | `出力.発行 = 偽` |
| case10 | イベント | `入力.準備済 == 真 かつ 入力.行商人 == 偽 かつ 入力.キャンセル == 真 かつ 入力.抽選成功 == 偽` | 1 | `出力.発行 = 偽` |
| case11 | イベント | `入力.準備済 == 真 かつ 入力.行商人 == 偽 かつ 入力.キャンセル == 真 かつ 入力.抽選成功 == 真` | 1 | `出力.発行 = 偽` |
| case12 | イベント | `入力.準備済 == 真 かつ 入力.行商人 == 真 かつ 入力.キャンセル == 偽 かつ 入力.抽選成功 == 偽` | 1 | `出力.発行 = 偽` |
| case13 | イベント | `入力.準備済 == 真 かつ 入力.行商人 == 真 かつ 入力.キャンセル == 偽 かつ 入力.抽選成功 == 真` | 1 | `出力.発行 = 真` |
| case14 | イベント | `入力.準備済 == 真 かつ 入力.行商人 == 真 かつ 入力.キャンセル == 真 かつ 入力.抽選成功 == 偽` | 1 | `出力.発行 = 偽` |
| case15 | イベント | `入力.準備済 == 真 かつ 入力.行商人 == 真 かつ 入力.キャンセル == 真 かつ 入力.抽選成功 == 真` | 1 | `出力.発行 = 偽` |
