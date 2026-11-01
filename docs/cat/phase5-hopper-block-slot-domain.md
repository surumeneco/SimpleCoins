# Phase 5: Hopper BlockEntity physical slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B BlockEntity asset accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

A vanilla hopper block has five physical inventory slots, numbered 0–4. A saved `minecraft:hopper` BlockEntity `Items` entry with slot 5 or higher must abort the read-only settlement extraction rather than count a fictitious currency location. Slot 4 remains countable. This local check does not establish BlockLocker ownership, snapshot consistency or all-world settlement.

## Test conditions

- HB-01: `minecraft:hopper` with a currency stack in slot 5 raises `IOException`.
- HB-02: slot 4 remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
