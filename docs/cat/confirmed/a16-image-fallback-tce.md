---
cat_version: '0.3'
kind: tce
id: simplecoins.presentation.tce.image-selection
status: confirmed
process: simplecoins.presentation.currency-image-selection
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
decision_ref: user-chat:2026-11-18:A16-image-fallback-approved
refs:
  - simplecoins.presentation.pi.image-selection
semantic_contract: markdown-v2
language: ja
---

# TCE: 額面画像の選択分岐

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| s0 | 画像選択 | `入力.候補重複 == 真` | 1 | `出力.選択 = "reject"` |
| s1 | 画像選択 | `入力.候補重複 == 偽 かつ 入力.一致画像あり == 真 かつ 入力.一致画像有効 == 真` | 1 | `出力.選択 = "custom"` |
| s2 | 画像選択 | `入力.候補重複 == 偽 かつ 入力.一致画像あり == 真 かつ 入力.一致画像有効 == 偽` | 1 | `出力.選択 = "reject"` |
| s3 | 画像選択 | `入力.候補重複 == 偽 かつ 入力.一致画像あり == 偽` | 1 | `出力.選択 = "standard"` |

## 限定した閉包

3入力Booleanの全組合せを覆うのは「検証済みの候補判定」からの選択であり、ファイル変更競合やプラグイン停止・Nexo/Geyser配信経路まで実証するものではない。画像の名前のみ変わり有効な候補が存在しない場合の**standard**の選択を確定要求とする。
