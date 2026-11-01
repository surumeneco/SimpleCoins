# Phase 5: Hopper-minecart physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B saved Entity inventory accounting and 05_1.0.0 Phase 5/G0 fail-closed validation.

## Process / PI

A vanilla hopper minecart exposes five physical inventory slots, numbered 0–4. A saved `Items` entry with slot 5 or higher cannot represent a real inventory position. The read-only settlement decoder must reject that saved Entity source rather than attribute fabricated currency. Valid slot 4 remains countable. This local decoder rule does not imply ownership or world-snapshot completeness.

## Test conditions

- HM-01: `minecraft:hopper_minecart` with a currency stack in slot 5 raises `IOException`.
- HM-02: the same Entity with a currency stack in slot 4 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed; require independent CI Red and Green evidence.
