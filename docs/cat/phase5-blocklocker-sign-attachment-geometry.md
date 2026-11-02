---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.sign-attachment-geometry
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: BlockLocker 1.14.1 sign attachment geometry (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | Verified saved/live BlockLocker evidence boundary |
| simplecoins.settlement | No ownership publication from partial evidence |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| geometry.wall-north | wall sign faces north | Support block is south-adjacent, not north-adjacent | BL-GEO-01 |
| geometry.wall-other | wall sign faces east, south or west | Resolve support in opposite cardinal direction | BL-GEO-02 |
| geometry.standing | standing sign has verified physical state | Support block must be directly below | BL-GEO-03 |
| geometry.invalid | container nonadjacent or coordinate overflow | Do not attach; overflow rejects rather than wrapping | BL-GEO-04 |

Status: Draft, not Confirmed. Sources: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md; BlockLocker v1.14.1 BlockFinder.java.

## Process / PI

Given a **verified physical block state** for a supported wall or standing sign, determine only whether that sign is supported by a particular physical container block. BlockLocker v1.14.1 uses the block behind a wall sign (opposite the wall sign's facing), or the block directly below a standing sign. It searches signs on the north/east/south/west/up faces of a protected block. This is geometry only: it must not parse a header, assign an owner, validate a chest half, or claim a coherent world snapshot.

The mount enum accepts standing signs and the four cardinal wall-sign facing directions; hanging signs are not treated as supported wall/standing signs by the referenced BlockFinder. Arithmetic overflow is invalid geometry and fails closed. A caller must obtain mount and facing from the authoritative block-state palette or live Paper state, not infer them from saved sign PDC.

## Test model / TCE

- BL-GEO-01 Red: wall sign facing north is supported by the south-adjacent block, not the north-adjacent block.
- BL-GEO-02: east/south/west wall sign directions resolve the opposite adjacent support.
- BL-GEO-03: standing sign is supported only by the block immediately below.
- BL-GEO-04: nonadjacent containers are never attached, and integer-coordinate overflow fails closed.

Double-chest linkage, actual block type, sign header/profile text, loaded/disk reconciliation and ownership remain independent required checks. Formal CAT machine gates not-run; Draft is not Confirmed.
