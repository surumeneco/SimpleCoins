---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.discovery-visit-budget
status: draft
process: simplecoins.storage.discovery
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# WorldStorageDiscovery visit budget: existing behavior characterization

## Source and scope

TASK-20261101-003. Observed source `WorldStorageDiscovery.java` blob
`9050e638d5d10c6b4173ae400f62daca5b3edde7` on Draft PR #34
at `b88cee565e78376f0161965cc9fdf7ad3b196915`.

Code-to-spec **Draft**, not Confirmed. Tests record the present bounded
manifest-discovery behavior; they do not establish that an arbitrary
`maximumEntries` is operationally sufficient for actual Paper storage.

## Characterization vectors

| Vector | Setup | Observed expectation |
| --- | --- | --- |
| SC-DISCOVERY-VISIT-01 | one included root, one `region` directory and one canonical `.mca` file | budget 3 succeeds; budget 2 rejects with `IOException` |
| SC-DISCOVERY-VISIT-02 | one included world with 3 visited entries, plus an excluded world with 6 visited entries in unrelated backup locations | budget 9 succeeds with only the included source; budget 8 rejects, even though the extra entries are not accounting sources |
| SC-DISCOVERY-VISIT-03 | a root containing only unrelated backup/export data | discovered source set remains empty at a sufficient visit budget; reducing the budget rejects rather than publishing a partial manifest |

These vectors are separate from the existing budget-exhaustion test
using 12 authoritative regions; they characterize visit charges on
unrelated and excluded paths.

## Verification and boundaries

The only intended production inputs are the existing `WorldStorageDiscovery`
and `WorldScope` classes. Execute the exact JDK25 JUnit class and require
3/3 pass, no skips. This draft does not authorize source changes or
override normative CAT approvals. Raw saved-asset parsing, MCA chunk integrity,
memory/saved consistency, settlement publication, Paper runtime and CAT
process/model/queue Gates remain **not-run**.
