---
cat_version: '0.2'
kind: work
id: simplecoins.grants.startup-persistent-verification
status: draft
process: simplecoins.grants.startup-guarded-evidence
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.grants.startup-persistent-verification

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.startup-guarded-evidence | simplecoins.grants.startup-persistent-verification | simplecoins.grants.existing-journal-contract |

## Technologies

| Technology |
| --- |
| Paper 26.2; Java 25; JUnit 5 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.startup-persistent-verification.md;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;.github/workflows/paper-guarded-bootstrap-smoke.yml | src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | .github/workflows/paper-guarded-bootstrap-smoke.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| paper | regression | junit | @repo:SimpleCoins | gradle test | GrantLedgerTest | plugin JAR; Paper 26.2; grant head | GitHub JUnit and Paper startup logs | 180 |

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

## Source-grounded gap, acceptance and exclusions

- Accepted 03 specifies a durable issuance reservation journal; `GrantLedger` already performs checksummed record replay, sequence verification, head recovery and fail-closed corruption detection. However `SimpleCoinsPlugin.onEnable()` only opens `NormalMutationJournal`, never `GrantLedger`. A damaged grant journal can therefore go entirely unexamined during plugin bootstrap.
- **Test first**: official Paper 26.2 guarded startup should initialize `plugins/SimpleCoins/grants/head` (the journal's chosen plugin-private storage path), preserve independent default-deny, and stop Paper on corrupt grant head while `normal-mutations/head` is intact. Failure before the new code is evidence of missing bootstrap verification.
- **Production**: open one `GrantLedger` after the normal movement journal's successful verification and before registering any Paper event/recipe/service. On any `IOException`, fail-stop via the existing outer guarded init error path. No `reserve`, `acknowledge`, delivery or ledger-to-inventory action is introduced.
- **Limits**: this does not prove exactly-once issuance, user command permissions, inventory save, crash-recovery idempotence or physical settlement. The journal is checked on startup, not live-monitored; it is not yet a complete grant delivery service.
- **CAT**: structural Work validation may pass, while normative Confirmed Process/PI/TCE and CAT machine Gates are unverified. This issue-work Draft cannot be self-promoted to Confirmed; JUnit and Paper CI are separate external evidence.
