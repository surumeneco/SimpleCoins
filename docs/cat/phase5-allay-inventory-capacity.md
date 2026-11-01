# Phase 5: Saved Allay inventory physical capacity (CAT Draft)

Status: **Draft, not Confirmed**. Sources: SimpleCoins 02B Entity asset accounting and 05_1.0.0 Phase 5/G0 fail-closed source validation.

## Process / PI

A vanilla Allay has one physical carried inventory slot. When its saved `Inventory` list contains two or more entries, the read-only settlement decoder must reject the source rather than count additional fictitious currency. One valid entry remains countable. The rule concerns saved storage shape only and does not establish ownership, loaded-Entity reconciliation or world snapshot consistency.

## Test conditions

- AL-01: `minecraft:allay` with two coin stacks in `Inventory` raises `IOException`.
- AL-02: one coin stack remains countable.

Formal CAT machine Gates: **not-run**. This is a Draft, not a Confirmed normative Artifact.
