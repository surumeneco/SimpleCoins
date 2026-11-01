---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.settlement.manifest-source-closure
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: Manifest source closure identity (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.settlement | Read-only source staging and fail-closed settlement boundary |
| simplecoins.snapshot | Verified saved-source manifest and closure identity |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| manifest.required-source-ids | authoritative region and playerdata files are declared in manifest | Return immutable canonical absolute-normalized source IDs for both categories | SC-MANIFEST-01 |
| manifest.duplicate | same canonical path occurs in different source categories | Reject ambiguous source identity instead of silently deduplicating | SC-MANIFEST-02 |
| manifest.exclusion | unrelated backup folders or physical file aliases are present | Keep existing exclusion and physical alias validation as independent guards | SC-MANIFEST-03 |

Status: Draft, not Confirmed. Sources: SimpleCoins 02B, 05_1.0.0_実装計画.md, G0.

## Process
A complete settlement must declare every authoritative region, entity and playerdata source before staging assets. The manifest shall expose deterministic canonical source IDs for all entries: absolute normalized path strings. Duplicate canonical paths across source categories are an authority conflict and must fail closed, never silently collapse into one required source. The result is an immutable set usable by SettlementAccumulator's required-source closure check.

## Test model
- Red: a manifest with one region file and one playerdata file produces two canonical required source IDs.
- Red: a duplicate path appearing in two categories is rejected instead of silently deduplicated.
- Regression: discovery still excludes backup folders and detects physical aliases; this helper does not replace inode/hardlink preflight.

Formal CAT machine gates: not-run; dedicated Chat plugin/Skill unavailable. Draft is not Confirmed.
