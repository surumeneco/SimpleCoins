---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.localized-header
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: BlockLocker localized sign header matching (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | Verified saved/live BlockLocker evidence boundary |
| simplecoins.settlement | No ownership publication from partial evidence |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| header.private | visible first line matches localized PRIVATE alias with color or whitespace | Normalize legacy formatting and match exact alias without assigning owner | BL-HEADER-01 |
| header.more-users | visible first line matches localized MORE_USERS alias | Recognize independently with case-insensitive comparison | BL-HEADER-02 |
| header.unknown | visible first line matches neither configured alias | Return no header candidate, not an unprotected verdict | BL-HEADER-03 |
| header.invalid-config | alias set missing, malformed or overlapping across types | Reject ambiguity and avoid hard-coded English fallback | BL-HEADER-04 |

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
