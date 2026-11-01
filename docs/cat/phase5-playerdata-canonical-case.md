# Phase 5: Canonical playerdata filename identity (CAT Draft)

Status: Draft (not Confirmed). Sources: SimpleCoins 02B and 05_1.0.0_実装計画.md (G0, Phase 5).

## Process
An authoritative saved playerdata file must be named with the exact lowercase canonical UUID plus `.dat`. The low-level playerdata reader and discovery/traversal must agree: accepting uppercase aliases in one layer could allow ambiguous sources to be treated differently when called independently. Reject case aliases before decoding, never normalize and silently accept them.

## Test model
- Red: a valid GZIP NBT playerdata file renamed to `AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA.dat` must throw IOException.
- Existing Green: lowercase canonical UUID filenames remain readable; symlinks and invalid UUIDs remain rejected.

Formal CAT machine gates: not-run; dedicated Chat plugin/Skill unavailable. This is not a Confirmed artifact.
