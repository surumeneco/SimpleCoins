---
title: Phase 6 wasApplied 非末尾耐久証拠の検証候補
status: draft
flow: code-to-spec
source: src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
source_ref: feature/phase6-journal-nontail-integrity-20261102-queue-c@6beae7dc296965243657cdcda1810607327e739e
authority: implementation-derived-candidate
---

# wasApplied 非末尾レコード破損の検証候補

## 適用境界
既存の `GrantLedger.wasApplied(grant, receipt)` に対する独立した読取境界の回帰候補。正式なCAT規範仕様ではなく、ユーザーによる意味論確認・昇格、機械Gateは未実施。非末尾の破損については既存PR #93の `pending` / `reserve` Redテストと原因を共有するが、`wasApplied` の直接呼出しは別の受領判定入口となる。

## 観測したコード契約
- `wasApplied` は `checkHealthy()` を呼んだ後、入力grantとメモリ上のgrantを照合し、receiptを検証する。
- `checkHealthy()` はheadと最新末尾grantを永続層から再読取するが、非末尾grantの耐久性を再確認しない。
- 非末尾レコードを削除または破損した状態で、元のgrantと正しいreceiptを渡すと、永続的な予約証拠がないにもかかわらず `wasApplied` がtrueを返す可能性がある。

## 検証ベクトル（Candidate）
| ID | 入力と故障注入 | 期待する振る舞い |
| --- | --- | --- |
| SC-WASAPPLIED-NONTAIL-01 | 2件予約後、1.grantを削除し、1件目とreceipt=1を照合 | fail-closed。trueを返さない |
| SC-WASAPPLIED-NONTAIL-02 | 2件予約後、1.grantを破損し、1件目とreceipt=1を照合 | fail-closed。trueを返さない |
| SC-WASAPPLIED-NONTAIL-03 | 2件とも無傷で、1件目とreceipt=1を照合 | trueを返す |

## Gate・判定
この文書はcode-to-spec Draftであり、Confirmedではない。正式CAT意味論Gate、生成TestModel、Paper実機復旧試験はnot-run。Redを確認しても実装変更の承認と同一視しない。既存の非末尾整合性検証と一括で規範確認すること。
