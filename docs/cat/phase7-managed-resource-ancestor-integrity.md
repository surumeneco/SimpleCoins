# Phase 7: managed Geyser pack root ancestry (CAT Draft)

Status: **Draft**, not Confirmed. Leaf TASK-20261101-006.

## Process / PI

SimpleCoins may write or remove only its explicitly managed Geyser resource-pack and mapping files. Before creating a managed root or changing any resource, reject symbolic links at every existing component of the root and target ancestry, not merely the final file or immediate parent. A redirected ancestor must never cause a pack, mapping, or manifest to be written outside the intended real data folder. Recheck after creating directories. This is a best-effort local path integrity check, not an atomic protection against malicious concurrent filesystem replacement.

## TCE / vectors

- GEYSER-MANAGED-ANCESTOR-01 (Red): an ancestor symlink above the managed root rejects replacement before any file is written under the symlink destination.
- GEYSER-MANAGED-ANCESTOR-02: a normal real nested root still installs, updates and removes only managed outputs.
- GEYSER-MANAGED-ANCESTOR-03: a symlinked managed output or immediate parent remains rejected; unrelated files are untouched.

No Paper/Geyser/BE live verification, CAT machine Gate, merge or deployment is authorized. This Draft is not Confirmed.
