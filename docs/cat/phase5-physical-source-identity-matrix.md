---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.physical-source-identity-matrix
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 — saved-source physical identity matrix (CAT Draft)

This test-only Draft strengthens the existing fail-closed physical-source identity invariant in `WorldStorageDiscovery`. The current implementation uses filesystem file keys and `Files.isSameFile` to reject aliases among authoritative saved-source candidates. It must not confuse identical bytes with identical physical files. The Draft does not authorize changes to production code.

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot.physical-source-identity | Distinguish physical aliases from independent authoritative sources across categories and UUIDs |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-PHYSICAL-01 | Two distinct canonical player UUID filenames are hard links to the same physical data file | Reject manifest discovery with IOException rather than count both UUIDs | distinctPlayerUuidsHardLinkedToOneFileFailClosed |
| SC-PHYSICAL-02 | A canonical region and entity region source are hard links to the same physical file | Reject manifest discovery with IOException across Kind boundaries | regionAndEntityHardlinkFailClosed |
| SC-PHYSICAL-03 | Two canonical region files contain identical bytes but are independently allocated | Accept both files; content equality is not physical aliasing | identicalBytesInIndependentFilesRemainSeparateSources |
| SC-PHYSICAL-04 | An excluded world's region hard-links to an included world's region | Do not count excluded terrain or falsely reject the included authoritative source | excludedWorldRegionDoesNotBecomeMandatoryPhysicalSource |
| SC-PHYSICAL-05 | Distinct player UUID files in different world roots are hard links | Reject the shared physical source across world boundaries | crossWorldPlayerdataHardlinkFailsClosed |
| SC-PHYSICAL-06 | Excluded-world playerdata hard-links to included-world region data | Playerdata is still mandatory, so reject the cross-category physical alias | excludedWorldPlayerdataAliasToIncludedRegionFailsClosed |

## Test vectors

| ID | Input | Required result |
| --- | --- | --- |
| SC-PHYSICAL-01 | playerdata/UUID-A.dat and playerdata/UUID-B.dat created with Files.createLink | IOException |
| SC-PHYSICAL-02 | region/r.0.0.mca and entities/r.1.0.mca created with Files.createLink | IOException |
| SC-PHYSICAL-03 | region/r.0.0.mca and region/r.1.0.mca created with separate Files.write of identical bytes | Two REGION sources |
| SC-PHYSICAL-04 | included/region/r.0.0.mca and excluded/region/r.0.0.mca hard-linked, with excluded world filtered by WorldScope | One REGION source from included world |
| SC-PHYSICAL-05 | root-A/playerdata/UUID-A.dat and root-B/playerdata/UUID-B.dat hard-linked | IOException |
| SC-PHYSICAL-06 | included/region/r.0.0.mca and excluded/playerdata/UUID.dat hard-linked, excluded world filtered | IOException |

## Scope and verification status

The implementation under test is the pure Java source manifest utility extracted into PR #34; this test-only branch is stacked on that exact head. This does not prove an authoritative Paper/Multiverse world catalog, snapshot consistency, live/disk reconciliation, or safe settlement publication.

CAT semantic Process/PI/TCE are not Confirmed; formal machine Gates and Paper live acceptance are not-run. The matrix is regression preparation, not a release approval. Do not merge to develop/main until applicable Gate and exact-head tests pass.
