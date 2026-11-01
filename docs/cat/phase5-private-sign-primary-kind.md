# Phase 5: BlockLocker PRIVATE sign primary identity domain (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-004.

## Process / PI

A verified BlockLocker `PRIVATE` sign must have a primary identity that is either an authoritative `PERSONAL(owner UUID)` or unresolved `UNKNOWN`. The `FREE` decision is an **aggregate** result of an authenticated, attached Everyone profile with a resolved primary, or a separately proven ownerless asset; it is not a valid primary profile lookup result. A `PRIVATE` sign cannot become FREE solely because a profile lookup adapter returned FREE.

If an identity lookup yields FREE, normalize the sign primary to UNKNOWN before protection aggregation; direct construction of a `VerifiedSign` with PRIVATE/FREE is invalid. Unknown must not be promoted to FREE even when an Everyone marker appears. No asset may be published while ownership remains UNKNOWN.

## TCE / acceptance vectors

- SC-BLOCKLOCKER-PRIMARY-01 (Red): `PrimaryIdentity.resolve` returns FREE for a PRIVATE sign with a visible Everyone marker; aggregate decision remains UNKNOWN, not FREE.
- SC-BLOCKLOCKER-PRIMARY-02 (Red): direct PRIVATE `VerifiedSign` with a FREE primary is rejected at the trust boundary.
- SC-BLOCKLOCKER-PRIMARY-03 (Regression): a verified PERSONAL primary and attached Everyone still yields FREE, while a PERSONAL primary without Everyone remains PERSONAL.
- SC-BLOCKLOCKER-PRIMARY-04 (Regression): missing/unavailable lookup remains UNKNOWN or aborts, never a zero-asset or FREE result.

## Gate and integration boundary

Draft only. Formal CAT machine gates, real BlockLocker 1.14.1 runtime identity resolution, coherent saved/live world snapshot, and Paper integration are **not-run**. This local guard does not establish owner authority or permission to publish a settlement.
