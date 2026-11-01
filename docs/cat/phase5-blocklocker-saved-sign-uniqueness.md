---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.saved-sign-uniqueness
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: saved sign physical-coordinate uniqueness (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | Verified saved/live BlockLocker evidence boundary |
| simplecoins.settlement | No ownership publication from partial evidence |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| sign.duplicate-pdc-text | PDC-bearing and text-only signs share one physical coordinate | Reject duplicate entries regardless of input order | BL-SIGN-UNIQUE-01 |
| sign.duplicate-text | two text-only signs share one physical coordinate | Reject duplicate physical sign identity | BL-SIGN-UNIQUE-02 |
| sign.text-only | distinct text-only signs have no BlockLocker PDC | Preserve physical uniqueness check without manufacturing owner hints | BL-SIGN-UNIQUE-03 |
| sign.distinct-pdc | PDC-bearing signs occupy different positions | Preserve distinct coordinate-keyed candidate hints | BL-SIGN-UNIQUE-04 |

Status: Draft, not Confirmed. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md (BlockLocker 1.14.1), existing saved terrain candidate reader.

## Process / PI

Input: one decoded terrain chunk with `block_entities`. Each saved `minecraft:sign` or `minecraft:hanging_sign` occupies a unique physical block coordinate, irrespective of whether it has BlockLocker PDC. A second sign entry at the same position makes the source ambiguous and must fail closed. Text-only legacy signs cannot be discarded before the uniqueness check. Preserve the existing public return contract: only recognized saved PDC hints are returned, never authoritative ownership.

## Test model / TCE

- BL-SIGN-UNIQUE-01 Red: a PDC-bearing saved sign and a text-only sign at the same position cause `IOException`, regardless of entry order.
- BL-SIGN-UNIQUE-02 Red: two text-only signs at the same position also cause `IOException`.
- BL-SIGN-UNIQUE-03 Regression: distinct text-only sign positions do not become BlockLocker owner hints.
- BL-SIGN-UNIQUE-04 Regression: two different PDC-bearing sign coordinates remain distinct candidates.

No settlement is published by this helper. Display-text precedence, actual attachment and double-chest ownership remain unresolved. Formal CAT machine gates not-run; Draft is not Confirmed.
