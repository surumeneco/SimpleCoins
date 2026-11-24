---
cat_version: '0.2'
kind: work
id: simplecoins.a01.startup-source-rediscovery
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/actions/runs/45295796411
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.startup-world-universe-identity
  - simplecoins.a01.world-registry-transition-veto
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01/G0 — bounded fresh rediscovery before any publication

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/actions/runs/45295796411 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.startup-source-rediscovery | simplecoins.a01.world-registry-transition-veto;simplecoins.a01.startup-world-universe-identity |

## Technologies
| Technology |
| --- |
| Java 25; Paper 26.2; JUnit; bounded asynchronous source discovery; existing default-deny admission |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.startup-source-rediscovery.md;src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;src/test/java/co/surumene/simplecoins/domain/SavedManifestDiscoveryClosureTest.java;.github/workflows/paper-saved-discovery-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java | src/test/java/co/surumene/simplecoins/domain/SavedManifestDiscoveryClosureTest.java;.github/workflows/paper-saved-discovery-live-smoke.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Technical acceptance under existing Confirmed A01

The real Paper initial boot in run 45295796411 attempt 1 stopped at an authoritative saved source set change after ServerLoadEvent, before any manifest or settlement publication. Attempt 2 on identical HEAD succeeded. The exact changing source was not logged. This is a recoverable candidate invalidation only before publication; it is NOT permission to ignore drift or to treat an unprotected generation as valid.

- Recognize only the typed mismatch produced by two successful complete source discoveries. Discard every abandoned attempt and restart the entire discovery and saved receipt audit with fresh local evidence, up to five attempts with bounded off-main delay. Do not reuse an old manifest, receipt audit or metadata witness. Keep the existing global watchdog, default deny and world lifecycle generation; no retry of arbitrary IO failures, symlinks, orphan backups, invalid receipts, world registry transitions, budget exhaustion or cancellation. If attempts exhaust, safe stop as before.
- JUnit uses actual filesystem discovery, adds a source after the first manifest, checks fresh complete result and exact attempt count; persistent growth exhausts the bound, and a non-drift IO error is never retried. Existing strict callers of requireUnchangedDiscovery still fail immediately; no relaxation of protected G0/G3 stages.
- Real Paper smoke must retain one successful unpublished observation, independent Admission Guard, and self-stop for orphan .dat_old. This does not establish G0 writer closure or admit players.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-rediscovery-model.json | @work/.cat-flow/cache/a01-rediscovery-vectors.json | @work/.cat-flow/cache/a01-rediscovery-queue.json | — | — | @work/.cat-flow/cache/a01-rediscovery-obligations.json | — | false |
