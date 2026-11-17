---
cat_version: '0.2'
kind: work
id: simplecoins.a01.packaged-writer-entrypoints
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0 and A02/G3 — packaged-plugin writer entrypoint inventory

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/470 |
| https://github.com/surumeneco/SimpleCoins/pull/471 |
| https://github.com/surumeneco/SimpleCoins/pull/475 |
| https://github.com/surumeneco/SimpleCoins/pull/438 |
| https://github.com/surumeneco/SimpleCoins/issues/4 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.grants.paper-save-commit | simplecoins.a01.packaged-writer-entrypoints | simplecoins.a01.live-writer-coherence-model;simplecoins.a02.verify-to-ack-writer-gap |

## Technologies

| Technology |
| --- |
| Python 3 standard-library classfile constant-pool parser and unittest; actual fixed GitHub MineCraftXPlayServer packaged JARs; GitHub Actions; pinned CAT issue-work structure validator |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.packaged-writer-entrypoints.md;tools/audit_writer_surface.py;tests/test_writer_surface_audit.py;.github/workflows/a01-distribution-writer-surface.yml | — | tools/audit_writer_surface.py;tests/test_writer_surface_audit.py;.github/workflows/a01-distribution-writer-surface.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Verified technical question

The adopted requirement is complete physical and saved-asset accounting with fail-closed behavior for unknown or untracked writes. [#470](https://github.com/surumeneco/SimpleCoins/pull/470)/[#471](https://github.com/surumeneco/SimpleCoins/pull/471) demonstrate that disk SHA stability and zero players do not prevent Paper LIVE mutation. [#475](https://github.com/surumeneco/SimpleCoins/pull/475) demonstrates the analogous after-proof writer-gap before journal ACK. Treating this as an operator choice *before* testing engineering solutions was corrected in Issue #423.

The next design input is an **observed list of potential write entrypoints in actual distributed plugin bytecode**, not speculation about what a Java module may do. Scan every `plugins/*.jar` in a **fixed snapshot** of public `surumeneco/MineCraftXPlayServer@90aa2d0e0a5ea80c705fd67d717f463decaf2a32`. The audit reads constant-pool `Methodref` and `InterfaceMethodref` records, identifies candidate Bukkit inventory/PDC/world mutations, direct saved-file operations, Paper playerdata saves, NMS mutations, async dispatch and dynamic/reflection usage, and writes a machine-readable JSON with exact per-JAR SHA256, class counts, method-call references, counts and examples. Require the actual packaged WCL/WWC/WWW/BlockLocker files rather than treating absence as an empty writer universe. Synthetic parser regressions must reject invalid class constants, duplicate names and missing required JARs.

## Interpretation boundary and stop condition

**No classfile reference count proves actual execution**, much less the complete set of first-party/third-party writer operations. Calls may be dead, shaded or executed through reflection; inherited callbacks, native code, dynamic classloading and Paper itself remain outside this enumeration. Conversely, no listed call does not prove no writes. This report is deliberately **static candidate evidence**, not a complete barrier, runtime event trace, actual VPS plugin list, G0/G3 guarantee, CAT Confirmed/Gate or a permission to ACK/publish/admit. It never edits world/playerdata, production sources, admission or `main`.

This Work finishes when actual pinned distribution JARs are enumerated under CI, the JSON digest/report is uploaded, parser tests pass and evidence can be used to prioritize **real runtime writer capture and quiescence integration experiments**. Do not continue adding speculative generic method categories without an observed need. Next work must validate plugin-specific mutation paths and an enforceable barrier/epoch through the commit boundary, not claim bytecode scanning solves G0/G3.
