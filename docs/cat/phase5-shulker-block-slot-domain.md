# Phase 5: Shulker BlockEntity physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B recursive container asset accounting and 05_1.0.0 Phase 5/G0 fail-closed validation.

## Process / PI

Vanilla uncolored and colored shulker boxes have 27 physical inventory slots (0–26). A saved `minecraft:shulker_box` or `minecraft:<color>_shulker_box` BlockEntity `Items` entry in slot 27 or higher must abort read-only settlement extraction instead of adding a fictitious currency location. Slot 26 remains countable. The nested shulker item decoder already enforces the corresponding item-component bound; this Work aligns the placed BlockEntity path without claiming snapshot or ownership completeness.

## Test conditions

- SB-01: uncolored shulker block with currency in slot 27 raises `IOException`.
- SB-02: colored shulker block with currency in slot 27 raises `IOException`.
- SB-03: slot 26 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
