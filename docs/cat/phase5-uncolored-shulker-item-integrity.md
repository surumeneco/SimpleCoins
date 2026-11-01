# Phase 5: Uncolored shulker item physical integrity (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B nested item asset accounting and 05_1.0.0 Phase 5/G0 fail-closed validation.

## Process / PI

The vanilla uncolored `minecraft:shulker_box` is a shulker container just like its colored variants. A saved shulker item carrying a nonempty `minecraft:container` component must not be accepted with stack count greater than one, and a nested slot outside 0–26 must be rejected. Existing colored shulker behavior remains unchanged. These are physical saved-item invariants, not a live snapshot guarantee.

## Test conditions

- US-01: uncolored shulker count 2 with a coin child raises `IOException`.
- US-02: uncolored shulker count 1 with a coin in slot 27 raises `IOException`.
- US-03: uncolored shulker count 1 with a coin in slot 26 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
