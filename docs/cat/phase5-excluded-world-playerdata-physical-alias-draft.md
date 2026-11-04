---
id: SC-EXCLUDED-WORLD-PLAYERDATA-ALIAS-DRAFT
status: Draft
flow: code-to-spec
target: WorldStorageDiscovery.discover
source_task: TASK-20261101-003
date: 2026-11-04
---

# Excluded-world playerdata physical identity

## Candidate contract

The world accounting filter excludes region/entity storage, not playerdata. Thus an excluded world remains a source of authoritative playerdata, and any physical alias between its playerdata and an included world's region or another excluded world's playerdata must fail closed. In contrast, a hard-linked region file from an excluded world is not an authoritative source and must not trigger a false physical-source collision.

## Test vectors

| ID | Included world | Excluded world | Expected |
| --- | --- | --- | --- |
| SC-EXCLUDED-ALIAS-01 | region/r.0.0.mca | playerdata/<canonical UUID>.dat hard-linked to region | IOException |
| SC-EXCLUDED-ALIAS-02 | playerdata/<canonical UUID>.dat | same UUID under excluded-world playerdata, hard-linked | IOException |
| SC-EXCLUDED-ALIAS-03 | region/r.0.0.mca | region/r.1.0.mca hard-linked | manifest contains only included region |

Hard-link capability is an explicit test assumption. These are candidate characterization tests, not normative CAT confirmation or production-change authorization.

## Gate status

Process/PI/TCE confirmation, process-closure, TestModel generation, model-conformance, vectors, Queue/current-item, implementation-conformance: not-run. Paper live: not-run.
