# Phase 5: Asset owner classification result invariants (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02A/02B P/F/D ownership classification and 05_1.0.0 Phase 5 fail-closed validation.

## Process / PI

An `AssetOwner.Result` is internally consistent: FREE has no player and no dead reason; PERSONAL has a player but no dead reason; DEAD has a player and a non-null `DeadReason`. The public record constructor must reject contradictory combinations rather than expose a classification that cannot be produced by the canonical `AssetOwner.resolve` path. This is a pure domain invariant, not a BAN-source integration.

## Test conditions

- AO-01: PERSONAL with CURRENT_BAN reason raises `IllegalArgumentException`.
- AO-02: DEAD without a reason raises `IllegalArgumentException`.
- AO-03: a null classification kind raises `IllegalArgumentException`.
- AO-04: valid FREE, PERSONAL and DEAD combinations remain constructible.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
