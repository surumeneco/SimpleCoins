# Phase 6: durable settlement root directory identity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-005.

## Process / PI

A `SettlementSnapshotStore` instance is bound to the **physical directory** validated at construction, not merely a reusable path string. Deleting or moving the original root and creating another ordinary directory at the same pathname must not make the existing instance treat a previously initialized store as generation zero. This is distinct from the existing tests for a missing directory or replacement by a regular file.

At construction, record a stable file-system directory identity from `BasicFileAttributes.fileKey()`. Require the same identity before every load/persist operation. If the provider cannot supply a stable key, fail closed rather than claiming a durable accounting guarantee. This check is an in-process guard, not a defense against adversarial file-system identity reuse, a cross-process transaction, or a frozen Paper world.

## TCE / acceptance vectors

- SC-SNAPSHOT-ROOT-ID-01 (Red): after moving the original directory and creating a new empty directory at the same pathname, `load()` must throw `IOException`, not return `Optional.empty()`.
- SC-SNAPSHOT-ROOT-ID-02 (Red): `persist(generation=1)` on the old store instance must reject the substituted directory without writing a snapshot or initialization marker.
- SC-SNAPSHOT-ROOT-ID-03 (Regression): a fresh instance bound to a genuine empty directory returns no generation; a normal committed generation survives restart using a newly constructed store on the same physical directory.

## Gate and deployment

CAT Draft only; formal machine gates, real Paper lifecycle fault injection and cross-process storage tests **not-run**. The change does not authorize settlement publication.
