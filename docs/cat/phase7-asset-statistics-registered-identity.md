---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.admin.asset-statistics-registered-identity
status: draft
process: simplecoins.admin
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7 — registered-player UUID integrity (CAT Draft / Red)

The `AssetStatistics.calculate(Map<UUID, BigInteger>, free, dead)` input represents **registered personal owners**. A null UUID is not a registered player and must not be converted into a personal account or counted as part of the registered population. Verified free assets use the separate `free` argument; unresolved ownership must remain outside a published complete snapshot.

| ID | Input | Required result |
| --- | --- | --- |
| STATS-IDENTITY-01 (Red) | Mutable map containing `null -> 100`, free/dead 0 | Reject the malformed registered identity before any report publication; do not report one registered player and 100 personal assets |
| STATS-IDENTITY-02 | Valid UUID -> 100, free/dead 0 | Preserve the existing valid personal asset calculation |

## Reproduced current behavior

The exact `develop` `AssetStatistics.java` blob `0f011f2e1e019cf85711eaccd48b95f86151a71c` was compiled under Java 21. The null-key probe returned `registeredPlayers=1, personal=100, world=100` rather than rejecting the identity. This is an **observed Red precondition**, not a confirmed design change or a Green implementation.

This Draft does not authorize an implementation change until the required CAT Process/PI/TCE Gate. A future fix should validate registered keys without changing the established separate free/dead accounting or accepting UNKNOWN ownership as FREE. Live Paper, source completeness and admin permission acceptance remain not-run.
