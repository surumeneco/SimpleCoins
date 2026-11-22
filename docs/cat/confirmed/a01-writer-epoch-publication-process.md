---
cat_version: '0.3'
kind: process
id: simplecoins.settlement.writer-epoch-publication
status: confirmed
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs:
  - simplecoins.settlement.writer-epoch-publication.pi.authorize
semantic_contract: markdown-v2
language: ja
---

# Process: G0全writer保護世代の決算公開判定

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.settlement.writer-epoch-publication.pi.authorize |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |
| 観測.全ソース確定 | 設定対象の全World、LIVE/SAVED、playerdata、外部収納、帰属、BANの独立証拠 | 真偽 |
| 観測.全writer閉包 | Paper native、全有効Plugin、非同期処理、直接ファイルwriterの入場・排出証拠 | 真偽 |
| 観測.世代継続 | 全候補取得から原子公開まで排他された同一会計世代 | 真偽 |
| 観測.通常仕訳引継 | 公開後の正常変更を欠落・重複なく追跡するJournal監視 | 真偽 |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |
| 作用.公開試行許可 | 原子的な統計Snapshot切替の試行権限 | 真偽 |
| 作用.未公開保留 | 既存の入室拒否と未公開決算候補 | 真偽 |
| 作用.公開失効 | 公開済みSnapshotと独立入室許可の取消 | 真偽 |

## 起点
| ID | 由来 |
| --- | --- |
| 公開判定 | simplecoins.settlement.writer-epoch-publication.pi.authorize/evaluate |
| 失効通知 | simplecoins.settlement.writer-epoch-publication.pi.authorize/revoke |

## 正本保証と技術境界

Drive 02B/04で既決のG0保証を緩めない。0人の走査で全貨幣・財布を一つの正本世代で決算し、失敗した候補・統計は公開せず、入室も許可しない。本Processは、既存のPaperSavedSourceDiscovery、PaperCapturedHybridSettlement、SettlementScanRunner、SettlementSnapshotPublisherをつなぐ際の公開判定を定める規範であり、下位スキャナーの実装選択を規定しない。

全writerは、受付拒否だけでなく既入場済み処理の排出を要求し、候補取得・保存読取・BAN/所有者・重複解消・通常仕訳への引継・原子切替終了まで保護を維持する。Paper main-threadのイベント取消、0人、JAR一覧、最終hash再検査では十分でない。未観測・不一致・出所不明を成功や資産0に変換しない。現24-JARの動的実行とPaper native writerを全て制御できた証拠は**まだない**。

出力「公開試行許可」は原子publishの完了・耐久化・入室Permitを意味しない。保護逸脱やJournal監視失効は旧候補の再公開ではなく即時失効へ進む。正当な公開後仕訳は継続できる設計が必要。Processの閉世界性は入出力の判定表を指し、実サーバーの全writer閉包の成立とは別。正式CATのModel/Implementation Gate・配備・入室解禁は別途検証を要する。
