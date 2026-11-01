# Phase 5: saved item-frame representation integrity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

The existing `StoredItemFrameAssets` extractor already reads optional single `Item` stacks from saved `minecraft:item_frame` and `minecraft:glow_item_frame` Entities. `StoredEntityInventoryAssets` independently reads `Items` and `Inventory` lists. A frame cannot legitimately present both a single displayed item and a second slotted/unslotted inventory.

For both normal and glow frames, reject `Items` or `Inventory` regardless of whether the list is empty, even if the frame itself is empty. The independent inventory extractor must also reject fabricated frame inventories. Preserve existing single-stack valuation, UUID deduplication and empty-frame handling; no ownership inference is authorized.

The result is a staged saved-asset candidate, not a complete live-world snapshot. Loaded/saved reconciliation, source closure, ownership and publication are independent gates.

## TCE / acceptance vectors

- SC-FRAME-01 (Regression): a normal saved item frame with a coin contributes one UUID-keyed location.
- SC-FRAME-02 (Regression): a glow item frame follows the same rule; an empty frame contributes zero.
- SC-FRAME-03 (Red): a frame with `Items` or `Inventory` is rejected by the frame extractor, including empty competing lists.
- SC-FRAME-04 (Red): the inventory extractor rejects fabricated slotted frame inventories.
- SC-FRAME-05 (Regression): malformed Item and duplicate UUID fail closed; dropped items and unrelated Entities remain outside the frame extractor.

## Gate status

Formal CAT machine gates **not-run**; Draft not Confirmed. Local Java/JUnit checks are independent evidence and do not authorize settlement publication, merge or deploy.
