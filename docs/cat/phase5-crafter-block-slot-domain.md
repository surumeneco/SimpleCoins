# Phase 5: Crafter BlockEntity physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B BlockEntity asset accounting and 05_1.0.0 Phase 5/G0 fail-closed validation.

## Process / PI

A vanilla crafter has nine physical inventory slots (0–8). A saved `minecraft:crafter` BlockEntity `Items` entry in slot 9 or higher must abort read-only settlement extraction rather than count fictitious currency. Slot 8 remains countable. This is a local decoder check and does not establish full settlement correctness.

## Test conditions

- CR-01: crafter currency in slot 9 raises `IOException`.
- CR-02: slot 8 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
