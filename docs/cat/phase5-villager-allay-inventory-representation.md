# CAT Draft — Phase 5: canonical inventory representation for villager and allay

Status: **Draft, not Confirmed**. Source: `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md` and `05_1.0.0_実装計画.md`. Work kind: Phase 5 fail-closed storage-adapter integrity.

## Contract

Minecraft Java saved `minecraft:villager` and `minecraft:allay` inventory-bearing Entities expose their positional `Inventory` list without per-stack `Slot` tags. The scanner must not interpret an `Items` list on these known unslotted types as additional, independent physical currency slots. An unexpected slotted representation fails closed before a settlement can publish totals. Valid unslotted villager (at most eight entries) and allay (at most one entry) inventories remain countable.

## Exploratory vectors

- V1: Villager with only a syntactically valid `Items` list containing a coin and `Slot=0` is rejected.
- V2: Allay with only a syntactically valid `Items` list containing a coin and `Slot=0` is rejected.
- V3: Valid `Inventory` list for each type remains accepted and valued exactly once.
- V4: Ambiguous `Items` plus `Inventory` remains rejected (existing regression).

This Draft does not claim canonical CAT process closure, generated TestModel/Queue, model conformance or normative Red/Green. Actual Paper 26.2 NBT samples and integration remain separate release gates.
