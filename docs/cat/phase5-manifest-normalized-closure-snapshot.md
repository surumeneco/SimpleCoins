---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.manifest-normalized-closure-snapshot
status: candidate
process: simplecoins.world-storage-discovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/b88cee565e78376f0161965cc9fdf7ad3b196915/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# WorldStorageDiscovery Manifest: normalized closure and snapshot — code-to-spec Candidate

## 参照と権威

- Source: PR #34, commit `b88cee565e78376f0161965cc9fdf7ad3b196915`, `WorldStorageDiscovery.java` blob `9050e638d5d10c6b4173ae400f62daca5b3edde7`.
- This is **observed implementation behavior**, not a Confirmed settlement specification. `Manifest.requiredSourceIds()` is a manifest-metadata closure, not a proof of persisted file existence, snapshot freshness, physical de-duplication, complete asset inventory or publication authorization.
- Work: `TASK-20261101-003` and SD-04/SD-08. Test target: `WorldStorageManifestClosureSnapshotCandidateTest`.

## Observations / Candidates

| ID | Existing source observation | Test |
| --- | --- | --- |
| MSC-01 | `requiredSourceIds()` applies `toAbsolutePath().normalize().toString()` to world and player paths; two lexical aliases of one normalized region ID throw `IOException`. | `normalizationRejectsTwoLexicalRegionAliases` |
| MSC-02 | The same normalized path cannot appear in both world and playerdata source categories, even with different literal spellings. | `normalizationRejectsCrossCategoryLexicalAlias` |
| MSC-03 | `Manifest` snapshots caller-supplied list/map with `List.copyOf`/`Map.copyOf`, and the exposed collections and closure IDs cannot be mutated through their public interfaces. | `callerMutationCannotRewriteManifestOrClosure` |
| MSC-04 | The returned closure IDs have deterministic natural sorted iteration order (`TreeSet`), independently of insertion order in the input lists and maps. | `closureKeysAreSortedAcrossSourceKinds` |

## Unresolved decisions

These observed properties are not permission to treat manually created manifests as verified storage, to accept directory races, or to classify missing/unreadable assets as zero. Physical identity, saved-versus-loaded deduplication, NBT read coverage, actual JDK25/Paper integration, confirmed CAT Process/PI/TCE and publication Gate remain separate. Reconcile with the normative specification before any production changes or Confirmed promotion.

## Evidence boundary

The accompanying JUnit tests characterize existing code only; a passing JUnit/CI outcome is not process-closure, model-conformance, implementation-conformance, SD-07/08 or release approval.
