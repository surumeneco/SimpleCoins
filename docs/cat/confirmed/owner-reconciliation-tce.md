---
cat_version: '0.3'
kind: tce
id: simplecoins.accounting.owner-reconciliation.tce.replace
status: confirmed
process: simplecoins.accounting.owner-reconciliation
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: 
  - simplecoins.accounting.owner-reconciliation.pi.replace
semantic_contract: markdown-v2
language: ja
---

# TCE: 所有者再分類

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| hold | 再分類 | `入力.完全 == 偽` | 1 | `出力.結果 = "保留"` |
| banned | 再分類 | `入力.完全 == 真 かつ 入力.BAN == 真` | 1 | `出力.結果 = "死蔵BAN"` |
| missing | 再分類 | `入力.完全 == 真 かつ 入力.BAN == 偽 かつ 入力.playerdata == 偽` | 1 | `出力.結果 = "死蔵欠損"` |
| registered | 再分類 | `入力.完全 == 真 かつ 入力.BAN == 偽 かつ 入力.playerdata == 真 かつ (入力.登録根拠 == 真 または 入力.残存 == 真)` | 1 | `出力.結果 = "個人登録"` |
| unregistered | 再分類 | `入力.完全 == 真 かつ 入力.BAN == 偽 かつ 入力.playerdata == 真 かつ 入力.登録根拠 == 偽 かつ 入力.残存 == 偽` | 1 | `出力.結果 = "個人未登録"` |
