# CAT Draft — SC-JOURNAL-ACK-PAYLOAD-01

Status: Draft / Candidate (not Confirmed)
Task: TASK-20261101-005
Flow: code-to-spec observation + proposed safety invariant
Base: SimpleCoins PR #185 HEAD cc97aeafbe4550cd67c04272f79521455ccabccc
Observed production blob: GrantLedger.java 6e9416d8ce74ba4a09ce74d72bd10bad05505461

## Observation

The existing `GrantLedger.wasApplied(Grant, receipt)` verifies the supplied grant's immutable fields (sequence, id, player, amount, issuer, createdMillis, denomination, coinCount) against the live journal. By contrast, `acknowledge(Grant, receipt)` looks up the journal's current grant using the supplied sequence/id, then calls `wasApplied(current, receipt)` with the **internal current value**, rather than the supplied argument.

Consequently, a caller supplying a different recipient, amount, issuer, or created timestamp but the same sequence and grant UUID may acknowledge the canonical grant. The durable journal then changes to delivered even though the supplied record was not equivalent to the current record. This is a code-derived hypothesis until the exact regression is executed; no external caller exploitability has been established.

## Candidate Process / PI / TCE

- `SC-JOURNAL-ACK-PAYLOAD-01`: when the supplied grant differs in recipient (same sequence and UUID), acknowledgement rejects it and does not mark the canonical grant delivered.
- `SC-JOURNAL-ACK-PAYLOAD-02`: immutable monetary amount or issuer mismatch cannot authorize acknowledgement.
- `SC-JOURNAL-ACK-PAYLOAD-03`: a supplied grant with a mismatched creation timestamp cannot authorize acknowledgement.
- `SC-JOURNAL-ACK-PAYLOAD-04`: an unmodified canonical grant with a matching saved receipt is acknowledged exactly once, and remains delivered after reopening the durable journal.

Failure oracle for 01–03: `IllegalStateException`; saved durable bytes remain unchanged, and pending canonical grant is still visible to the live ledger. A successful unchanged grant is a Green control, not approval of the proposed negative behavior.

## Decision and limits

This candidate is separate from PR #185's older-record durability tests and PR #201's untracked-future-record candidate. No direct production edit is authorized by this Draft. Before any production Green, obtain normative CAT confirmation and run the canonical process-closure, TestModel, model-conformance, vectors, Queue, current-item Red, implementation and integration Gates. Scope is argument integrity on the in-memory/durable issuance journal; no Paper integration, playerdata durability, or real crash recovery is claimed.
