---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.journal.receipt-nontail-integrity
status: draft
process: simplecoins.journal
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: wasApplied must verify non-tail durable reservation (CAT Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.journal | `GrantLedger.wasApplied` の保存済みreceiptと対象grantの照合 |
| simplecoins.recovery | 後続の正常なtail/headが古いrecordの欠損・破損を隠さない |

PR #19の `acknowledge` におけるnon-tail record欠損・破損検出とは別に、`wasApplied` 自体が対象recordの永続証拠を再検証する必要性を検証する。現行 `checkHealthy` はheadとtailのみを照合するため、より古いrecordが欠損・破損していても、in-memory grantとreceiptの一致だけで `wasApplied` がtrueを返し得る。これは実際のplayerdataを読み取った証拠ではなく、保存済みreceiptを呼出側が与える純粋な境界テストである。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| journal.receipt.missing-target | head/tail正常、対象の古いgrant recordが欠損 | `wasApplied` はtrueを返さずfail-closedで拒否する | SC-RECEIPT-NONTAIL-01 |
| journal.receipt.corrupt-target | head/tail正常、対象の古いgrant recordが破損 | `wasApplied` はtrueを返さずfail-closedで拒否する | SC-RECEIPT-NONTAIL-02 |
| journal.receipt.substituted-target | head/tail正常、対象の古いgrant recordが別台帳の正常なchecksum付きrecordに置換 | `wasApplied` はin-memory UUIDと一致する永続証拠を要求し、trueを返さず拒否する | SC-RECEIPT-NONTAIL-04 |
| journal.receipt.intact-target | head/tailと対象recordが整合し、receiptが対象を指す | 既存の正当なtrue判定を維持する | SC-RECEIPT-NONTAIL-03 |

## Test model / TCE

- SC-RECEIPT-NONTAIL-01 (Red): 2件予約後、1件目の永続recordを削除して `wasApplied(first, first.sequence())` を呼ぶと拒否する。
- SC-RECEIPT-NONTAIL-02 (Red): 1件目の永続recordを破損させて同様に呼ぶと拒否する。
- SC-RECEIPT-NONTAIL-04 (Red): 別台帳の正常なchecksum付きrecordに1件目を置換しても拒否する。
- SC-RECEIPT-NONTAIL-03 (Green): 両recordが正常なら、1件目のreceiptをtrueとして扱える。

## 検証・Gate

対象はPR #19の現行 `GrantLedger.java` blob `6e9416d8ce74ba4a09ce74d72bd10bad05505461`。PR #19は別のDraft Redであり、本PRは新しいテストだけを積み重ねる。永続recordの全件検査を毎回要求するか、対象だけを検査するか、fail-closedの例外とpoisoning責務は規範決定を要する。

**Draft / not Confirmed.** CAT Process/PI/TCEの規範確認、process-closure、TestModel、model-conformance、vectors、Queue、current-item、implementation-conformanceおよびPaperクラッシュ復旧・playerdata受入は not-run。productionの変更・merge/deployを許可しない。
