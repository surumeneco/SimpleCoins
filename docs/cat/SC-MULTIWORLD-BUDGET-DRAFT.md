---
id: SC-MULTIWORLD-BUDGET-DRAFT
status: Draft
flow: code-to-spec
target: WorldStorageDiscovery.discover
source_task: TASK-20261101-003
date: 2026-11-04
---

# Shared visit budget across world roots (CAT Draft)

## Targets
- `WorldStorageDiscovery.discover(worldFolders, scope, maximumEntries)` with two distinct, empty world directories.
- The visit counter is scoped to the entire discovery invocation, not reset for each world.

## Rules
1. Two empty world roots consume two visits in total, irrespective of their map iteration order.
2. A budget of one must reject the combined discovery with an `IOException`, without returning a partial manifest.
3. A budget of two must return an empty manifest for both roots.
4. An explicitly excluded world still consumes a visit: scope exclusion affects accounting, not traversal safety.
5. These are candidate characterizations of current behavior; they do not authorize publication of settlement statistics or imply a consistent filesystem snapshot.

## Test vectors
| ID | Roots | Scope | Maximum entries | Expected |
| --- | --- | --- | ---: | --- |
| SC-MULTI-BUDGET-01 | two empty roots | both included | 1 | IOException |
| SC-MULTI-BUDGET-02 | two empty roots | both included | 2 | empty manifest |
| SC-MULTI-BUDGET-03 | two empty roots | second excluded | 1 | IOException |
| SC-MULTI-BUDGET-04 | two empty roots | second excluded | 2 | empty manifest |

## Gate state
Draft/Candidate only. Normative CAT confirmation and process-closure, TestModel, model-conformance, vectors, Queue, current-item, and implementation-conformance Gates have not been executed. No production code changes are authorized by this artifact.
