---
cat_version: '0.3'
kind: pi
id: simplecoins.settlement.writer-epoch-publication.pi.authorize
status: confirmed
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-24:approved-revised-simplecoins-scope
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: G0全件決算の公開判定

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.settlement | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.全対象読取完了 | 受信 | 真偽 | 必須 |
| 入力.帰属重複照合済 | 受信 | 真偽 | 必須 |
| 入力.競合解消済 | 受信 | 真偽 | 必須 |
| 入力.通常仕訳引継可能 | 受信 | 真偽 | 必須 |
| 入力.参加禁止かつ無人 | 受信 | 真偽 | 必須 |
| 入力.重大な不整合あり | 受信 | 真偽 | 必須 |
| 出力.原子公開試行許可 | 送信 | 真偽 | 必須 |
| 出力.未公開維持 | 送信 | 真偽 | 必須 |
| 出力.公開済を失効 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| evaluate | 受付 | 公開判定 |
| revoke | 受付 | 失効通知 |

## 制約
| 式 |
| --- |

## 項目の意味

- 全対象読取完了：設定対象World、playerdata、LIVE/SAVED、外部収納等を漏れなく読取。破損・未読取は偽。
- 帰属重複照合済：02A/02Bに従う分類、入れ子、LIVE/SAVED、装備ミラー等の二重計上を解消した状態。
- 競合解消済：採用方式の対象範囲で、走査中の現物変化が欠損・重複を生じないよう停止・補正・再捕捉等を実施した状態。0人だけでは真としない。全writerの列挙や任意コード耐性を要求しない。
- 通常仕訳引継可能：正常運用の変化を集計に反映する処理が準備済み。永続Journalの常時監視は要件ではない。
- 参加禁止かつ無人：OPを含め入室禁止が実効でオンライン0人。
- 重大な不整合あり：未解消の決算矛盾・公開結果の正しさを失わせる既知の重大な障害。正常操作・軽微な差分訂正を失効理由にしない。失効通知のr1は新しい許可を発行しない。
