---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.source-container-shape
status: draft
process: simplecoins.snapshot
source_refs:
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Source container shape (Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| source-container | Directory type of authoritative storage containers |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-CONTAINER-01 | Included world region or entities path is a regular file | Reject discovery with IOException | regionContainerIsFile |
| SC-CONTAINER-02 | Playerdata directory path is a regular file | Reject discovery with IOException, including excluded worlds | playerdataContainerIsFile |
| SC-CONTAINER-03 | Existing empty source directories | Accept an empty manifest | emptyDirectoriesAreValid |
| SC-CONTAINER-04 | Excluded terrain or unrelated backup container is a regular file | Ignore non-authoritative data | excludedAndBackupAreIgnored |

## Test vectors

| ID | Input | Required result |
| --- | --- | --- |
| SC-CONTAINER-01 | world/region is a file | IOException |
| SC-CONTAINER-02 | world/playerdata is a file | IOException |
| SC-CONTAINER-03 | world/region is an empty directory | Empty manifest |
| SC-CONTAINER-04 | excluded/region is a file | Empty manifest |

This Draft is not Confirmed. Formal CAT Gates and Paper acceptance are not-run. No production changes are authorized.
