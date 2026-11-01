# Phase 5: saved WWC follow-target evidence (CAT Draft)

Status: **Draft**, not Confirmed. Parent TASK-20261031-SIMPLECOINS-100; leaf TASK-20261101-004.

## Process / PI

The current WhatAWonderfulChicken `WonderfulChickenStore` persists a marker `wonderful_chicken`, `data_version` (2–4), and an optional canonical UUID `follow_target` through Bukkit PersistentDataContainer keys. A saved Java chicken Entity may expose those typed values under the `BukkitValues` NBT compound with the plugin's lowercase namespace `whatawonderfulchicken`. This is a read-only, **positive-only commander hint**: a valid marker/version/target yields a UUID; missing/malformed evidence is UNKNOWN, never FREE or a zero-asset assertion. It does not prove that the entity was unloaded at snapshot time, that the commander is registered/unbanned, or that the entity is absent from loaded memory.

The caller must verify the saved Entity's UUID, authoritative world snapshot, current WWC contract, ownership precedence and loaded-vs-disk reconciliation before settlement. No Paper world or plugin state may be mutated.

## TCE / vectors

- WWC-SAVED-01 (Red): a saved `minecraft:chicken` with canonical PDC marker/version/target yields known commander UUID.
- WWC-SAVED-02: absent `BukkitValues`, missing marker, missing target or unsupported version yields UNKNOWN.
- WWC-SAVED-03: wrong entity type, malformed PDC compound, wrong-typed target or noncanonical UUID yields UNKNOWN.
- WWC-SAVED-04: a valid typed v2/v3/v4 marker and target remain recognized; no inference of FREE for ordinary chickens.
- WWC-SAVED-05: no changes to source maps or Bukkit state.

The saved NBT representation requires real Paper 26.2 integration verification. Formal CAT machine Gates: **not-run**; Draft not Confirmed. No merge, deployment or settlement publication.
