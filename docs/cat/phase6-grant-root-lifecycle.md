# Phase 6: issuance journal root lifecycle integrity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-005.

## Process / PI

`GrantLedger` verifies its durable root at construction, then serves in-memory `pending`, receipt and replay decisions. If the root directory is deleted or replaced after construction, those reads currently continue as if the journal remained durable. A stale in-memory issuance state must not authorize further delivery.

Before any public ledger operation, fail closed if the root no longer exists as a real directory, the durable journal head is missing or non-regular, or a path ancestor has become symbolic. Mark the ledger unhealthy so subsequent calls remain blocked until explicit restart/recovery. This is a single-host lifecycle guard; it cannot detect deletion/recreation of an identical directory or substitute for durable replay.

## TCE / acceptance vectors

- SC-GRANT-ROOT-01 (Red): after deleting the root and head file, `pending` fails closed instead of returning an empty list.
- SC-GRANT-ROOT-02 (Red): after replacing the root with a regular file, `validateReceipt` fails closed.
- SC-GRANT-ROOT-03 (Red): removing the durable head file while the root remains must block pending delivery.\n- SC-GRANT-ROOT-04 (Regression): a healthy newly constructed ledger permits an empty pending list and receipt zero.
- SC-GRANT-ROOT-05 (Regression): an unhealthy ledger remains blocked after a root is recreated; restart/replay is required.

## Gate status

Formal CAT machine Gates **not-run**; Draft not Confirmed. No live Paper delivery or crash-recovery verification is claimed.
