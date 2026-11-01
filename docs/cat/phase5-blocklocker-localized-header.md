# Phase 5: BlockLocker localized sign header matching (CAT Draft)

Status: Draft, not Confirmed. Sources: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md; BlockLocker v1.14.1 SignParserImpl.getSignTypeOrNull.

## Process / PI

After an authoritative adapter has decoded the displayed sign text into a plain first line, match the line against **caller-supplied BlockLocker localized simple header aliases**. Follow BlockLocker 1.14.1's color-code stripping, trim and case-insensitive equality. Never hard-code English aliases as a complete localization contract. Distinguish PRIVATE and MORE_USERS; overlapping alias sets are ambiguous and fail closed. A nonmatching header yields no candidate, not proof that the attached container is unprotected.

This helper does not parse raw JSON text components, infer a sign's support block, or assign an owner. The caller must supply the exact runtime BlockLocker alias configuration and verify the physical sign block and protection topology.

## Test model / TCE

- BL-HEADER-01 Red: exact and color-formatted localized PRIVATE aliases match after stripping legacy color codes and trimming.
- BL-HEADER-02: MORE_USERS aliases match independently and case-insensitively.
- BL-HEADER-03: an unknown header yields Optional.empty without owner classification.
- BL-HEADER-04: empty/malformed alias sets and aliases shared by both sign types fail closed.

Formal CAT machine gates not-run; Draft is not Confirmed.
