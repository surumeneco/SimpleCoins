# Phase 5: Barrel BlockEntity physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B BlockEntity asset accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

A vanilla barrel has 27 physical inventory slots (0–26). A saved `minecraft:barrel` BlockEntity `Items` entry in slot 27 or higher must abort read-only settlement extraction, not count fabricated currency. Slot 26 remains countable. The check is local to saved storage shape; it does not establish ownership or snapshot coherence.

## Test conditions

- BB-01: barrel currency in slot 27 raises `IOException`.
- BB-02: barrel currency in slot 26 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
