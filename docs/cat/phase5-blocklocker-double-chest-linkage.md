# Phase 5: verified double-chest linkage geometry (CAT Draft)

Status: Draft, not Confirmed. Sources: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md; BlockLocker v1.14.1 BlockFinder and SeparateContainersBlockFinder.

## Process / PI

Given two **verified physical chest block states**, determine whether they are reciprocal halves of one vanilla double chest. Both must have the same facing and chest kind (normal or trapped), opposite LEFT/RIGHT half types, and adjacent positions that agree with BlockLocker's `turn90Degrees(facing)` mapping (NORTH→EAST→SOUTH→WEST→NORTH). A LEFT half's neighbor is in that rotated direction; a RIGHT half's neighbor is opposite. SINGLE is never linked. Coordinates can cross chunk boundaries. Overflow fails closed.

This is only physical geometry for later authoritative BlockLocker protection resolution. It does not parse signs, infer ownership, choose an inventory's economic owner, or prove a stable world snapshot. Callers must provide actual block states from an authoritative snapshot; PDC alone cannot establish chest topology.

## Test model / TCE

- BL-CHEST-01 Red: north-facing LEFT at x=15 and RIGHT at x=16 form a reciprocal double chest across a chunk boundary.
- BL-CHEST-02: EAST/SOUTH/WEST facing directions use the same BlockFinder rotation.
- BL-CHEST-03: mismatched facing, chest kind, half type or distance never form a pair.
- BL-CHEST-04: SINGLE is unlinked; integer-coordinate overflow aborts with IOException.

Formal CAT machine gates not-run; Draft is not Confirmed.

## Sign attachment to a validated chest pair (additional Draft vectors)

A sign may protect either half of a reciprocal double chest. Only after verifying that the two supplied halves form one pair, evaluate sign attachment against both physical halves. Invalid chest topology must abort rather than silently treat the pair as two unrelated economic owners. This remains geometry-only; a valid protection sign and its owner still require authoritative text/PDC and world-state resolution.

- BL-CHEST-05 Red: a wall sign attached to either half of a valid pair is recognized, including across a chunk boundary.
- BL-CHEST-06: a standing sign above either half is recognized; a distant sign is not.
- BL-CHEST-07: a nonreciprocal pair fails closed before any sign attachment decision.
