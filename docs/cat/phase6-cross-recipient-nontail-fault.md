# Phase 6 cross-recipient durable journal integrity — code-to-spec Draft

Status: Draft / candidate characterization; not Confirmed.
Task: TASK-20261101-005.
Source: GrantLedger.java on feature/phase6-nontail-receipt-pending-queue-a-20261103 (existing in-memory TreeMap, checkHealthy verifies head and tail).
Scope: read-only pending and receipt validation when an earlier durable reservation belongs to a different player.

## Observation

The journal uses one global sequence across recipients. Its current checkHealthy verifies the head and final record, but not each older reservation. A missing or corrupted older record for player A can therefore be hidden when player B reads an intact later record. In-memory entries are not independent durability evidence.

## Candidate invariants (not approved requirements)

| Target | Rule | Evidence |
| --- | --- | --- |
| GrantLedger.pending(B) | A missing earlier global journal record for A must prevent a successful pending(B) result | Expected-Red JUnit fixture |
| GrantLedger.validateReceipt(B, 0) | A corrupted earlier global journal record for A must prevent a successful receipt decision for B | Expected-Red JUnit fixture |
| Intact journal | Two players with intact sequential records retain independent pending/receipt results | Green characterization |

## Decision and gate

Whether every read validates the entire global journal, uses a verified integrity manifest, or first marks the ledger unhealthy is a normative design decision. These tests are Draft expected-Red and must not be treated as authorization for a production fix or as a formal CAT Gate pass. Reproduce with the project runner and confirm failure reasons before promotion. Do not merge into develop before required approvals and Gates.
