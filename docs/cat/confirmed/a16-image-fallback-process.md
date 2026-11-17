---
cat_version: '0.3'
kind: process
id: simplecoins.presentation.currency-image-selection
status: confirmed
process: simplecoins.presentation.currency-image-selection
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A16-image-fallback-approved
refs:
  - simplecoins.presentation.pi.image-selection
semantic_contract: markdown-v2
language: ja
---

# Process: 額面画像の決定

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.presentation.pi.image-selection |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 画像選択 | simplecoins.presentation.pi.image-selection/select |

## 規範意味と対象外

各額面で、設定された表示名に正規化一致する有効なPNGが1件ならそれを使用し、一致するPNGがない場合は当該額面の**内蔵標準画像**を使う。表示名・画像ファイル名が変わっても固定PDC、額面、item_model、元の物理貨幣の価値は変えない。複数候補で曖昧、または存在する候補が無効ならパック生成を拒否し、標準画像に無言で切り替えない。これは2026-11-18に利用者が明示承認した表示名変更時の振る舞いの形式化である。

対象は**1額面について正規化照合を済ませた画像選択**。ファイル操作の原子性、画像デコーダーの妥当性、設定された全額面間の名前重複、Nexo/GEYSER配布、JE/BE描画、入室ゲートはこの局所Processの外部責務であり、機能実装・安全性は別途検証する。
