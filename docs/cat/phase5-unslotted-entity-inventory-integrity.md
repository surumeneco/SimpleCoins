# Phase 5: saved Entity unslotted inventory identity (CAT Draft)

Status: **Draft** (not Confirmed). Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

The saved Entity inventory scanner accepts the unslotted `Inventory` representation only for vanilla `minecraft:villager` (eight physical positions) and `minecraft:allay` (one physical position). Vanilla container vehicles use slotted `Items`; their fabricated unslotted `Inventory` must not become an additional countable asset source. An unsupported unslotted representation aborts the read-only extraction with `IOException` before any value can be published. This does not determine ownership, loaded/disk identity, or snapshot consistency.

## TCE / acceptance vectors

- SC-ENTITY-UNSLOTTED-01 (Red): `minecraft:chest_minecart` with `Inventory` containing a coin rejects instead of returning a balance.
- SC-ENTITY-UNSLOTTED-02 (Red): `minecraft:item` with an extra `Inventory` rejects instead of exposing a second physical source.
- SC-ENTITY-UNSLOTTED-03 (Regression): villager `Inventory` still counts one valid coin stack at position 0.
- SC-ENTITY-UNSLOTTED-04 (Regression): allay `Inventory` still counts one valid coin stack at position 0.
- SC-ENTITY-UNSLOTTED-05 (Regression): chest minecart `Items` continues to count a valid slot.

## Gate status

Formal CAT machine gates: **not-run**. This Draft is not Confirmed. No settlement publication, merge, or deployment is authorized by these vectors.
