# Phase 5: Chest-minecart physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B physical inventory accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

A vanilla chest minecart has 27 physical inventory slots, numbered 0–26. A saved `Items` entry with slot 27 or higher is not a real asset location. The read-only settlement decoder must reject the entire source rather than count fabricated currency. Valid slot 26 remains countable. This is a local decoder contract, not complete world snapshot or ownership validation.

## Test conditions

- CM-01: `minecraft:chest_minecart` with a coin in slot 27 raises `IOException`.
- CM-02: a coin in slot 26 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed; require CI Red and Green.
