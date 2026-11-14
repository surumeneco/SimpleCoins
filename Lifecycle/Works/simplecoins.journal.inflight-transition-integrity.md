---
cat_version: '0.2'
kind: work
id: simplecoins.journal.inflight-transition-integrity
status: draft
process: simplecoins.journal.fail-closed-transitions
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
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/5
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.journal.inflight-transition-integrity

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.journal.fail-closed-transitions | simplecoins.journal.inflight-transition-integrity | simplecoins.journal.default-deny |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java;src/test/java/co/surumene/simplecoins/domain/NormalMutationJournalTest.java;Lifecycle/Works/simplecoins.journal.inflight-transition-integrity.md | src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java | src/test/java/co/surumene/simplecoins/domain/NormalMutationJournalTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| journal | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.NormalMutationJournalTest | journal-transition-drift;journal-duplicate-drift | durable monetary journal | JUnit XML | 180 |

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

## Grounded behavior and defect candidate

- **Adopted invariant**: SimpleCoins 02B calls for durable journal changes to fail-closed on uncertain operation state; no unconfirmed currency or physical assets may be counted or silently overwritten. This Work does **not** alter the gameplay or monetary semantics.
- **Observed source**: `NormalMutationJournal.markEventApplied` checks only the journal HEAD/lock before rewriting a saved `.mutation` file. A checksum-valid external replacement of that same file can be overwritten by a later phase transition, erasing evidence of the mismatch before the next bounded historical-record audit. The idempotent duplicate-operation `prepare` path likewise returns the in-memory record without checking the associated durable record.
- **Tests first**: after preparing one movement, change its persisted amount by 1 and recompute the durable SHA-256 wrapper, without touching HEAD or `.writer.lock`. Both `(a) markEventApplied` and `(b) prepare` with the same operation ID must reject, preserve the externally modified bytes, and permanently mark the journal unhealthy. Valid untouched phase transitions and repeated IDs remain accepted.
- **Bounded remedy**: verify *only the record about to be rewritten or re-used*, under the journal's existing synchronized monitor, using existing `DurableFile.read`/`decode`. This does not claim atomic protection against a concurrent malicious writer between verification and rename, nor substitute for cyclic historical auditing/complete world settlement.
- **CAT authority**: a source-grounded issue-work Draft is not a Confirmed Process/PI/TCE. No formal closure/model- or implementation-conformance Gate is declared as passed; independently measurable Java Red/Green and Paper guard CI are reported separately.
