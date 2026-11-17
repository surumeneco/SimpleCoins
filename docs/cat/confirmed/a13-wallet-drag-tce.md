---
cat_version: '0.3'
kind: tce
id: simplecoins.wallet.drag-isolation.tce.assess
status: confirmed
process: simplecoins.wallet.drag-isolation
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A13-lower-only-drag-correction-approved
refs: 
  - simplecoins.wallet.drag-isolation.pi.assess
semantic_contract: markdown-v2
language: ja
---

# TCE: ドラッグの拒否・許可分岐

網羅要求: 閉包

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| s0 | ドラッグ判定 | `入力.先行キャンセル == 真` | 1 | `出力.取消 = 真` |
| s1 | ドラッグ判定 | `入力.先行キャンセル == 偽 かつ 入力.財布セッション有効 == 偽` | 1 | `出力.取消 = 真` |
| s2 | ドラッグ判定 | `入力.先行キャンセル == 偽 かつ 入力.財布セッション有効 == 真 かつ 入力.ドラッグ元が財布 == 真` | 1 | `出力.取消 = 真` |
| s3 | ドラッグ判定 | `入力.先行キャンセル == 偽 かつ 入力.財布セッション有効 == 真 かつ 入力.ドラッグ元が財布 == 偽 かつ 入力.下段だけ変更 == 偽` | 1 | `出力.取消 = 真` |
| s4 | ドラッグ判定 | `入力.先行キャンセル == 偽 かつ 入力.財布セッション有効 == 真 かつ 入力.ドラッグ元が財布 == 偽 かつ 入力.下段だけ変更 == 真 かつ 入力.固定財布枠に接触 == 真` | 1 | `出力.取消 = 真` |
| s5 | ドラッグ判定 | `入力.先行キャンセル == 偽 かつ 入力.財布セッション有効 == 真 かつ 入力.ドラッグ元が財布 == 偽 かつ 入力.下段だけ変更 == 真 かつ 入力.固定財布枠に接触 == 偽` | 1 | `出力.取消 = 偽` |

## 局所閉包

真偽5項目の全組合せを排他的に分類する。すでに取消されたイベントを再許可しない。これは判定規範であり、実Paperイベントのraw slotの全入力経路を検証済みという意味ではない。上段へのドラッグによる貨幣入出金はこの修正範囲では行わず、既存の拒否を維持する。
