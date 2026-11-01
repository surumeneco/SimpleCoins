# Phase 6: issuance journal root directory identity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-005.

## Process / PI

An in-memory `GrantLedger` must remain bound to the same physical durable journal directory validated during construction. A directory moved away and replaced by another ordinary directory at the same path (even one containing a copied, superficially valid `head` file) is **not** the original issuance journal. Continuing `pending`, receipt validation or new reservations against stale in-memory grants risks duplicate issuance or lost acknowledgements.

Record the original directory's stable file-system identity and compare it in every health check. If no stable identity can be obtained, reject the durable ledger. A detected mismatch marks the instance unhealthy until a fresh instance replays a verified journal. This is a best-effort single-host identity guard, not an atomic cross-process transaction or protection against file-key reuse.

## TCE / acceptance vectors

- SC-GRANT-ROOT-ID-01 (Red): after moving the journal root, recreating the same path and copying a valid `head` file, `pending` and `reserve` must reject the old instance rather than operating on stale in-memory state.
- SC-GRANT-ROOT-ID-02 (Regression): a newly constructed ledger on the unchanged original directory replays pending grants normally; healthy reservations and acknowledgements remain supported.
- SC-GRANT-ROOT-ID-03 (Regression): a missing, non-directory or symbolic journal root remains fail-closed.

## Gate and integration boundary

CAT Draft only. Formal CAT machine gates and Paper crash/restart fault injection **not-run**. A verified physical root is necessary but not sufficient for end-to-end exactly-once offline grants.
