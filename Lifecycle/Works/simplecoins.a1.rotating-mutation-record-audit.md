---
cat_version: '0.2'
kind: work
id: simplecoins.a1.rotating-mutation-record-audit
status: draft
process: simplecoins.mutations.record-integrity
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
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

# Work: rotating monetary mutation record audit

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/5 |
| https://github.com/surumeneco/SimpleCoins/pull/403 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.mutations.record-integrity | simplecoins.a1.rotating-mutation-record-audit | simplecoins.mutations.live-journal-health;simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2; JUnit; durable mutation records; BukkitScheduler; isolated Paper test-only plugin |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java;src/main/java/co/surumene/simplecoins/paper/PaperNormalJournalWatchdog.java;src/test/java/co/surumene/simplecoins/domain/NormalMutationJournalRotatingAuditTest.java;src/test/java/co/surumene/simplecoins/paper/JournalRecordLiveProbe.java;tests/paper-journal-record-smoke/*;build.gradle.kts;.github/workflows/paper-journal-record-audit-smoke.yml | src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java;src/main/java/co/surumene/simplecoins/paper/PaperNormalJournalWatchdog.java | src/test/java/co/surumene/simplecoins/domain/NormalMutationJournalRotatingAuditTest.java;src/test/java/co/surumene/simplecoins/paper/JournalRecordLiveProbe.java;tests/paper-journal-record-smoke/*;build.gradle.kts;.github/workflows/paper-journal-record-audit-smoke.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

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

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Source-grounded boundaries

- Adopted 02B requires persistent normal mutation journal integrity and fail-closed behavior for unknown currency states. PR #403 polled only durable head and writer-lock identity; existing `verifyAll()` checked historical records on startup, not continuously.
- Add a bounded rotating audit that independently checks the exact durable bytes/record value for one existing `.mutation` record per call, in ascending sequence with wraparound. Detect valid-but-mutated older entries and disappeared files. Mark journal unhealthy irreversibly on mismatch. New legitimate tail reservations and `markEventApplied` must be observed using the current synchronized in-memory record, not a stale snapshot.
- Invoke a bounded batch from the already scheduled Paper journal watchdog, avoiding full-file scan on each game tick and avoiding duplicate timers. A failure follows the same one-shot shutdown path as a corrupted durable head, without logging in.
- Real isolated Paper build 134 CI must use a **test-only** plugin to prepare a record and then externally overwrite its `.mutation` bytes while Paper runs. Require a distinct proof of record-level failure and automatic Paper termination. The fixture may create a proposed external→free mutation on a server whose admission guard stays closed; it must **never** issue actual coins, and must be excluded from shipped SimpleCoins.
- This rotating audit is defense-in-depth: full source-set validation, unknown newly inserted `.mutation` files, saved-world physical accounting, price/issuer consistency and user admission **remain unproved**. A successful Paper test is not a formal CAT Gate, production deployment or real user test.
