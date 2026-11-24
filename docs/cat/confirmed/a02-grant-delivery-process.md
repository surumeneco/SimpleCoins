---
cat_version: '0.3'
kind: process
id: simplecoins.grants.paper-save-commit
status: confirmed
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-24:approved-revised-simplecoins-scope
refs:
  - simplecoins.grants.pi.delivery
semantic_contract: markdown-v2
language: ja
---

# Process: G3配布予約の保存確認と完了判定

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.grants.pi.delivery |

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
| 配布検査 | simplecoins.grants.pi.delivery/prepare |
| 保存後照合 | simplecoins.grants.pi.delivery/verify |
| クラッシュ復旧 | simplecoins.grants.pi.delivery/recover |

## 保証境界

予約受付は発行ではない。対象UUIDの予約を保持し、次回ログインで実貨幣を渡す。正常な切断・満杯・再起動・代表的保存失敗では未配布分を消さず確認済み分を再発行しない。全額の保存確認後だけ予約を完了する。部分配布と未配布はこの完了判定では同じ予約維持だが、実配布サービスは受領済額を保存して残額を区別する。矛盾時は当該配布の自動再発行・完了を止め、記録を保持する。対象単位の直列処理と同じplayerdataの受領情報を第一候補とし、既知の通常運用の保存競合を処理する。任意writer・署名済証明・全サーバー排他・全割込み点への耐性を必須としない。
