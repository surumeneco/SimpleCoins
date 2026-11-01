# CAT Draft: canonical Anvil filename at the reader boundary

Status: **Draft / not Confirmed**. Sources: SimpleCoins 02B/05 G0 Phase 5 physical-source uniqueness, and the already adopted canonical-name rule in WorldStorageDiscovery/WorldSnapshotTraversal.

## Process / PI

- A read-only Anvil reader receives a region file path.
- PI-1: filename must be the canonical `r.<int32-x>.<int32-z>.mca` spelling, not merely integer-parseable.
- PI-2: leading zeros, explicit plus signs and negative-zero aliases must fail before any chunk callback, even if discovery/traversal is bypassed.
- PI-3: canonical filenames retain existing decoding behavior.

## TCE

| ID | Input | Expected |
| --- | --- | --- |
| ANV-01 | valid file renamed from `r.0.0.mca` to `r.00.0.mca` | IOException, zero callbacks |
| ANV-02 | `r.0.0.mca` | existing single-chunk success |

Formal CAT machine Gates: **not-run**; no Confirmed promotion. Test-first Red and feature-branch CI Green are required. No claim of complete snapshot or production settlement.
