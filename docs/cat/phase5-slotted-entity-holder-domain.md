# Phase 5: saved slotted Entity holder domain (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

A saved Entity may expose an `Items` list. The list alone is not proof that the Entity has physical inventory slots. Before counting any currency, classify the vanilla Entity holder against its supported slot topology. An unsupported or unknown holder exposing `Items` is ambiguous saved economic evidence and must abort extraction, not silently create currency. Do not infer ownership or publish a settlement from this extractor.

Supported slotted holders for the current adapter: `minecraft:chest_minecart` (27 slots), `minecraft:hopper_minecart` (5 slots), vanilla `minecraft:*_chest_boat` (27 slots), and `minecraft:bamboo_chest_raft` (27 slots). Villagers and allays retain their separate unslotted `Inventory` contract. Unknown custom/plugin inventory formats require an independently verified adapter rather than broad acceptance.

## TCE / acceptance vectors

- SC-ENTITY-SLOT-HOLDER-01 (Red): `minecraft:cow` with an `Items` list containing a valid coin fails closed instead of counting a fictitious inventory.
- SC-ENTITY-SLOT-HOLDER-02 (Red): an unknown namespaced custom Entity with an `Items` list fails closed.
- SC-ENTITY-SLOT-HOLDER-03 (Regression): chest minecart, hopper minecart, chest boat and bamboo chest raft retain supported slots and existing capacity guards.
- SC-ENTITY-SLOT-HOLDER-04 (Regression): villager/allay unslotted inventories remain countable; a holder without any inventory tag remains outside this extractor.

## Gate and publication status

This document is a CAT **Draft**, not an approved/Confirmed specification. Formal CAT machine gates and live Paper tests are **not-run**. This isolated fail-closed check does not resolve loaded/saved snapshot reconciliation, ownership or authoritative settlement publication.
