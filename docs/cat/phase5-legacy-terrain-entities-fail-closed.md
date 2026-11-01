# Phase 5: legacy terrain-embedded Entity assets (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: `TASK-20261101-003`.

## Process / PI

A modern Paper world stores saved Entities in the dedicated `entities/*.mca` source, which is scanned and checked for UUID uniqueness. Older Anvil terrain chunks may instead contain an `Entities` list at the root or under `Level`. The current terrain visitor does not establish a complete, independently validated owner/asset path for those embedded Entities.

When a terrain (`Kind.REGION`) chunk contains a nonempty `Entities` list in either representation, reject the entire staged traversal before passing the chunk to a settlement visitor. A present but non-list `Entities` value is also invalid. Empty lists are permitted; dedicated entity-region chunks retain their existing UUID validation. Do not silently treat unsupported legacy Entity assets as zero, or publish a partial settlement. Full legacy entity extraction is a separate integration requirement.

## TCE / acceptance vectors

- SC-LEGACY-ENT-01: root-level terrain `Entities` with one compound -> `IOException`, zero visitor callbacks.
- SC-LEGACY-ENT-02: `Level.Entities` with one compound -> `IOException`, zero visitor callbacks.
- SC-LEGACY-ENT-03: malformed non-list `Entities` -> `IOException`, zero visitor callbacks.
- SC-LEGACY-ENT-04: both terrain lists empty -> ordinary successful traversal.
- SC-LEGACY-ENT-05: a dedicated entity-region file remains subject to `SavedEntityUuidIntegrity` and is not classified as terrain.

## Gate boundary

This is a defensive fail-closed guard, not a complete legacy asset reader, authoritative Paper root inventory, loaded/disk snapshot reconciliation, or permission to publish economic totals. Formal CAT machine Gates remain not-run until executed against the canonical routing and confirmed artifacts.
