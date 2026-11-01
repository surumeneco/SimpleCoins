# Phase 7: Java pack publication ancestor integrity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-006.

## Process / PI

The generated Java pack is published with an atomic rename. Checking only that the temporary archive itself is a regular file is insufficient when one of its parent directories, or the destination's parent directory, is a symbolic link. The move may then publish into an unexpected directory even though the final archive path has no symlink at its last component.

Before invoking the atomic move, reject any symbolic-link component in the absolute source and destination paths. Reject parent traversal before normalization; preserve the existing atomic-only replacement contract. This protects the publication boundary but does not establish that the configured public HTTP(S) URL serves the same bytes or that a JE/BE client has received them.

## TCE / acceptance vectors

- SC-PACK-PATH-01 (Red): a regular candidate reached through a symbolic-link ancestor must not invoke the move or replace the existing pack.
- SC-PACK-PATH-02 (Red): a destination reached through a symbolic-link ancestor must not invoke the move.
- SC-PACK-PATH-03 (Regression): ordinary same-directory atomic publication remains successful.

## Gate status

Formal CAT machine gates **not-run**. Live Paper/Geyser/JE/BE pack delivery and Nexo delegation **not-run**.
