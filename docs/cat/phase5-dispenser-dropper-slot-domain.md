# Phase 5: Dispenser/dropper physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B BlockEntity asset accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

Vanilla dispensers and droppers each expose nine physical inventory slots, numbered 0–8. A saved `minecraft:dispenser` or `minecraft:dropper` BlockEntity `Items` entry in slot 9 or higher must abort read-only extraction rather than add fictitious currency. Slot 8 remains countable. This is a decoder-level safety check, not an ownership or snapshot guarantee.

## Test conditions

- DD-01: dispenser currency in slot 9 raises `IOException`.
- DD-02: dropper currency in slot 9 raises `IOException`.
- DD-03: dispenser currency in slot 8 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
