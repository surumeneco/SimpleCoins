---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.excluded-nonregular-source-identity
status: draft
process: simplecoins.storage.discovery
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Excluded world source-shaped filesystem objects: current-code characterization

## Evidence and authority

TaskQueue TASK-20261101-003; code-to-spec **Draft/Candidate**, not Confirmed.
Read-only observation of `WorldStorageDiscovery.java` blob
`9050e638d5d10c6b4173ae400f62daca5b3edde7` at
`feature/phase5-world-storage-discovery-20261102-queue-b` HEAD
`b88cee565e78376f0161965cc9fdf7ad3b196915`.
World exclusion is driven by `WorldScope`, but playerdata is world-independent
under the approved `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02A_所有者・統計定義.md`.

## Bounded observed vectors

| Vector | Fixture | Existing behavior to characterize |
| --- | --- | --- |
| SC-EXCLUDED-SOURCE-SHAPE-01 | Included world's valid `region/r.0.0.mca`; excluded world's `region/r.1.0.mca` and `entities/r.2.0.mca` are **directories**, excluded world's valid canonical playerdata | Only included region and excluded playerdata are required; no phantom terrain source |
| SC-EXCLUDED-SOURCE-SHAPE-02 | Included world's valid `region/r.0.0.mca` and source-shaped **directory** `entities/r.1.0.mca` | IOException; fail closed rather than publish partial included-world manifest |
| SC-EXCLUDED-SOURCE-SHAPE-03 | Excluded world's malformed region name `r.+0.0.mca` and malformed canonical-playerdata candidate `1-1-1-1-1.dat` | World-owned region is excluded, but malformed playerdata aborts instead of disappearing |
| SC-EXCLUDED-SOURCE-SHAPE-04 | Excluded world's source-shaped symbolic link under `region/` | IOException; excluded-world scope is not permission to traverse unsafe symbolic links |

These vector outcomes reflect **current implementation behavior**, not a complete
guarantee about Paper/Multiverse catalogs, NBT/Anvil contents, disk/live
deduplication, or snapshot consistency. The corresponding JUnit fixture tests
use a positive visit budget and filesystem isolation; symbolic link support
must be available on the Linux/JDK test runner.

## Gate and integration boundaries

The code-to-spec characterization does not edit production source, does not
promote this draft to confirmed, and does not pass CAT process-closure,
model-conformance, Queue, implementation-conformance, Paper live, or G0 by
itself. JUnit Green on JDK25 is only scoped existing-behavior evidence.
Upstream PR #34 is still Draft; this branch is stacked on it and must not
be merged directly to develop before dependency and normative gates.
