---
id: SC-MULTIWORLD-ORDER-DRAFT
status: Draft
flow: code-to-spec
target: WorldStorageDiscovery.discover
source_task: TASK-20261101-003
date: 2026-11-04
---

# World root ordering and shared traversal budget (CAT Draft)

## Scope
Characterize the existing discovery implementation with two distinct empty world roots. A single visit budget must apply across all roots regardless of map insertion order. Excluding one world must not exempt its root from the traversal budget.

## Candidate vectors

| ID | Root insertion order | Exclusion | Budget | Expected |
| --- | --- | --- | ---: | --- |
| SC-MULTI-ORDER-01 | first, second | none | 1 | IOException |
| SC-MULTI-ORDER-02 | second, first | none | 1 | IOException |
| SC-MULTI-ORDER-03 | first, second | second | 2 | empty manifest |
| SC-MULTI-ORDER-04 | second, first | second | 2 | empty manifest |

## Boundaries
These are candidate observations, not confirmed normative requirements. They do not assert atomic filesystem snapshots or authorization to publish statistics. CAT confirmation and all normative Gates remain unexecuted; no production change is authorized.
