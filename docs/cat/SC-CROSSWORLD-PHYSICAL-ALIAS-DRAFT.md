---
id: SC-CROSSWORLD-PHYSICAL-ALIAS-DRAFT
status: Draft
flow: code-to-spec
target: WorldStorageDiscovery.discover
source_task: TASK-20261101-003
date: 2026-11-04
---

# Cross-world physical alias detection (CAT Draft)

## Scope
Characterize the current discovery contract when two distinct world roots contain different authoritative paths that resolve to the same physical inode through hard links. The world-root overlap check alone cannot detect this case.

## Candidate rules
1. A region file in one included world and a differently named region file in a second included world cannot both become authoritative manifest entries when they share a physical file.
2. A region file in one world and a canonical playerdata file in a different world cannot both become authoritative inputs when hard-linked.
3. Both cases must throw IOException before returning a partial manifest, regardless of which world is visited first.
4. A filesystem without hard-link support is not evidence of successful alias rejection; such environments must report an explicit test assumption skip.
5. No production change or CAT normative confirmation is implied by this Draft.

## Vectors
| ID | First root | Second root | Ordering | Expected |
| --- | --- | --- | --- | --- |
| SC-CROSSWORLD-ALIAS-01 | region/r.0.0.mca | region/r.1.0.mca (hard link) | A then B | IOException |
| SC-CROSSWORLD-ALIAS-02 | region/r.0.0.mca | playerdata/<canonical-UUID>.dat (hard link) | B then A | IOException |

## Gate state
Draft/Candidate only. Normative CAT confirmation, process-closure, TestModel, model-conformance, vectors, Queue, current-item, and implementation-conformance Gates are not-run. No production source is changed.
