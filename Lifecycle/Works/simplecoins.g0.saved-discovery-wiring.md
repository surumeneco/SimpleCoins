---
cat_version: '0.2'
kind: work
id: simplecoins.g0.saved-discovery-wiring
status: draft
process: simplecoins.settlement.saved-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/3
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.g0.saved-discovery-wiring

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |
| https://github.com/surumeneco/SimpleCoins/issues/3 |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.saved-discovery | simplecoins.g0.saved-discovery-wiring | simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2; JUnit; saved MCA + playerdata filesystem |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/paper/WorldScopeConfigReader.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;src/main/resources/config.yml;src/test/java/co/surumene/simplecoins/paper/WorldScopeConfigReaderTest.java;src/test/java/co/surumene/simplecoins/paper/SavedDiscoveryLiveProbe.java;tests/paper-saved-discovery-smoke/*;build.gradle.kts;.github/workflows/paper-saved-discovery-live-smoke.yml | src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/paper/WorldScopeConfigReader.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;src/main/resources/config.yml | src/test/java/co/surumene/simplecoins/paper/WorldScopeConfigReaderTest.java;src/test/java/co/surumene/simplecoins/paper/SavedDiscoveryLiveProbe.java;tests/paper-saved-discovery-smoke/*;build.gradle.kts;.github/workflows/paper-saved-discovery-live-smoke.yml |

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

## Scoped source-backed behavior

- Current user-approved 02B/03 specification: default world accounting ON; explicit ON/OFF lists; when both contain a world name, use the configured default; world-independent playerdata/ender-chest is always in scope. Names and exact config structure are technical choices, not edits to the normative gameplay rule.
- `WorldStorageDiscovery.discover` is already implemented and pure Java. It walks bounded known world folders, includes 26.1+ `dimensions/minecraft/...` and `players/data`, validates canonical file names, path aliases and symlinks, and does not parse MCA or prove content consistency. It has never been connected to Paper bootstrap.
- Capture the actual loaded Paper World names and `getWorldFolder()` paths on the main thread; run ONLY the filesystem walk on a Bukkit asynchronous task, with a strict, configurable count budget. Keep the atomic admission guard blocked and normal journal valid throughout. Do not call main-thread-only Bukkit methods from the worker.
- A completed discovery manifest is **not** an accounting snapshot. Do not mark settlement Complete, publish any statistics, authorize players, register issuance, or infer full set completeness from just a directory walk. Persistent source content, loaded world data and external ownership/BAN evidence need separate gates.
- On failed bounded discovery, preserve default-deny and request safe server stop via a main-thread task; on plugin shutdown, prevent a late worker from modifying state or scheduling a stop after disabling. Project implementation should not block the Paper main thread for bulk disk I/O.

## Acceptance boundaries

1. Paper config reader rejects nonboolean default, scalar/nonnull nonstring arrays, empty world identifiers, and invalid budget; overlaps use default, independent sources remain included.
2. Isolated real Paper 26.2 server boots with normal monetary journal, captures actual world folder mapping, then emits a discovery-only diagnostic marker after pure Java bounded source discovery. No player admission or false monetary publication.
3. The worker can observe a failed scan without silently passing; ordinary shutdown prevents late messages or callbacks. Invalid symlinks/files/budgets never publish a false empty result. Source files remain untouched.
4. Java/JUnit and Paper CI must be measured. Product formal CAT process/model/implementation conformance remains `not-run`; no simulated approval or real-world settlement claim.
