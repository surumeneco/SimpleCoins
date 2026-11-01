# Phase 5: Saved player equipment slot identity (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B saved player inventory accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

A saved vanilla player has armor slots head/chest/legs/feet and hand slots mainhand/offhand; the `body` and `saddle` equipment slots belong to other Entity types and cannot represent independent player currency locations. The playerdata decoder must reject a saved player `equipment.body` or `equipment.saddle` entry rather than count fictitious currency. Existing valid armor and hand behavior remains unchanged. This is a local saved-player identity rule, not a live snapshot guarantee.

## Test conditions

- PE-01: playerdata with `equipment.body` containing currency raises `IOException`.
- PE-02: playerdata with `equipment.saddle` containing currency raises `IOException`.
- PE-03: valid `equipment.head` containing currency remains countable.
- PE-04: explicitly present `equipment: null` is malformed and fails closed; omitted legacy `equipment` remains valid.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.

## Missing selected-slot identity (additional Draft vector)

If modern `equipment.mainhand` and a legacy `Inventory` hotbar slot (0..8) are both present, but `SelectedItemSlot` is absent, the reader cannot determine whether they are the same physical item. Reject this ambiguous saved snapshot rather than counting the mainhand and a potentially identical hotbar stack independently. A mainhand with no legacy hotbar entry remains countable, and a valid selected slot retains the existing overlap checks. This does not establish live playerdata consistency.

- PE-05 Red: missing `SelectedItemSlot`, one saved hotbar item and one `equipment.mainhand` item must raise `IOException`.
- PE-06 regression: missing `SelectedItemSlot` with no saved hotbar item may count `equipment.mainhand`.
- Formal CAT machine Gates remain not-run; Draft not Confirmed.
