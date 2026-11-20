---
cat_version: '0.3'
kind: tce
id: simplecoins.blocklocker.saved-owner.tce.classify
status: draft
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: ''
refs:
  - simplecoins.blocklocker.saved-owner.pi.classify
semantic_contract: markdown-v2
language: ja
---

# TCE: SAVED BlockLocker保護帰属の未公開候補（Draft）

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| f0 | 保存保護照合 | `入力.保存地形ソース同定済 == 真 かつ 入力.全関連看板照合済 == 真 かつ 入力.物理収納重複排除済 == 真 かつ 入力.保存源局所世代整合済 == 真 かつ 入力.矛盾あり == 偽 かつ 入力.保護適用を検証済 == 偽 かつ 入力.非保護を検証済 == 真 かつ 入力.主所有者UUID検証済 == 偽 かつ 入力.Everyone有効を検証済 == 偽 かつ 入力.Everyone不在を検証済 == 偽` | 1 | `出力.所有者候補 = 偽; 出力.フリー候補 = 真; 出力.決算保留 = 偽` |
| f1 | 保存保護照合 | `入力.保存地形ソース同定済 == 真 かつ 入力.全関連看板照合済 == 真 かつ 入力.物理収納重複排除済 == 真 かつ 入力.保存源局所世代整合済 == 真 かつ 入力.矛盾あり == 偽 かつ 入力.保護適用を検証済 == 真 かつ 入力.非保護を検証済 == 偽 かつ 入力.Everyone有効を検証済 == 真 かつ 入力.Everyone不在を検証済 == 偽` | 1 | `出力.所有者候補 = 偽; 出力.フリー候補 = 真; 出力.決算保留 = 偽` |
| o0 | 保存保護照合 | `入力.保存地形ソース同定済 == 真 かつ 入力.全関連看板照合済 == 真 かつ 入力.物理収納重複排除済 == 真 かつ 入力.保存源局所世代整合済 == 真 かつ 入力.矛盾あり == 偽 かつ 入力.保護適用を検証済 == 真 かつ 入力.非保護を検証済 == 偽 かつ 入力.主所有者UUID検証済 == 真 かつ 入力.Everyone有効を検証済 == 偽 かつ 入力.Everyone不在を検証済 == 真` | 1 | `出力.所有者候補 = 真; 出力.フリー候補 = 偽; 出力.決算保留 = 偽` |
| h0 | 保存保護照合 | `入力.保存地形ソース同定済 == 偽 または 入力.全関連看板照合済 == 偽 または 入力.物理収納重複排除済 == 偽 または 入力.保存源局所世代整合済 == 偽 または 入力.矛盾あり == 真 または (入力.保護適用を検証済 == 偽 かつ 入力.非保護を検証済 == 偽) または (入力.保護適用を検証済 == 真 かつ 入力.非保護を検証済 == 真) または (入力.非保護を検証済 == 真 かつ (入力.主所有者UUID検証済 == 真 または 入力.Everyone有効を検証済 == 真 または 入力.Everyone不在を検証済 == 真)) または (入力.保護適用を検証済 == 真 かつ 入力.Everyone有効を検証済 == 真 かつ 入力.Everyone不在を検証済 == 真) または (入力.保護適用を検証済 == 真 かつ 入力.Everyone有効を検証済 == 偽 かつ (入力.Everyone不在を検証済 == 偽 または 入力.主所有者UUID検証済 == 偽))` | 1 | `出力.所有者候補 = 偽; 出力.フリー候補 = 偽; 出力.決算保留 = 真` |

## Draftの検証範囲

10個の真偽入力の1024組合せを有界列挙した結果、f0=1、f1=2、o0=1、h0=1020で、各組合せが一行にのみ属する。これはDraftの局所的な論理分割検査であり、正式CAT Process Closure、Model Conformance、本人の規範承認ではない。

Everyone有効なら主所有者UUIDが未知でもFree。保護あり・Everyone不在・UUID検証済みなら所有者候補。積極的に検証済みの非保護だけFree。看板不足、支持先・保存元・世代不明、保護と非保護の矛盾、Everyoneの有効/不在未確認、主所有者UUID不明、他の入力矛盾はHOLD。所有者のP/D分類・全writer閉包・G0原子公開・G3 ACK・Admissionを本契約だけで認めない。
