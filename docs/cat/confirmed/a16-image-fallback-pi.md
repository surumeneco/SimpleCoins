---
cat_version: '0.3'
kind: pi
id: simplecoins.presentation.pi.image-selection
status: confirmed
process: simplecoins.presentation.currency-image-selection
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
decision_ref: user-chat:2026-11-18:A16-image-fallback-approved
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: 額面画像選択の入力と結果

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.presentation.currency-pack | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.候補重複 | 受信 | 真偽 | 必須 |
| 入力.一致画像あり | 受信 | 真偽 | 必須 |
| 入力.一致画像有効 | 受信 | 真偽 | 必須 |
| 出力.選択 | 送信 | 列挙{custom,standard,reject} | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| select | 受付 | 画像選択 |

## 制約
| 式 |
| --- |

## 項目と外部証拠

- `候補重複`は表示名の半角スペース・`_`・`-`を同じ区切りに正規化した後、同額面に一致する画像候補が複数存在すること。
- `一致画像あり`は対応する候補PNGが1件以上見つかったこと。`一致画像有効`は一意な候補が許容された正規のPNGであるという、ファイルの実バイト・パスを検査した結果である。候補が無い場合、この真偽は画像を選択するためには利用しない。
- `custom`は一致する1件の外部画像、`standard`は額面で固定された内蔵標準画像、`reject`は入力をエラーとして拒否する。選択したバイト列を必ず安全に読み取れたか、出力アーカイブを原子的に公表できるかは別の技術検査とする。
