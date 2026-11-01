# Phase 5: Brewing-stand BlockEntity physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B BlockEntity asset accounting and 05_1.0.0 Phase 5/G0 fail-closed validation.

## Process / PI

A vanilla brewing stand exposes five physical slots: bottles 0–2, ingredient 3, fuel 4. Saved `minecraft:brewing_stand` `Items` entries in slot 5 or higher cannot represent additional currency and must cause read-only settlement extraction to fail closed. Slot 4 remains a valid physical position. Ownership and snapshot completeness are separate contracts.

## Test conditions

- BS-01: brewing stand currency in slot 5 raises `IOException`.
- BS-02: slot 4 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
