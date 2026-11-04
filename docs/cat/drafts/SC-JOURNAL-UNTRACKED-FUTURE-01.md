# CAT Draft — SC-JOURNAL-UNTRACKED-FUTURE-01

Status: Draft (not Confirmed)
Task: TASK-20261101-005
Base: SimpleCoins PR #185 at cc97aeafbe4550cd67c04272f79521455ccabccc
Flow: issue-work candidate; no normative Gate or production implementation authorized.

## Process / PI candidate
A single-writer GrantLedger instance has a valid durable head and tracked reservations. An unexpected, syntactically valid future-sequence `.grant` record appears in its journal directory without being reserved through this instance. A subsequent read of pending grants must not report the in-memory snapshot as authoritative while untracked durable issuance evidence exists.

## TCE / oracle
Given an empty journal, reserve one grant, copy its durable bytes to a future-sequence filename (without updating the head), then call `pending(player)`. Expected fail-closed: `IllegalStateException`, not a normal pending list. The record is intentionally a valid checksummed payload but misnamed; this distinguishes unexpected file discovery from corrupted-tail checks.

## Status and limitations
This is a candidate safety invariant, not an approved requirement. It is distinct from PR #185's deleted/corrupted older-record acknowledgement tests. Before Green implementation, obtain normative CAT confirmation and run canonical process-closure, TestModel/vectors/Queue/current-item and model-conformance Gates. No Paper live verification claimed.
