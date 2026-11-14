---
cat_version: '0.2'
kind: work
id: simplecoins.a1.incremental-mutation-directory-audit
status: draft
process: simplecoins.mutations.source-set-integrity
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

# Work: bounded runtime mutation source-set integrity

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/5 |
| https://github.com/surumeneco/SimpleCoins/pull/404 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.mutations.source-set-integrity | simplecoins.a1.incremental-mutation-directory-audit | simplecoins.mutations.record-integrity;simplecoins.mutations.live-journal-health |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2; JUnit; Files.newDirectoryStream; BukkitScheduler; isolated test-only Paper fixture |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java;src/main/java/co/surumene/simplecoins/paper/PaperNormalJournalWatchdog.java;src/test/java/co/surumene/simplecoins/domain/NormalMutationJournalDirectoryAuditTest.java;.github/workflows/paper-journal-directory-audit-smoke.yml | src/main/java/co/surumene/simplecoins/domain/NormalMutationJournal.java;src/main/java/co/surumene/simplecoins/paper/PaperNormalJournalWatchdog.java | src/test/java/co/surumene/simplecoins/domain/NormalMutationJournalDirectoryAuditTest.java;.github/workflows/paper-journal-directory-audit-smoke.yml |

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

- PR #404 checks *known* historical mutation files but cannot detect an unknown extra `.mutation` file inserted after startup. `NormalMutationJournal.verifyAll()` already rejects untracked files during full verification; this Work adds bounded incremental discovery without scanning the entire directory each Paper tick.
- Maintain one bounded `DirectoryStream` iteration across five-second watchdog heartbeats; examine at most a fixed number of `.mutation` directory entries per heartbeat. Compare the entire canonical filename against the sequence IDs in the currently synchronized journal. Unknown, malformed, extra and noncanonical suffix filenames fail-stop, not silently ignored.
- A legitimate new record created through `prepare()` must not be rejected. Directory entries added mid-iteration may not appear until the next pass, so this is eventual observation only. Always close directory cursor on exhaustion, failure and plugin/journal close. Never promise atomic file-set snapshots, known mutation world finality or immediate detection. Reuse the existing Paper one-shot fail-stop callback and independent admission guard.
- Isolated Paper 26.2 build 134 test: test-only plugin prepares one historical record, CI inserts a second rogue `.mutation` while Paper runs, leaves head and existing record unchanged, and verifies the watchdog reports unexpected durable file + automatic shutdown without login. The test-only plugin must not issue physical coins or ship with production jar.
- Product CAT semantic approval, Process/PI/TCE Gates, full saved+LIVE asset settlement, missing BAN/provider sources, production deployment and real JE/BE usage remain **not-run**; a successful CI does not imply release readiness.
