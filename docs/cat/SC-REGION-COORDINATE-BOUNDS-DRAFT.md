---
title: Phase 5 Anvil coordinate boundary candidates
status: draft
source_work: TASK-20261101-003
normative: false
---

# Phase 5 — Anvil coordinate boundary candidates

These are **candidate** regression vectors for the existing `WorldStorageDiscovery` scanner, not a confirmed CAT specification. Do not treat them as authority to change production behavior before the formal CAT process.

## Candidate process

Given a world root with a direct `region/` directory and one source-shaped file, discovery must either accept a canonical 32-bit signed Anvil region coordinate or fail closed for an invalid coordinate. No partial manifest may be published on failure.

| Vector | Region filename | Candidate outcome |
| --- | --- | --- |
| SC-REGION-BOUND-01 | `r.-2147483648.2147483647.mca` | accept exactly one region source |
| SC-REGION-BOUND-02 | `r.2147483647.-2147483648.mca` | accept exactly one region source |
| SC-REGION-BOUND-03 | `r.2147483648.0.mca` | throw IOException (overflow) |
| SC-REGION-BOUND-04 | `r.-2147483649.0.mca` | throw IOException (underflow) |
| SC-REGION-BOUND-05 | `r.+2147483647.0.mca` | throw IOException (noncanonical alias) |

## Evidence and limitations

Existing production `requireCanonicalRegionFilename` uses `Integer.parseInt` and compares the filename with its canonical reconstruction. These vectors characterize that existing boundary, but do not establish whether a future Anvil implementation should support a different coordinate domain. Requires CAT authority confirmation and exact-head JDK25 JUnit execution. No implementation or release is authorized by this Draft.
