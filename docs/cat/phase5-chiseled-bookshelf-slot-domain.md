# Phase 5: Chiseled bookshelf physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B BlockEntity asset accounting and 05_1.0.0 Phase 5/G0 fail-closed validation.

## Process / PI

A vanilla chiseled bookshelf has six physical slots (0–5). A saved `minecraft:chiseled_bookshelf` BlockEntity `Items` entry in slot 6 or higher must be rejected as an impossible currency location by the read-only settlement decoder. Slot 5 is a valid physical position. This checks slot identity only; vanilla insertion restrictions and authoritative ownership remain separate contracts.

## Test conditions

- CBK-01: slot 6 raises `IOException`.
- CBK-02: slot 5 remains countable at the storage decoder boundary.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
