---
cat_version: '0.3'
kind: tce
id: simplecoins.accounting.wcl-cargo-source.tce.capture
status: confirmed
process: simplecoins.accounting.wcl-cargo-source
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/681
decision_ref: user-chat:2026-11-24:continue-and-complete-PR681;approved-02B:通常時の仕訳・差分反映
refs: 
  - simplecoins.accounting.wcl-cargo-source.pi.capture
semantic_contract: markdown-v2
language: ja
---

# TCE: 通常時の収納読取源選択

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| open | 収納読取 | `入力.GUI状態 == "開" かつ 入力.正本確認 == 真` | 1 | `出力.読取源 = "GUI優先"` |
| closed | 収納読取 | `入力.GUI状態 == "閉" かつ 入力.正本確認 == 真` | 1 | `出力.読取源 = "WCL"` |
| hold | 収納読取 | `入力.GUI状態 == "不明" または 入力.正本確認 == 偽` | 1 | `出力.読取源 = "保留"` |
