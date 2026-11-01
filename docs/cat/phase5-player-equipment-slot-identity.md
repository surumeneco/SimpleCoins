# Phase 5: Saved player equipment slot identity (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B saved player inventory accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

A saved vanilla player has armor slots head/chest/legs/feet and hand slots mainhand/offhand; the `body` and `saddle` equipment slots belong to other Entity types and cannot represent independent player currency locations. The playerdata decoder must reject a saved player `equipment.body` or `equipment.saddle` entry rather than count fictitious currency. Existing valid armor and hand behavior remains unchanged. This is a local saved-player identity rule, not a live snapshot guarantee.

## Test conditions

- PE-01: playerdata with `equipment.body` containing currency raises `IOException`.
- PE-02: playerdata with `equipment.saddle` containing currency raises `IOException`.
- PE-03: valid `equipment.head` containing currency remains countable.

Formal CAT machine Gates: **not-run**. Draft is not Confirmed.
