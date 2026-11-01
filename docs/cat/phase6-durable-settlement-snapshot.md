# Phase 6: restart-safe settlement snapshot generations (CAT Draft)

Status: **Draft**, not Confirmed. Leaf `TASK-20261101-005`.

## Process / PI

A complete `SettlementAccumulator.Result` can be persisted as one versioned, checksummed generation. Publication is visible to in-process readers only after the writer atomically replaces and fsyncs the complete snapshot and verifies its readback. On restart, a new publisher must recover the last committed generation before accepting a successor. Missing data means generation zero only if no snapshot file exists; corrupt, truncated, unknown-version, or semantically inconsistent files must fail closed. A writer rejects stale/nonsequential generations and protects a single-host write sequence with a file lock.

Serialize all personal balances, Free/Dead totals, and positive dead-asset locations; recompute statistics on load rather than trusting serialized derived statistics. Verify that the sum of dead locations equals the dead total, reject duplicate UUIDs, negative values, excess sizes, and unknown dead reasons. The result is a durable *completed input set*, not proof that Minecraft's loaded/unloaded world sources were frozen or complete.

For the first generation, write and durably confirm the initialization marker before attempting the snapshot replacement. If the snapshot write then fails, the marker-without-snapshot state must fail closed rather than allow a later generation-zero reset. This store does not itself provide a distributed lock, authoritative source closure, Paper join barrier, external owner evidence, or a world-consistent snapshot. An IOException after a write may be ambiguous: the in-process publisher stays poisoned until a fresh instance reloads the durable record.

## TCE / Test model

- SETTLEMENT-STORE-01 (Red): a committed generation survives store/publisher restart with exact balances, dead locations and recomputed statistics.
- SETTLEMENT-STORE-02: generation 1→2 succeeds; a stale or skipped generation cannot overwrite the durable head.
- SETTLEMENT-STORE-03: corrupt/truncated or unknown-version bytes cause checked IOException and block further publication; no fallback to empty generation.
- SETTLEMENT-STORE-04: incomplete/failed staging cannot write a snapshot.
- SETTLEMENT-STORE-05: symlinked ancestor/root and non-regular snapshot paths fail closed.
- SETTLEMENT-STORE-06: a snapshot with invalid totals or duplicated identity cannot be accepted as a committed candidate.
- SETTLEMENT-STORE-07: concurrent writers using the same store contract cannot both commit the same generation.
- SETTLEMENT-STORE-08: an occupied local writer lock rejects promptly rather than waiting indefinitely.
- SETTLEMENT-STORE-09: after durable publication or verified restart recovery, memory-only publication is forbidden; new generations must pass the durable writer.
- SETTLEMENT-STORE-10: a durable initialization marker prevents a deleted snapshot from being misread as a fresh generation-zero store.
- SETTLEMENT-STORE-11 (Red): initialization marker is durable before the first snapshot write; a failed initial snapshot replacement cannot silently reset to generation zero after the obstructing file is removed.
- SETTLEMENT-STORE-12 (Red): a present snapshot with its required initialization marker missing must fail closed on load and successor persist.
- SETTLEMENT-STORE-13 (Red): if a durable writer commits and then throws an Error, the publisher must rethrow that Error and poison further in-process publication until a fresh durable recovery; the failure must never be treated as a successful acknowledgement.

Formal CAT machine Gates, real-world source consistency, and Paper fault-injection integration: **not-run**. Do not promote this Draft to Confirmed without an authorized decision.
