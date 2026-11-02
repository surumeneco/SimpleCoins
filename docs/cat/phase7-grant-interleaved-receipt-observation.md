---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.interleaved-receipt-observation
status: candidate
process: simplecoins.grant
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: 複数プレイヤー交互発行時のreceipt境界（code-to-spec Candidate）

## 観測対象

現行 `GrantLedger`（Git blob `df93d2c042cf83601a6c9d7492601579a3bfe4fc`）の `validateReceipt` / `acknowledge` / `pending` が、プレイヤーごとに連続しないグローバルsequenceを扱う既存動作を観測する。既存 `GrantLedgerTest` の単一プレイヤー連続予約と異なり、A/B/Aの順で予約する。新たな規範仕様の決定ではない。

| ID | 既存分岐 | 観測結果 | 検証 |
| --- | --- | --- | --- |
| GRANT-INTERLEAVE-01 | A#1、B#2、A#3の予約と再起動 | Aには2件、Bには1件の未配送予約が残る | `pending`をUUIDごとに照合 |
| GRANT-INTERLEAVE-02 | A#1を保存済みreceiptとして確認後、A#3が未配送 | A#1を重複配送扱いにし、A#3を未配送扱いに保つ | `wasApplied` / `acknowledge` / 再起動 |
| GRANT-INTERLEAVE-03 | AにB#2のreceipt、BにA#1のreceiptを渡す | 他プレイヤーのsequenceを受理しない | `validateReceipt`例外 |
| GRANT-INTERLEAVE-04 | A#1が未配送のままA#3のreceiptを提示 | Aの未配送予約を飛び越えたreceiptを拒否する | `validateReceipt`例外 |
| GRANT-INTERLEAVE-05 | A#1・B#2・A#3を各本人のreceiptで確認・ack後に再起動 | いずれの予約も未配送として再出現しない | `pending`空リスト |

## 限界

テストは既存のディスクjournal/receipt判定のcode-to-spec観測のみ。Paperの実playerdata保存・インベントリ反映・Bukkitスケジューラ・オンラインgive・Geyser・Nexoは未実行。CAT規範確認とprocess-closure/model-conformance/implementation-conformanceはnot-run。TASK-20261101-006全体の完了証拠ではない。
