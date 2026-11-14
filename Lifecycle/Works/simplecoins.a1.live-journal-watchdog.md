---
cat_version: '0.2'
kind: work
id: simplecoins.a1.live-journal-watchdog
status: draft
process: simplecoins.mutations.live-journal-health
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

# Work: simplecoins.a1.live-journal-watchdog

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/5 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/pull/386 |
| https://github.com/surumeneco/SimpleCoins/pull/392 |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.mutations.live-journal-health | simplecoins.a1.live-journal-watchdog | simplecoins.settlement.admission-barrier;simplecoins.mutations.durable-journal |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2; JUnit; BukkitScheduler; durable normal-mutation journal |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/paper/PaperNormalJournalWatchdog.java;.github/workflows/paper-normal-journal-watchdog-smoke.yml;src/test/java/co/surumene/simplecoins/paper/PaperNormalJournalWatchdogTest.java | src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/paper/PaperNormalJournalWatchdog.java | .github/workflows/paper-normal-journal-watchdog-smoke.yml;src/test/java/co/surumene/simplecoins/paper/PaperNormalJournalWatchdogTest.java |

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

## Source-grounded runtime boundaries

- `NormalMutationJournal` currently validates the exact durable record set on bootstrap. It has a cheap `checkHealthy()` method for the head and writer lock, but no recurring Paper task calls it. The independent plugin admission guard is default-deny while asset settlement is incomplete. A subsequently corrupted head or replaced writer lock must not be ignored until shutdown or the next login.
- After journal open/verification and before allowing runtime operations, install a bounded periodic main-thread health check. Never run arbitrary saved-world NBT scans inside the watchdog. A failure must revoke publication, mark admission fatal, log an exact recognizable marker, and request Paper shutdown. Avoid duplicate task registration, cancellation races and late invocation after onDisable; never reopen the gate from watchdog success.
- The Paper CI must start the real SimpleCoins+independent guard JARs in a clean world, wait until journal head exists and initialization completes, then externally corrupt head bytes while Paper remains online. The process must stop automatically through the watchdog, without a player login or admin action. Verify the watchdog failure marker and independent guard activation in logs, and never count a process killed by CI as success.
- The clean-process startup and existing corrupt-on-boot fail-stop tests must continue to pass. `checkHealthy` is only a cheap liveness check and does **not** establish that all `.mutation` records still match the world, save commit, authoritative settlement or permission to admit players.
- Product CAT normative Process/PI/TCE approval, full journal reconciliation, all mutation event interception and server operational checks remain `not-run` even if isolated Paper/JUnit CI passes.
