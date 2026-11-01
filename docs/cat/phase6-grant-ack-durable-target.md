# Phase 6: verify targeted grant before acknowledgement (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: `TASK-20261101-005`.

## Process / PI

Acknowledging a previously issued grant replaces its `<sequence>.grant` file with the delivered form. A live ledger may have more recent grants, so a tail-only health check does not prove that an older targeted record still exists and matches the in-memory reservation.

Before acknowledging a grant, re-read the targeted durable record using the bounded checksummed journal reader and compare its full immutable grant fields with the in-memory state. Missing, corrupted, or mismatched evidence must latch the ledger unhealthy and prevent replacement. A valid pending grant may still be acknowledged once its saved receipt is verified.

## TCE / acceptance vectors

- SC-JOURNAL-ACK-01: with two reservations, delete the older record; acknowledging it must fail without recreating the file.
- SC-JOURNAL-ACK-02: corrupt the older record; acknowledgement must fail without overwriting it.
- SC-JOURNAL-ACK-03: intact older reservation with a matching receipt can be acknowledged.

This is a targeted durable write precondition, not a full historical journal audit or proof of playerdata save durability.
