---
cat_version: '0.3'
kind: tce
id: simplecoins.accounting.memory-journal.tce.record
status: confirmed
process: simplecoins.accounting.memory-journal
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: 
  - simplecoins.accounting.memory-journal.pi.record
semantic_contract: markdown-v2
language: ja
---

# TCE: 確定仕訳

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| duplicate | 確定仕訳 | `入力.順序 == "記帳済"` | 1 | `出力.結果 = "重複"` |
| apply | 確定仕訳 | `入力.順序 == "次" かつ 入力.差額整合 == 真` | 1 | `出力.結果 = "反映"` |
| reject | 確定仕訳 | `入力.順序 == "欠落" または (入力.順序 == "次" かつ 入力.差額整合 == 偽)` | 1 | `出力.結果 = "拒否"` |
