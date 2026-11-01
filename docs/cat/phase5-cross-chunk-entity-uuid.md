# Phase 5: Cross-chunk saved Entity UUID uniqueness (CAT Draft)

Status: Draft, not Confirmed. Sources: SimpleCoins 02B, 05_1.0.0_実装計画.md (A02/A03, G0).

## Process
The UUID of a saved Entity is a global physical identity, not a per-chunk or per-world identity. During one stable world snapshot traversal, the same Entity UUID appearing in two different entity chunks/files must invalidate the entire traversal before any successful report or settlement publication. A per-chunk identity check alone is insufficient when the same Entity is copied into two separate chunks. Terrain chunks do not contribute to the saved Entity UUID set.

## Test model
- Red: two distinct entity region files with different NBT byte streams but the same saved Entity UUID must fail the traversal, even when their visitor callbacks would otherwise succeed.
- Green: distinct UUIDs across entity chunks remain valid; an empty Entities list is valid.
- Existing guarantees: physical file aliases and malformed Entity UUIDs remain fail-closed.

Formal CAT machine gates: not-run; dedicated Chat plugin/Skill unavailable. This Draft is not a Confirmed artifact.
