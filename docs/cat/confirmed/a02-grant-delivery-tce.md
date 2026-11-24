---
cat_version: '0.3'
kind: tce
id: simplecoins.grants.tce.delivery
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

# TCE: G3配布予約の保存確認と完了判定

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| prepare0 | 配布検査 | `入力.矛盾あり == 真 かつ 入力.未完了予約あり == 真` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 真` |
| prepare1 | 配布検査 | `入力.矛盾あり == 真 かつ 入力.未完了予約あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 偽; 出力.異常遮断 = 真` |
| prepare2 | 配布検査 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 偽; 出力.異常遮断 = 偽` |
| prepare3 | 配布検査 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 真 かつ 入力.全額の保存確認済 == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 偽` |
| prepare4 | 配布検査 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 真 かつ 入力.全額の保存確認済 == 真` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 偽` |
| verify0 | 保存後照合 | `入力.矛盾あり == 真 かつ 入力.未完了予約あり == 真` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 真` |
| verify1 | 保存後照合 | `入力.矛盾あり == 真 かつ 入力.未完了予約あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 偽; 出力.異常遮断 = 真` |
| verify2 | 保存後照合 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 偽; 出力.異常遮断 = 偽` |
| verify3 | 保存後照合 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 真 かつ 入力.全額の保存確認済 == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 偽` |
| verify4 | 保存後照合 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 真 かつ 入力.全額の保存確認済 == 真` | 1 | `出力.ack許可 = 真; 出力.予約維持 = 偽; 出力.異常遮断 = 偽` |
| recover0 | クラッシュ復旧 | `入力.矛盾あり == 真 かつ 入力.未完了予約あり == 真` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 真` |
| recover1 | クラッシュ復旧 | `入力.矛盾あり == 真 かつ 入力.未完了予約あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 偽; 出力.異常遮断 = 真` |
| recover2 | クラッシュ復旧 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 偽; 出力.異常遮断 = 偽` |
| recover3 | クラッシュ復旧 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 真 かつ 入力.全額の保存確認済 == 偽` | 1 | `出力.ack許可 = 偽; 出力.予約維持 = 真; 出力.異常遮断 = 偽` |
| recover4 | クラッシュ復旧 | `入力.矛盾あり == 偽 かつ 入力.未完了予約あり == 真 かつ 入力.全額の保存確認済 == 真` | 1 | `出力.ack許可 = 真; 出力.予約維持 = 偽; 出力.異常遮断 = 偽` |

本表は宣言された判定の閉包であり、実環境の観測・保存・公開・配布の実行成功を保証する証拠ではない。
