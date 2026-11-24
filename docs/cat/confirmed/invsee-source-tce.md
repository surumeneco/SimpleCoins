---
cat_version: '0.3'
kind: tce
id: simplecoins.accounting.invsee-source.tce.capture
status: confirmed
process: simplecoins.accounting.invsee-source
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/Jannyboy11/InvSee-plus-plus/blob/v0.31.22/InvSee%2B%2B_Common/src/main/java/com/janboerman/invsee/spigot/api/InvseeAPI.java
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs:
  - simplecoins.accounting.invsee-source.pi.capture
semantic_contract: markdown-v2
language: ja
---

# TCE: 対象収納の正本選択

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| live | 収納読取 | `入力.オンライン == 真` | 1 | `出力.読取源 = "実Player"` |
| editor | 収納読取 | `入力.オンライン == 偽 かつ 入力.有効編集画面 == 真` | 1 | `出力.読取源 = "編集画面"` |
| saved | 収納読取 | `入力.オンライン == 偽 かつ 入力.有効編集画面 == 偽` | 1 | `出力.読取源 = "保存"` |
