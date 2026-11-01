# Phase 6: source-closure readiness barrier (CAT Draft)

Status: **Draft**, not Confirmed. Leaf `TASK-20261101-005`.

## Process / PI

A settlement generation cannot be exposed to new joins before every manifest-declared authoritative source has completed staging and the **exact** completed result has been durably persisted and read back. The barrier begins closed. A factory derives the required IDs from the manifest's canonical source closure, and the admission callback is serialized with mutation notifications. A partially failed admission invalidates readiness. Its source IDs are immutable and nonempty; unknown or duplicate completions, failed sources, and mutations during staging invalidate the attempt (fail closed). A mutation observed after opening closes the barrier to subsequent joins.

The barrier coordinates readiness **only**. It does not freeze loaded chunks/entities, prevent existing players from mutating inventories, discover authoritative sources, integrate Paper join events, or prove that the world and disk were captured atomically. Those obligations belong to the caller and remain acceptance blockers until integrated and tested. The caller must notify the barrier of mutations and must prevent physical mutation between source capture and durable verification.

## TCE / Test model

- SETTLEMENT-BARRIER-01: joining is denied before staging, while staging, and before complete source closure.
- SETTLEMENT-BARRIER-02: only exact manifest source IDs can complete; duplicates and unknown IDs poison the attempt.
- SETTLEMENT-BARRIER-03: source failure or mutation during staging blocks publication and joins.
- SETTLEMENT-BARRIER-04: a completed source set opens only after exact durable snapshot readback, not an in-memory candidate or mismatched generation.
- SETTLEMENT-BARRIER-05: corrupt/missing durable state or a failed verification closes the barrier.
- SETTLEMENT-BARRIER-06: mutation observed after opening closes the gate to subsequent joins.
- SETTLEMENT-BARRIER-07: the barrier cannot be restarted/reopened after a failed attempt without constructing a new instance and reconciling authoritative state.

Formal CAT machine gates, Paper listener wiring, authoritative world freeze, and real server fault injection: **not-run**. Do not promote Draft to Confirmed without authorization.
