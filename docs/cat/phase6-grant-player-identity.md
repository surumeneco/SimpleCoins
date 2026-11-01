# Phase 6: issuance journal player identity queries (CAT Draft)

Status: **Draft** (not Confirmed). Leaf TASK-20261101-005.

## Process / PI

A journal lookup requires a concrete player UUID. A null player identity must not be interpreted as an empty pending queue or a valid zero receipt. Otherwise a caller with missing identity evidence could silently skip pending grants during recovery. Reject null identities before reading pending grants or validating saved receipts. Valid UUID lookups and the existing crash replay/acknowledgement behavior remain unchanged.

## TCE

- SC-GRANT-IDENTITY-01 (Red): `pending(null)` rejects the request rather than returning an empty list.
- SC-GRANT-IDENTITY-02 (Red): `validateReceipt(null, 0)` rejects the request rather than accepting an empty receipt.
- SC-GRANT-IDENTITY-03 (Regression): valid recipient still has a pending reservation after restart and can acknowledge it once.

Formal CAT machine Gates not-run; Draft not Confirmed. No release or merge.

## Journal grant identity binding (Draft vector)

- SC-GRANT-IDENTITY-04: `wasApplied` must match the supplied grant's sequence, grant UUID and recipient UUID against the current durable journal record before using a playerdata receipt. A foreign or forged grant with a coincidentally matching sequence/recipient must not be treated as already applied, even if its receipt is numerically sufficient.
