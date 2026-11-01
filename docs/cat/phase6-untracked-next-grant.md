# Phase 6: prevent overwriting untracked issuance records (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: `TASK-20261101-005`.

## Process / PI

The live `GrantLedger` computes the next sequence from its in-memory records. An unexpected `<next>.grant` file may have appeared after startup (for example, from an interrupted independent writer or recovery). The durable write helper replaces its target atomically, so writing without checking would destroy that untracked reservation.

Before reserving the next sequence, reject any existing directory entry at the target path using `NOFOLLOW_LINKS`. Latch the ledger unhealthy and require an authoritative restart/reconciliation rather than overwriting the record. A genuinely absent target may be created normally.

## TCE / acceptance vectors

- SC-JOURNAL-NEXT-01: after initialization, an untracked next `.grant` path appears; reservation throws and leaves the existing bytes unchanged.
- SC-JOURNAL-NEXT-02: a dangling symlink at the next record path also blocks reservation.
- SC-JOURNAL-NEXT-03: a normal absent next path permits exactly one reservation.

This does not provide multi-process transactional locking or replace a full journal replay after an ambiguous crash.
