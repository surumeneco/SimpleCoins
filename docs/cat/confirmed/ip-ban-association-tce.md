---
cat_version: '0.3'
kind: tce
id: simplecoins.accounting.ip-ban-association.tce.resolve
status: confirmed
process: simplecoins.accounting.ip-ban-association
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view#IP-BANのUUID帰属（2026-11-25確定）
refs:
  - simplecoins.accounting.ip-ban-association.pi.resolve
semantic_contract: markdown-v2
language: ja
---

# TCE: IP BAN照合の優先順位

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| manual-match | 照合 | `入力.補正あり == 真 かつ 入力.補正一致 == 真` | 1 | `出力.IPBAN = 真` |
| manual-clear | 照合 | `入力.補正あり == 真 かつ 入力.補正一致 == 偽` | 1 | `出力.IPBAN = 偽` |
| auto-match | 照合 | `入力.補正あり == 偽 かつ 入力.自動一致 == 真` | 1 | `出力.IPBAN = 真` |
| auto-clear | 照合 | `入力.補正あり == 偽 かつ 入力.自動一致 == 偽` | 1 | `出力.IPBAN = 偽` |
