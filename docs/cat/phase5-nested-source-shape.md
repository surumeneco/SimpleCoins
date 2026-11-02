---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.nested-source-shape
status: draft
process: simplecoins.snapshot
source_refs:
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Nested source directories (Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| nested-container | Nested authoritative source directory types |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-NESTED-01 | Included root/entities is a file | IOException | entityContainerIsFile |
| SC-NESTED-02 | Included DIM-1/region is a file | IOException | legacyRegionContainerIsFile |
| SC-NESTED-03 | Included custom dimension entities is a file | IOException | customEntityContainerIsFile |
| SC-NESTED-04 | Legacy players/data is a file | IOException | legacyPlayerdataContainerIsFile |

## Test vectors

| ID | Input | Required result |
| --- | --- | --- |
| SC-NESTED-01 | world/entities file | IOException |
| SC-NESTED-02 | world/DIM-1/region file | IOException |
| SC-NESTED-03 | world/dimensions/custom/moon/entities file | IOException |
| SC-NESTED-04 | world/players/data file | IOException |

CAT Draft, not Confirmed; formal Gates and Paper acceptance not-run.
