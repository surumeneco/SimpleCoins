# Phase 6: Manifest source closure identity (CAT Draft)

Status: Draft, not Confirmed. Sources: SimpleCoins 02B, 05_1.0.0_実装計画.md, G0.

## Process
A complete settlement must declare every authoritative region, entity and playerdata source before staging assets. The manifest shall expose deterministic canonical source IDs for all entries: absolute normalized path strings. Duplicate canonical paths across source categories are an authority conflict and must fail closed, never silently collapse into one required source. The result is an immutable set usable by SettlementAccumulator's required-source closure check.

## Test model
- Red: a manifest with one region file and one playerdata file produces two canonical required source IDs.
- Red: a duplicate path appearing in two categories is rejected instead of silently deduplicated.
- Regression: discovery still excludes backup folders and detects physical aliases; this helper does not replace inode/hardlink preflight.

Formal CAT machine gates: not-run; dedicated Chat plugin/Skill unavailable. Draft is not Confirmed.
