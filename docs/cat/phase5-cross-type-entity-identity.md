# Phase 5: Saved Entity UUID integrity across adapters (CAT Draft)

Status: Draft (not Confirmed). Source: SimpleCoins 02B and 05_1.0.0_実装計画.md, A02/A03 and G0.

## Process / invariant
A stable saved Entity chunk must not contain two records with the same Entity UUID, even if one record exposes `equipment` and the other exposes `Items`. Before an owner-resolved settlement stages any asset, validate the identity of **all** saved Entities, not just those recognized by an individual inventory/equipment adapter. Duplicate or missing UUID means an unreadable source and prevents commit.

## Test model
- Red: duplicate UUID across equipped wolf and inventory chest boat, both carrying a coin, must raise IOException; settlement.commit must reject the source.
- Regression: two different UUIDs across those adapters must still produce the correct personal and free totals.
- Failure is not a valid zero-balance source and must not be silently ignored.

## Verification
Formal CAT machine gates: not-run (Chat plugin/Skill not exposed). Draft is not Confirmed.
