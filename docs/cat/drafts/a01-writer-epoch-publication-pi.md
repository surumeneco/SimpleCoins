---
cat_version: '0.3'
kind: pi
id: simplecoins.settlement.writer-epoch-publication.pi.authorize
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: G0保護世代の公開・保留・失効判定（Draft）

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.settlement | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.必要ソース完全確認済 | 受信 | 真偽 | 必須 |
| 入力.新規writer入場拒否実効 | 受信 | 真偽 | 必須 |
| 入力.既入場writer排出完了 | 受信 | 真偽 | 必須 |
| 入力.全候補が同一保護世代 | 受信 | 真偽 | 必須 |
| 入力.帰属重複仕訳照合済 | 受信 | 真偽 | 必須 |
| 入力.通常仕訳監視への引継可能 | 受信 | 真偽 | 必須 |
| 入力.オンラインプレイヤー不在 | 受信 | 真偽 | 必須 |
| 入力.世代失効あり | 受信 | 真偽 | 必須 |
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

## 入力真偽の証拠責務

- 必要ソース完全確認済：全World、共有playerdata、LIVE/SAVED正本選択、BlockLocker有効保護、WCL・WWC・WWWと装備ミラー、BAN・財布UUIDと物理重複の全対象が欠落なく同じ世代にある。局所readerの成功・Manifestの発見だけでは偽。
- 新規writer入場拒否実効と既入場writer排出完了は別条件。既にイベントを通過した非同期保存やPaper native Player.saveData、任意ファイル書換えまで拘束できなければ偽。
- 全候補が同一保護世代：元LIVE取得から保存走査、全owner、BAN、Journal handoff、原子切替終了まで同一正本が維持されること。読み直し直後の割込みやABAが残れば偽。
- 帰属重複仕訳照合済：P/F/Dと残高の根拠、財布の二重ビュー、装備ミラー、保存・LIVE競合、通常Journalの境界を上位で確認。不明を空やFREEに変換しない。
- 通常仕訳監視への引継可能：公開後の正規変更を一度だけ反映し、監視障害では失効できること。
- オンラインプレイヤー不在：OP含む0人、独立入室Guardを確認するだけであり、全writer静止の代用ではない。
- 世代失効あり：writer逸脱、World集合入替、所有者・保存元矛盾、Journal停止など。未観測を「失効なし」と推測しない。どれか必須条件が未証明なら公開判定は保留。

原子公開試行許可は実publish成功やAdmission GuardのPermitではない。具体的排他手段・メインスレッド調停は実装依存の技術検証に残し、このPIで特定APIを強制しない。
