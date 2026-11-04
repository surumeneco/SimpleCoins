# Phase 6 non-tail durable journal loss at write boundaries — CAT Draft

Status: Draft (candidate, not Confirmed)
Task: TASK-20261101-005
Flow: code-to-spec
Target: GrantLedger.reserve / GrantLedger.acknowledge
Source: GrantLedger.java at stacked PR #166 HEAD fb9838e32f11203d30e23e65c1fc274c98340426

## Targets
- A live GrantLedger with two durable reservations and an intact head/tail.
- A previously durable first reservation removed from disk while the in-memory TreeMap still contains it.

## Candidate invariants
1. A subsequent `reserve` must not issue a third grant when a non-tail durable reservation has vanished. A healthy tail and head alone are insufficient proof of the full journal.
2. `acknowledge` must not mark a later reservation delivered when an earlier durable reservation is missing, even if its saved receipt matches.
3. Both operations must fail closed before mutating the durable journal further. This is a characterization request, not a confirmed implementation contract.

## Test vectors
| ID | Setup | Action | Expected |
| --- | --- | --- | --- |
| SC-JOURNAL-WRITE-01 | reserve seq 1 and 2; delete 1.grant; head and 2.grant intact | reserve third grant | IllegalStateException; no 3.grant |
| SC-JOURNAL-WRITE-02 | reserve and acknowledge seq 1; reserve seq 2 for same player; delete 1.grant; head and 2.grant intact | acknowledge seq 2 with receipt 2 | IllegalStateException; 2.grant remains undelivered |

## Gate and verification state
Draft only; not Confirmed. Formal CAT process-closure, TestModel generation, model-conformance, vectors and implementation-conformance are not run. These JUnit cases are expected Red against the pinned current code; no production change is authorized solely by this Draft. A successful assertion would require exact-head test evidence.
