---
cat_version: '0.3'
kind: pi
id: simplecoins.grants.pi.delivery
status: confirmed
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-24:approved-revised-simplecoins-scope
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: G3配布予約の保存確認と完了判定

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.playerdata-persistence | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.未完了予約あり | 受信 | 真偽 | 必須 |
| 入力.全額の保存確認済 | 受信 | 真偽 | 必須 |
| 入力.矛盾あり | 受信 | 真偽 | 必須 |
| 出力.ack許可 | 送信 | 真偽 | 必須 |
| 出力.予約維持 | 送信 | 真偽 | 必須 |
| 出力.異常遮断 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| prepare | 受付 | 配布検査 |
| verify | 受付 | 保存後照合 |
| recover | 受付 | クラッシュ復旧 |

## 制約
| 式 |
| --- |

## 項目の意味

- 未完了予約あり：永続予約台帳に当該対象の未完了予約がある。ACK済み履歴は再発行しない。
- 全額の保存確認済：対象予約について現物引渡しと受領情報の整合を確認し、全額が保存された状態。部分額の保存は偽。保存の確認が不明・失敗なら矛盾ありとして扱う。通常の後続取引後に元スロットへ現物が残ることは要求しない。
- 矛盾あり：保存失敗、台帳/受領情報の矛盾、既知の未解消保存競合等により引渡し済額を確定できない状態。既知の保存失敗や破損を正常未配布に変換しない。任意外部コード耐性を意味しない。
- ack許可は完了記録を試みる許可であり、記録成功ではない。予約維持が偽でも完了書込失敗時に予約を消さない。異常遮断は対象配布の自動操作を止めることであり全員の接続を拒否する指示ではない。
