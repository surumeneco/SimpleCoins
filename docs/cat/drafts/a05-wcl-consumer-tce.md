---
cat_version: '0.3'
kind: tce
id: simplecoins.external-storage-provider-integration.tce.capture
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/4
decision_ref: ''
refs:
  - simplecoins.external-storage-provider-integration.pi.capture
semantic_contract: markdown-v2
language: ja
---

# TCE: WCL保管証拠の採用・保留・対象外判定（Draft）

網羅要求: 部分

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| s0 | 収納照合 | `入力.必要提供者検証済 == 真 かつ 入力.対象個体確認済 == 真 かつ 入力.対象外検証済 == 偽 かつ 入力.読取成功 == 真 かつ 入力.個体と提供者の同一性一致 == 真 かつ 入力.帰属根拠確認済 == 真 かつ 入力.同一世代かつ重複排除済 == 真 かつ 入力.矛盾あり == 偽` | 1 | `出力.候補採用 = 真; 出力.対象外継続 = 偽; 出力.決算保留 = 偽` |
| s1 | 収納照合 | `入力.必要提供者検証済 == 真 かつ 入力.対象個体確認済 == 偽 かつ 入力.対象外検証済 == 真 かつ 入力.矛盾あり == 偽` | 1 | `出力.候補採用 = 偽; 出力.対象外継続 = 真; 出力.決算保留 = 偽` |
| s2 | 収納照合 | `入力.必要提供者検証済 == 偽 かつ 入力.矛盾あり == 偽` | 1 | `出力.候補採用 = 偽; 出力.対象外継続 = 偽; 出力.決算保留 = 真` |
| s3 | 収納照合 | `入力.必要提供者検証済 == 真 かつ 入力.対象個体確認済 == 真 かつ 入力.対象外検証済 == 偽 かつ 入力.読取成功 == 偽 かつ 入力.矛盾あり == 偽` | 1 | `出力.候補採用 = 偽; 出力.対象外継続 = 偽; 出力.決算保留 = 真` |
| s4 | 収納照合 | `入力.必要提供者検証済 == 真 かつ 入力.対象個体確認済 == 真 かつ 入力.対象外検証済 == 偽 かつ 入力.読取成功 == 真 かつ (入力.個体と提供者の同一性一致 == 偽 または 入力.帰属根拠確認済 == 偽 または 入力.同一世代かつ重複排除済 == 偽) かつ 入力.矛盾あり == 偽` | 1 | `出力.候補採用 = 偽; 出力.対象外継続 = 偽; 出力.決算保留 = 真` |
| s5 | 収納照合 | `入力.矛盾あり == 真` | 1 | `出力.候補採用 = 偽; 出力.対象外継続 = 偽; 出力.決算保留 = 真` |
| s6 | 収納照合 | `入力.必要提供者検証済 == 真 かつ 入力.対象個体確認済 == 偽 かつ 入力.対象外検証済 == 偽 かつ 入力.矛盾あり == 偽` | 1 | `出力.候補採用 = 偽; 出力.対象外継続 = 偽; 出力.決算保留 = 真` |

## 技術的な根拠と未決

- 既存の実Paper [PR #445](https://github.com/surumeneco/SimpleCoins/pull/445) と [PR #455](https://github.com/surumeneco/SimpleCoins/pull/455) は、配布WCL/WWC/WWWによる保存済み個体2件・合計25,000Cの読取、実Anvilソース発見、未ロードchunk維持を限定的に検証済み。これを再実装・再実験することは本Workの目的ではない。既存試験だけでは製品内の統合候補の同世代性や、全ワールドでの採用まで証明しない。
- `s0`は、各証拠の合致が本当に成立した**場合に限る未公開候補採用**。`s1`は積極的な対象外証拠がある場合だけで、欠落するprovider/個体を対象外扱いしない。`s2–s6`は不能・不明・矛盾・不一致ならfail-closedとする抽象化候補。
- このTCEは明示的に`網羅要求: 部分`とし、未知のBoolean入力組合せや観測不可能な同時代変化を一般的な「成功」に吸収しない。`s0`が最終公開の許可を出すわけではない。入力写像の正当性、G0 writer凍結、配備JARの同一性、全ソース索引とBAN/playerdataの時点整合、正式なCAT Process/Model Gateおよび実装は未完了。
- 現段階では成果物を**Draft**のまま維持し、正式な意味決定・Confirmed/Process Closureが成立するまでは製品コードの利用者向け動作を変更しない。
