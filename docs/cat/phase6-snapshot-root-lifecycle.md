# Phase 6: durable snapshot root lifecycle integrity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-005.

## Process / PI

`SettlementSnapshotStore` validates its storage root in the constructor. If the directory is subsequently deleted or replaced by a regular file, `load()` currently sees neither `settlement.snapshot` nor the initialization marker and may return `Optional.empty()`. That can be mistaken for a never-initialized store rather than a lost durable root.

Every `load()` and `persist()` must verify that the configured root still exists as a real directory (no symbolic links), before interpreting absence of the snapshot/marker. A missing or replaced root after construction is an error, not an empty generation. This does not solve out-of-band deletion and recreation of the same directory or cross-host durability; such cases require operational storage controls.

## TCE / acceptance vectors

- SC-ROOT-01 (Red): delete a constructed store's root directory, then `load()` must throw IOException rather than return empty.
- SC-ROOT-02 (Red): replace the root with a regular file, then `load()` must throw IOException.
- SC-ROOT-03 (Regression): a newly constructed empty real directory returns Optional.empty.
- SC-ROOT-04 (Regression): a valid persisted generation remains loadable, and symbolic ancestor rejection remains intact.

## Gate status

Formal CAT machine Gates **not-run**; Draft not Confirmed. Root integrity alone does not prove coherent source capture, Paper join serialization or settlement publication.
