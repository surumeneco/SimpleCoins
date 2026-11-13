---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-20-exchange-bridge
status: candidate
process: simplecoins.paper.coin-exchange
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/pull/315
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# 20種類の等価両替: 既存実装の技術Candidate

これは正式にConfirmedへ昇格したCAT仕様ではない。確定済みゲームプレイ要件を実現するためのPaper側技術候補を、旧PR #315の現行コード・テストから切り出す。

## 対象

| Ref | Aspect |
| --- | --- |
| exchange-formula | 四つの基本値nについて五種類、合計20種類の等価両替 |
| paper-recipe | Paper 26.2の不定形レシピ・原材料の固定PDC識別 |
| paper-guard | 通貨を通常の紙クラフトへ流用しないイベント境界 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| EXCHANGE-01 | n=1,10,100,1000の5変換 | 合計20レシピ、すべて投入総額=出力総額 | Formula unit tests |
| EXCHANGE-02 | 同一登録時の重複・途中失敗 | 途中まで追加した通貨レシピを除去し、登録状態を残さない | Paper integration still required |
| EXCHANGE-03 | 他の通常レシピが通貨PAPERを材料に使う | 通貨PDC識別により消費を拒否 | JE/BE, Shift, crafter acceptance required |
| EXCHANGE-04 | 表示名を変更した既存の正規通貨 | 表示文字列ではなくPDCとitem_modelによって識別し両替を維持 | Paper integration required |

## Source observations and unresolved concerns

- 旧PR #315には `CoinExchangeFormula` と `CoinExchangeRecipes`、式のUnitテストがある。現行developには `CoinItemCodec` は既に存在し、旧PRとGit blobが同一である。再実装や上書きを行わない。
- 既存旧DraftのPI/TCEはルールIDに `n1-f1` を記す一方、実コードは `n1_f1` を使っている。これは未解消の規範記述と実装の不一致であり、旧DraftをConfirmedとは扱わない。
- 今回の独立変更はコードと単体試験の取り込みであり、プラグインのリスナー登録、実ワールドの在庫変更、JE/BE/Geyserによる操作、仕訳の耐久性は含まない。これらは正式受入条件の未完了事項である。
- このCandidateの構文検査はCAT正式GateのPassではない。Process Closure、Model Conformance、current-item、implementation-conformanceは別工程で検証する。
