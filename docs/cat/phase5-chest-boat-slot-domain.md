# Phase 5: Chest-boat physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B physical inventory accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

Vanilla chest boats and chest rafts expose 27 physical inventory slots, numbered 0–26. A saved `Items` entry with slot 27 or higher cannot represent an additional physical currency location. The read-only settlement decoder must reject the entire source instead of counting fabricated currency. Valid slot 26 remains countable. This does not establish authoritative ownership or a coherent world snapshot.

## Test conditions

- CB-01: `minecraft:oak_chest_boat` with a currency stack in slot 27 raises `IOException`.
- CB-02: the same boat with a stack in slot 26 remains countable.
- CB-03: `minecraft:bamboo_chest_raft` has the same upper bound.

Formal CAT machine Gates: **not-run**. This Draft is not Confirmed; test Red and Green separately.
