# Phase 5: saved dropped-item Entity accounting (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

A saved `minecraft:item` Entity represents a physical dropped stack in the entity-region `Entities` list. Its single `Item` compound is an authoritative asset candidate, not a positional `Items` or `Inventory` list. The current extractor ignores that representation, so a world containing dropped SimpleCoins currency cannot be accounted completely.

Read the Entity's canonical four-int UUID before valuing the single stack, and use a stable `<uuid>:dropped:0` location key. Reuse `StoredItemValue.value` for currency, wallet and nested-container validation; never infer an owner. Missing/malformed `Item`, duplicate UUID, or competing `Item`/`Items`/`Inventory` representations must fail closed. A non-item Entity with an unhandled `Item` representation must not silently pass as an asset-free Entity. Ordinary non-inventory Entities without such fields remain ignored.

This extraction only stages saved values. Loaded-Entity reconciliation, dropped-item ownership, complete source inventory, source freeze, and settlement publication are independent release gates.

## TCE / acceptance vectors

- SC-DROP-01 (Red): a saved `minecraft:item` with a coin stack contributes its value exactly once at `<uuid>:dropped:0`.
- SC-DROP-02 (Red): a saved `minecraft:item` with no `Item` compound is rejected instead of treated as zero.
- SC-DROP-03 (Red): a dropped Entity with malformed `Item` or conflicting `Items`/`Inventory` fails closed.
- SC-DROP-04 (Red): a non-item Entity with a single `Item` compound is rejected pending a separately specified holder adapter; no silent omission.
- SC-DROP-05 (Regression): duplicate dropped UUIDs cannot create two asset locations.
- SC-DROP-06 (Regression): a normal non-inventory Entity without `Item` is ignored and valid chest-minecart `Items` remains countable.

## Gate status

Formal CAT machine gates **not-run**. This Draft is not an approval to publish balances, merge, or deploy. Direct Java checks and the repository's JUnit suite provide implementation evidence only.
