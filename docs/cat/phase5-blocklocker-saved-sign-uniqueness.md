# Phase 5: saved sign physical-coordinate uniqueness (CAT Draft)

Status: Draft, not Confirmed. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md (BlockLocker 1.14.1), existing saved terrain candidate reader.

## Process / PI

Input: one decoded terrain chunk with `block_entities`. Each saved `minecraft:sign` or `minecraft:hanging_sign` occupies a unique physical block coordinate, irrespective of whether it has BlockLocker PDC. A second sign entry at the same position makes the source ambiguous and must fail closed. Text-only legacy signs cannot be discarded before the uniqueness check. Preserve the existing public return contract: only recognized saved PDC hints are returned, never authoritative ownership.

## Test model / TCE

- BL-SIGN-UNIQUE-01 Red: a PDC-bearing saved sign and a text-only sign at the same position cause `IOException`, regardless of entry order.
- BL-SIGN-UNIQUE-02 Red: two text-only signs at the same position also cause `IOException`.
- BL-SIGN-UNIQUE-03 Regression: distinct text-only sign positions do not become BlockLocker owner hints.
- BL-SIGN-UNIQUE-04 Regression: two different PDC-bearing sign coordinates remain distinct candidates.

No settlement is published by this helper. Display-text precedence, actual attachment and double-chest ownership remain unresolved. Formal CAT machine gates not-run; Draft is not Confirmed.
