---
id: SC-EXCLUDED-ENTITY-ALIAS-DRAFT
status: Draft
flow: code-to-spec
target: WorldStorageDiscovery.discover
source_task: TASK-20261101-003
date: 2026-11-04
---

# Excluded-world playerdata versus entity storage

## Candidate contract

An included world's `entities/r.0.0.mca` is authoritative even when the same physical inode appears as playerdata in an excluded world. A physical collision must fail closed, regardless of traversal order. Conversely, an excluded world's region/entity files are outside the accounting scope and cannot invalidate otherwise unique included-world playerdata merely because they are hard links. Included-world region and entity sources in different dimensions must also be kept physically distinct; aliasing would duplicate an authoritative saved source.

## Characterization vectors

| ID | Input | Expected |
| --- | --- | --- |
| SC-EXCLUDED-ENTITY-01 | included/entities/r.0.0.mca hard-linked to excluded/playerdata/<canonical UUID>.dat; included traversed first | IOException |
| SC-EXCLUDED-ENTITY-02 | same alias; excluded traversed first | IOException |
| SC-EXCLUDED-ENTITY-03 | included/playerdata/<canonical UUID>.dat hard-linked to excluded/entities/r.0.0.mca | one playerdata, zero worldFiles |
| SC-EXCLUDED-ENTITY-04 | included/region/r.0.0.mca hard-linked to included/DIM-1/entities/r.1.0.mca | IOException (cross-dimension, cross-kind physical alias) |

The test assumes the filesystem supports hard links; otherwise it aborts by JUnit assumption. These are Draft characterization vectors, not confirmed requirements or permission to modify production code.

## Gate status

Process/PI/TCE confirmation, process-closure, TestModel, model-conformance, vectors, Queue/current-item, implementation-conformance, Paper live: not-run.
