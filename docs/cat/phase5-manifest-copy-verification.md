---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.manifest-copy-verification
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 — Manifest defensive copy and read-only closure (verification Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| manifest-defensive-copy | The existing Manifest record snapshots the caller-owned list and map rather than retaining mutable aliases |
| manifest-closure-readonly | The existing requiredSourceIds result is a read-only snapshot |

## Rules

| ID | Condition | Observed contract to verify | Verification |
| --- | --- | --- | --- |
| SC-COPY-01 | Caller mutates the source list after constructing Manifest | Existing worldFiles are unchanged | callerListMutationCannotChangeManifest |
| SC-COPY-02 | Caller mutates the player map after constructing Manifest | Existing playerDataFiles are unchanged | callerMapMutationCannotChangeManifest |
| SC-COPY-03 | Caller attempts to mutate the exposed worldFiles and playerDataFiles collections | Both reject mutation and remain unchanged | manifestCollectionsRejectExternalMutation |
| SC-COPY-04 | Caller attempts to mutate a requiredSourceIds closure | The closure rejects mutation and later reads remain unchanged | requiredSourceClosureIsReadOnly |

## Test vectors

| ID | Input | Expected result |
| --- | --- | --- |
| SC-COPY-01 | Mutable ArrayList with one SourceFile; add another after construction | Manifest contains only the original source |
| SC-COPY-02 | Mutable HashMap with one UUID/Path; clear and replace after construction | Manifest contains only the original UUID/Path |
| SC-COPY-03 | list.add and map.clear on Manifest accessors | UnsupportedOperationException; original values intact |
| SC-COPY-04 | Set.clear on requiredSourceIds result | UnsupportedOperationException; same closure IDs on subsequent read |

## Status and boundaries

**Draft verification of already present behavior only**, not a new approved normative Process/PI/TCE. The source under test is `WorldStorageDiscovery.java` in PR #34 at commit `b88cee565e78376f0161965cc9fdf7ad3b196915`. Its record constructor currently uses `List.copyOf` and `Map.copyOf`, and closure returns `Collections.unmodifiableSet`.

This Work adds no production implementation, does not resolve the separate physical-alias/required-source presence Draft Red proposals, and does not establish a coherent loaded/disk snapshot or Paper/Multiverse acceptance. Formal CAT semantic Gates and live integration remain not-run. Keep stacked Draft while PR #34 remains Draft.
