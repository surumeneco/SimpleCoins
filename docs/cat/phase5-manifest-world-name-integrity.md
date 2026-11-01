# Phase 5: Manifest world identity integrity (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B world-scoped asset accounting and 05_1.0.0 Phase 5/G0 manifest closure.

## Process / PI

Every saved world source must have a nonblank world identity. Discovery already rejects blank world names supplied as world roots; a manually assembled `WorldStorageDiscovery.SourceFile` must enforce the same invariant so no source can be staged or attributed under an empty world key. Valid names remain accepted. This is a manifest input contract, not a consistent-snapshot or ownership guarantee.

## Test conditions

- MW-01: creating a SourceFile with an empty or whitespace-only world name throws `IllegalArgumentException`.
- MW-02: a valid nonblank world name remains accepted.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
