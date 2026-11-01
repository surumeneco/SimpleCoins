# Phase 6: live issuance journal tail integrity (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: `TASK-20261101-005`.

## Process / PI

A live `GrantLedger` must not accept or expose a new grant while its latest committed `<sequence>.grant` record has disappeared or no longer matches the in-memory grant. The durable `head` alone proves only the sequence number, not the continued presence and content of the last record.

Before a public ledger operation, re-read and verify the current tail record with the existing bounded, checksummed `DurableFile` contract. Reject missing, corrupted, or mismatched tail bytes and latch the ledger unhealthy. Do not recreate the missing record, infer a successful receipt, or silently continue from the in-memory copy.

## TCE / acceptance vectors

- SC-JOURNAL-TAIL-01: after a successful reserve, delete its `.grant` file without changing `head`; `pending` must fail closed.
- SC-JOURNAL-TAIL-02: corrupt the last `.grant` bytes while retaining `head`; the next operation must fail closed.
- SC-JOURNAL-TAIL-03: intact journal still supports normal pending and acknowledgement.
- SC-JOURNAL-TAIL-04: the guard is bounded to the latest record; full historical record revalidation and authoritative crash recovery are separate requirements.

## Boundary

This is a narrow live integrity check, not proof of a coherent world snapshot, complete historical journal audit, or crash-safe settlement publication. Formal CAT machine Gates and Paper live failure injection are not-run until actually executed.
