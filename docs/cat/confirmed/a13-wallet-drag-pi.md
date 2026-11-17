---
cat_version: '0.3'
kind: pi
id: simplecoins.wallet.drag-isolation.pi.assess
status: confirmed
process: simplecoins.wallet.drag-isolation
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A13-lower-only-drag-correction-approved
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: ドラッグ状態の検証済み入力と取消結果

## 境界
| 相手Process | 方向 |
| --- | --- |
| paper.wallet-inventory-drag-adapter | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.先行キャンセル | 受信 | 真偽 | 必須 |
| 入力.財布セッション有効 | 受信 | 真偽 | 必須 |
| 入力.ドラッグ元が財布 | 受信 | 真偽 | 必須 |
| 入力.下段だけ変更 | 受信 | 真偽 | 必須 |
| 入力.固定財布枠に接触 | 受信 | 真偽 | 必須 |
| 出力.取消 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| assess | 受付 | ドラッグ判定 |

## 制約
| 式 |
| --- |

## 入力の意味

- 先行キャンセルはイベントの処理開始時点ですでに取消済みであること。この場合は再許可しない。
- 財布セッション有効はプレイヤーUUIDと当該GUIの保持者が一致し、使用中の固定ホットバースロットに同じ財布UUIDの正規財布が現在もあること。判定不能は偽とする。
- ドラッグ元が財布は、ドラッグ開始時にカーソルに持っていたItemStackに財布PDCマーカーが存在すること。改変・不正な財布でも真とする。
- 下段だけ変更は、変更対象のraw slotが全てそのviewの下段プレイヤーインベントリに所属し、上段9スロットや外部/不明スロットを含まないこと。
- 固定財布枠に接触は、変更対象のraw slotのうち、viewを通して変換したプレイヤーインベントリのローカルslotが、GUIを開いたときの財布保持ホットバースロットに一致すること。raw slot番号とhotbar番号をそのまま比較しない。
- 取消が偽は、このドラッグをSimpleCoins側から追加で拒否しないこと。既存キャンセルの解除を指示しない。
