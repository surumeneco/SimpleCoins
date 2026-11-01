# Phase 5: legacy terrain TileEntities cannot disappear (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: `TASK-20261101-003`.

## Process / PI

The current saved block-inventory adapter reads modern terrain `block_entities`. Legacy terrain chunks may contain block inventories in `Level.TileEntities` (or a root-level `TileEntities` tag), which are not accounted by that adapter. A traversal that silently ignores a nonempty legacy list can produce an incomplete economic total.

Before handing a `Kind.REGION` chunk to any staging visitor, reject nonempty or malformed legacy `TileEntities` lists at the root and inside `Level`. Empty lists are allowed. Modern `block_entities` are unaffected. This guard makes unsupported legacy storage an explicit fail-closed condition; a complete migration/legacy adapter is still required to count such assets.

## TCE / acceptance vectors

- SC-LEGACY-BLOCK-01: root `TileEntities` containing a block inventory -> IOException, no visitor callbacks.
- SC-LEGACY-BLOCK-02: `Level.TileEntities` containing a block inventory -> IOException, no visitor callbacks.
- SC-LEGACY-BLOCK-03: malformed `TileEntities` non-list -> IOException, no visitor callbacks.
- SC-LEGACY-BLOCK-04: empty lists -> ordinary traversal.
- SC-LEGACY-BLOCK-05: modern `block_entities` remain handled by the existing adapter, not this guard.

## Boundary

This does not implement a legacy block-entity decoder, Paper live/disk reconciliation, source inventory authority, or settlement publication. Formal CAT machine Gates are not-run unless explicitly executed against canonical artifacts.
