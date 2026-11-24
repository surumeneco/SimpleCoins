---
cat_version: '0.3'
kind: tce
id: simplecoins.settlement.writer-epoch-publication.tce.authorize
status: confirmed
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-24:approved-revised-simplecoins-scope
refs:
  - simplecoins.settlement.writer-epoch-publication.pi.authorize
semantic_contract: markdown-v2
language: ja
---

# TCE: G0全件決算の公開判定

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| p0 | 公開判定 | `入力.全対象読取完了 == 真 かつ 入力.帰属重複照合済 == 真 かつ 入力.競合解消済 == 真 かつ 入力.通常仕訳引継可能 == 真 かつ 入力.参加禁止かつ無人 == 真 かつ 入力.重大な不整合あり == 偽` | 1 | `出力.原子公開試行許可 = 真; 出力.未公開維持 = 偽; 出力.公開済を失効 = 偽` |
| p1 | 公開判定 | `入力.重大な不整合あり == 偽 かつ (入力.全対象読取完了 == 偽 または 入力.帰属重複照合済 == 偽 または 入力.競合解消済 == 偽 または 入力.通常仕訳引継可能 == 偽 または 入力.参加禁止かつ無人 == 偽)` | 1 | `出力.原子公開試行許可 = 偽; 出力.未公開維持 = 真; 出力.公開済を失効 = 偽` |
| p2 | 公開判定 | `入力.重大な不整合あり == 真` | 1 | `出力.原子公開試行許可 = 偽; 出力.未公開維持 = 真; 出力.公開済を失効 = 真` |
| r0 | 失効通知 | `入力.重大な不整合あり == 真` | 1 | `出力.原子公開試行許可 = 偽; 出力.未公開維持 = 真; 出力.公開済を失効 = 真` |
| r1 | 失効通知 | `入力.重大な不整合あり == 偽` | 1 | `出力.原子公開試行許可 = 偽; 出力.未公開維持 = 偽; 出力.公開済を失効 = 偽` |

本表は宣言された判定の閉包であり、実環境の観測・保存・公開・配布の実行成功を保証する証拠ではない。
