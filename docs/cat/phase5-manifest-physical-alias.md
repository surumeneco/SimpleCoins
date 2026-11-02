---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.manifest-closure-physical-alias
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 — manual manifest closure and physical aliases (CAT Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| manifest-closure-identity | WorldStorageDiscovery.Manifest.requiredSourceIds() must not certify duplicate physical sources under distinct lexical names |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-MANIFEST-01 | Two distinct region paths in a manually constructed Manifest refer to the same inode through hard links | Reject closure with IOException, rather than return two required source IDs | hardlinkedRegionPathsInManualManifestCannotClose |
| SC-MANIFEST-02 | A region path and a canonical playerdata path in a manually constructed Manifest are hard links | Reject closure across source categories | hardlinkedRegionAndPlayerdataInManualManifestCannotClose |
| SC-MANIFEST-03 | Two distinct files contain identical bytes but have distinct physical identities | Preserve both required source IDs | identicalContentIndependentFilesRemainDistinct |
| SC-MANIFEST-04 | A manually constructed Manifest repeats the exact same path across categories | Preserve existing duplicate-path rejection | identicalLexicalPathStillRejected |

## Test vectors

| ID | Input | Required result |
| --- | --- | --- |
| SC-MANIFEST-01 | region/r.0.0.mca and region/r.1.0.mca via Files.createLink | IOException |
| SC-MANIFEST-02 | region/r.0.0.mca and playerdata/UUID.dat via Files.createLink | IOException |
| SC-MANIFEST-03 | Two separate Files.write calls with identical byte arrays | Two distinct source IDs |
| SC-MANIFEST-04 | Same Path as world file and playerdata file | IOException |

## Status and authority

**Draft only.** The existing manifest utility currently rejects duplicate lexical paths but does not inspect physical identity in requiredSourceIds(). This proposed stricter closure rule is not yet Confirmed and must not authorize production changes. This regression isolates a possible manual-construction bypass from the already tested discover() physical-alias guard (PR #65); it does not duplicate that scanner's tests.

Normative CAT Process/PI/TCE confirmation and machine Gates, production Green, and Paper/Multiverse acceptance: **not-run**. Do not merge a Red Draft or treat closure as a coherent live/disk snapshot.
