# Phase 5: Furnace-family BlockEntity physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B BlockEntity asset accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

Vanilla furnaces, blast furnaces and smokers expose three physical inventory slots (input 0, fuel 1, output 2). Saved `Items` entries with slot 3 or higher must abort read-only settlement extraction instead of creating fictitious currency locations. Slot 2 remains countable. This decoder check is independent of world snapshot and owner resolution.

## Test conditions

- FB-01: `minecraft:furnace` slot 3 raises `IOException`.
- FB-02: `minecraft:blast_furnace` slot 3 raises `IOException`.
- FB-03: `minecraft:smoker` slot 3 raises `IOException`.
- FB-04: `minecraft:furnace` slot 2 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
