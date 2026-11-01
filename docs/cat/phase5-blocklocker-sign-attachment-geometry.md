# Phase 5: BlockLocker 1.14.1 sign attachment geometry (CAT Draft)

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
