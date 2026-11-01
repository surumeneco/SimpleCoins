# Phase 5: saved item-frame assets (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

Vanilla `minecraft:item_frame` and `minecraft:glow_item_frame` Entities can hold a physical item in the optional single `Item` compound. They are distinct from dropped-item Entities (`minecraft:item`) and from slotted `Items` inventories. Saved-frame currency is a candidate asset and must not be silently omitted from a complete settlement.

Provide a read-only extractor that stages an item at a stable Entity UUID key. Empty frames (no `Item` key) contribute zero. A nonempty frame must have a canonical four-int UUID and a well-formed `Item` compound; `Items` or `Inventory` on a frame is ambiguous and rejected. Duplicate frame UUIDs fail closed. Reuse `StoredItemValue` to value nested stacks and wallet/coin metadata. No owner is inferred. The extractor is not wired to a settlement publisher until a consistent loaded/saved snapshot and ownership resolution are proven.

## TCE / acceptance vectors

- SC-FRAME-01: a normal saved item frame with a coin contributes one UUID-keyed location.
- SC-FRAME-02: a glow item frame follows the same rule.
- SC-FRAME-03: an empty frame contributes zero without inventing a stack.
- SC-FRAME-04: malformed Item, duplicate UUID, or conflicting Items/Inventory fails closed.
- SC-FRAME-05: dropped items and unrelated Entities are not counted by this extractor.

## Gate status

Formal CAT machine gates **not-run**; Draft not Confirmed. The output is a staged saved-asset candidate, not authorization to publish settlement statistics or merge/deploy.
