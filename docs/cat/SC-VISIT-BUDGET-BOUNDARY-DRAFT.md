---
id: SC-VISIT-BUDGET-BOUNDARY-DRAFT
status: Draft
flow: code-to-spec
target: WorldStorageDiscovery.discover
source_task: TASK-20261101-003
date: 2026-11-04
---

# Saved-source discovery: visit-budget boundary (CAT Draft)

## Targets
- `WorldStorageDiscovery.discover(worldFolders, scope, maximumEntries)` on an isolated, caller-supplied world root.
- Visit budget includes directories and unrelated regular files, not only recognized `.mca` and `.dat` files.

## Rules
1. Exactly one visited world-root directory with `maximumEntries=1` is a valid empty manifest.
2. Adding a single unrelated regular file consumes another visit; `maximumEntries=1` must fail closed, even if the file does not match any saved asset category.
3. The same two-entry tree with `maximumEntries=2` succeeds with empty world/player manifests.
4. A failure must not return or publish a partial manifest. These cases characterize the existing bounded traversal; they do not certify complete Paper world-catalog coverage or stable snapshots.

## Test vectors
| ID | Filesystem | Budget | Expected |
| --- | --- | ---: | --- |
| SC-BUDGET-01 | root directory only | 1 | success, empty manifest |
| SC-BUDGET-02 | root and unrelated `notes.txt` | 1 | IOException |
| SC-BUDGET-03 | root and unrelated `notes.txt` | 2 | success, empty manifest |

## Gate state
Candidate/Draft only. Formal CAT process-closure, TestModel, model-conformance, vectors and implementation-conformance Gates are not run; no Confirmed normative promotion is asserted. JUnit assertions are a characterization of current behavior, not authorization for production changes.
