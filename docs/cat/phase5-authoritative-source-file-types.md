# Phase 5: authoritative saved-source file type integrity (CAT Draft)

Status: **Draft** (not Confirmed). Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

WorldStorageDiscovery enumerates only regular, authoritative playerdata and Anvil region/entity files. A directory or other non-regular filesystem object at a path shaped like a mandatory source (for example `world/region/r.0.0.mca` or `world/playerdata/<canonical UUID>.dat`) is an invalid snapshot input, not proof that the source contains zero assets.

During bounded, non-symlink discovery, reject a non-regular entry at an authoritative source path before constructing a manifest. Unrelated directories (including backup directories) must not be classified as authoritative data. An empty world or a missing authoritative source directory is not addressed by this change; completeness of the expected source set and loaded-vs-disk reconciliation remain independent release gates.

## TCE / acceptance vectors

- SC-SOURCE-TYPE-01 (Red): a directory at `world/region/r.0.0.mca` must throw `IOException` rather than yield a zero-region manifest.
- SC-SOURCE-TYPE-02 (Red): a directory at `world/playerdata/<UUID>.dat` must throw `IOException` rather than yield a zero-player manifest.
- SC-SOURCE-TYPE-03 (Green): an unrelated backup directory named `r.0.0.mca` under `world/backups` does not become a mandatory source.
- SC-SOURCE-TYPE-04 (Regression): regular canonical Anvil/playerdata paths are still discovered and the global visit budget is still enforced.
- SC-SOURCE-TYPE-05: the modern `dimensions/minecraft/the_nether/entities/r.0.0.mca` path must also reject a directory.
- SC-SOURCE-TYPE-06: legacy `players/data/<UUID>.dat` must reject a directory.
- SC-SOURCE-TYPE-07: a region-shaped directory in a world excluded by WorldScope remains outside the world asset scan.

- SC-SOURCE-TYPE-08 (Red): a hard link shared by an Anvil region file and a canonical playerdata file is a single physical source masquerading as two different authority categories; reject it even though each category has no internal duplicate.
- SC-SOURCE-TYPE-09 (Red): two distinct referenced external chunk `.mcc` paths that are hard links to one physical payload cannot be treated as independent authoritative inputs during snapshot traversal.
- SC-SOURCE-TYPE-10 (Regression): two genuinely distinct external sidecars remain readable and count as two independent chunks.

## Gate status

Formal CAT machine gates: **not-run**. This Draft does not authorize a settlement commit, merge, deployment, or publication. GitHub CI may validate the Java/JUnit contract independently.
