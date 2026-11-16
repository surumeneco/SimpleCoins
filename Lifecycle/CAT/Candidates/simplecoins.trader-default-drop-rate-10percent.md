---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.trader-default-drop-rate-10percent
status: candidate
process: simplecoins.trader-drop.defaults
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# 行商人ドロップ発生確率10%へのデフォルト更新 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| trader-drop-config | `src/main/resources/config.yml` の初期値 |
| trader-drop-policy | `WanderingTraderDropPolicy` の死亡時確率判定 |
| configured-overrides | サーバー運営者が明示設定した `trader-drops.chance` / `probability` |
| coin-weights | 9額面の初期重み、合計511 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| trader-default-01 | 初回生成されるデフォルトconfig | `trader-drops.chance: 0.1`（10%）とする | 出荷YAMLの値・Reader |
| trader-default-02 | 確率10%で抽選する | 既存の独立死亡判定が10%となる | 固定seed 10,000回の境界検証 |
| trader-default-03 | 明示設定の既存configが読み込まれる | 明示値はそのまま反映し、自動で10%へ書換えない | 既存Readerの明示確率テスト |
| trader-default-04 | ドロップが発生する | 額面重み256/128/64/32/16/8/4/2/1、1枚、初期enabled=trueは不変 | 出荷YAMLの回帰 |
| trader-default-05 | Paper上の実プラグインが起動する | 初期確率10%をconfig Readerから読む | Paper 26.2 isolated smoke |

## Authority・Boundary

2026-11-12、利用者がデフォルト50%→10%への変更を明示。Drive 03の同じ正本IDへ反映済み。既存運営者configを上書きせず、確率判定だけを変更する。実通貨発行は、貨幣仕訳および決算が接続されるまで不可。正式CAT Process/PI/TCEのConfirmedやGate成功とは独立する。
