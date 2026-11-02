---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.double-chest-linkage
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: verified double-chest linkage geometry (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | Verified saved/live BlockLocker evidence boundary |
| simplecoins.settlement | No ownership publication from partial evidence |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| chest.reciprocal | two verified chest halves cross a chunk boundary | Require reciprocal LEFT/RIGHT, same facing and kind, and exact adjacent coordinates | BL-CHEST-01, BL-CHEST-02 |
| chest.invalid | facing, kind, half or distance mismatch | Reject as unlinked rather than infer economic ownership | BL-CHEST-03 |
| chest.single-overflow | SINGLE half or coordinate arithmetic overflow | Never form a pair; reject overflow with IOException | BL-CHEST-04 |
| chest.sign-attachment | validated pair has a wall or standing sign near either half | Evaluate physical attachment to both halves, including cross-chunk adjacency | BL-CHEST-05, BL-CHEST-06 |
| chest.nonreciprocal | sign attachment requested for nonreciprocal pair | Fail closed before interpreting sign or owner | BL-CHEST-07 |

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
