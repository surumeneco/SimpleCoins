---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.blocklocker-palette-isolated-invalidity
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: independent malformed saved-sign palette rejection (CAT Draft)

## Existing test weakness

PR #41's `BlockLockerSavedSignMountTest.invalidPaletteStatesFailClosed` includes malformed-facing, rotation, unsupported-block and unexpected-property fixtures with no `waterlogged` property. Those tests prove that an IOException occurs but cannot isolate the intended rejection condition because modern saved sign palettes require `waterlogged` independently. This Draft proposes targeted regression evidence, not a change to the accepted BlockLocker semantics.

## Proposed TCEs

| ID | Given | Expected |
| --- | --- | --- |
| PALETTE-ISOLATE-01 | Complete wall palette with invalid facing | `Invalid saved wall sign facing` |
| PALETTE-ISOLATE-02 | Complete standing palette with invalid rotation | `Invalid saved standing sign rotation` |
| PALETTE-ISOLATE-03 | Complete palette with an orientation property of the wrong sign type | `Unexpected saved sign palette property` |
| PALETTE-ISOLATE-04 | Complete palette with unsupported wood, hanging sign, non-sign block or namespace | Respective unsupported/unknown block rejection |
| PALETTE-ISOLATE-05 | Missing waterlogged, malformed waterlogged, missing orientation or non-string property, each isolated | Respective property-specific rejection |

## Verification and limitations

Source target is `BlockLockerSavedSignMount.java` Git blob `6f014deef4106618c309ebcd756280b4c71e9d40`, on stacked PR #41. All tests preserve valid unrelated palette fields and inspect the error message to prevent masked false positives. No production code is changed.

**Draft / not Confirmed.** Formal CAT Process/PI/TCE confirmation and machine Gates, full Gradle/JUnit, Paper/BlockLocker live acceptance and merge are not-run. This test-only increment must remain Draft until required Gates and parent PR #41 are resolved.
