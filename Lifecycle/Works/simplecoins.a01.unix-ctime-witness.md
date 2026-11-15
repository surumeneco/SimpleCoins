---
cat_version: '0.2'
kind: work
id: simplecoins.a01.unix-ctime-witness
status: draft
process: simplecoins.settlement.saved-source-witness
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.a01.unix-ctime-witness

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedPlayerdataContentWitness.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.saved-source-witness | simplecoins.settlement.unix-change-time | simplecoins.settlement.sha256-stability;simplecoins.settlement.source-discovery |

## Technologies

| Technology |
| --- |
| Java 25, Unix filesystem change-time, JUnit 5 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.unix-ctime-witness.md;src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java;src/main/java/co/surumene/simplecoins/paper/SavedPlayerdataContentWitness.java;src/test/java/co/surumene/simplecoins/domain/WorldSnapshotTraversalTest.java;src/test/java/co/surumene/simplecoins/paper/SavedPlayerdataDriftAuditTest.java | src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java;src/main/java/co/surumene/simplecoins/paper/SavedPlayerdataContentWitness.java | src/test/java/co/surumene/simplecoins/domain/WorldSnapshotTraversalTest.java;src/test/java/co/surumene/simplecoins/paper/SavedPlayerdataDriftAuditTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| source-restored-change-time | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.WorldSnapshotTraversalTest --tests co.surumene.simplecoins.paper.SavedPlayerdataDriftAuditTest | same-size-mtime-SHA-restore;no-symlink;stable-source | WorldSnapshotTraversal;SavedPlayerdataContentWitness | JUnit XML | 180 |

## Gates

External/semantic gates are reported evidence. `passed` or `not-applicable` requires a concrete evidence/rationale reference.

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

## Source-grounded acceptance

- Existing SHA256/size/mtime/fileKey source witnesses miss an ABA writer who changes bytes during a read, restores the exact original bytes, and resets lastModifiedTime. A deterministic two-file transfer with initial aggregate 100, captured mixed aggregate 200, and final aggregate 100 reproduced this, despite matching pre/post SHA256/mtime/fileKey.
- **Ubuntu/Unix-only safety enhancement**: include filesystem change time (`unix:ctime`) in both WORLD source and saved playerdata per-file stamps, including stamp-before/stamp-after integrity, and reject unsupported change-time attributes rather than falling back to weak evidence. Preserve all existing byte and source-set checks and bounded IO.
- Test modifies a file twice restoring content and lastModifiedTime while the inode persists; a witness should now reject. The spec's G0 same-world LIVE state, other plugins and full temporal consistency **still not proven**. Do not open admission or publish money.
- Red must be the unsafe witness accepting restored file, Green must reject, after which product CI/regression is evidence only, not normative CAT gates. Process/PI/TCE Confirmed and formal gates remain not-run.

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
