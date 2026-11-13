---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.trader-draw-and-stack-budget
status: candidate
process: simplecoins.trader-issuance
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/pull/316
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# 行商人ドロップ抽選・スタック分割: 独立Domain技術Candidate

このCandidateは、01の「非常に低い確率・額面/個数/重みは設定可能」要件に対応する純粋計算処理を旧PR #316から独立して取り込むもの。正式CAT Confirmed仕様ではない。

## 対象

| Ref | Aspect |
| --- | --- |
| trader-draw | 有効無効、確率、9額面の重み付き抽選と枚数の決定 |
| stack-plan | 64枚単位の分割と同期処理の1024スタック上限 |
| issuance-boundary | 抽選結果だけでは新しい貨幣を現物化しない |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| TD-01 | disabled または確率0 | 抽選結果を返さず発行しない | fixed-seed unit test |
| TD-02 | 有効かつ許容された正の重み | 重み0の額面を選ばず、枚数の両端を含めた抽選を行う | fixed-seed unit test |
| TD-03 | 枚数が64を超える | 1スタック64以下へ分割、合計枚数と額面総額を不変にする | stack boundary tests |
| TD-04 | 巨大な枚数で予算超過 | 全スタック生成前に拒否し、部分生成しない | bounded allocation tests |
| TD-05 | 不正な確率、重み、枚数 | 不正設定を黙って補正せず失敗させる | validation tests |

## 実装境界・未承認事項

- 本単位は乱数注入可能なDomain純粋計算とJUnitのみ。Paperイベント、実体の出現、重複イベント、永続発行台帳、統計、ログイン許可には接続しない。
- 旧PRの「未配線」部分と技術記述を正本として再評価する。実ゲーム内のデフォルト確率、設定ファイルへの採用、死亡時の最終ドロップは別途現行設計・configの検証が必要。
- 正式なProcess Closure、Model Conformance、current-item、implementation-conformanceはこのCandidateの構文確認では満たされない。
