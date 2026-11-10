---
cat_version: '0.3'
kind: process
id: simplecoins.paper.trader-event
status: draft
process: simplecoins.paper.trader-event
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.trader-event
semantic_contract: markdown-v2
language: ja
---

# Process: 行商人の発行イベントと復旧境界 (Draft)

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.paper.pi.trader-event |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 行商人死亡 | simplecoins.paper.pi.trader-event/onDeath |

## 実装の境界
確定要件: 行商人の死亡で低確率貨幣発行。配置する貨幣の価値を不正に増減させない。イベント重複では二重発行しない。発行価値は統計仕訳に一回だけ通知する。

技術候補: 行商人 UUID を一意な発行IDとして、設定を一度抽選し、未発行・無発行・発行準備中・イベントへ追加した状態を区別する。発行準備中の永続化に失敗したら貨幣を一切追加せず処理停止。ドロップ追加失敗時は当該イベントへ追加した分を元に戻す。永続レコードが既にある同じ UUID は再抽選せず再発行しない。再起動時の未確定レコードは自動承認せず、全件走査・仕訳照合完了まで発行を止める。

**留保**: JavaイベントのドロップリストとMinecraft世界保存と仕訳ファイルの原子的同時確定は未実証。COMMITTEDというイベント内記録だけではゲーム世界へ保存済みであるとは主張しない。安全な全件再決算・入場制御の受入までは本番自動発行しない。設定キー/確率の規範Confirmedは別途正式決定を要する。