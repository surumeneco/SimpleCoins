---
cat_version: '0.2'
kind: work
id: simplecoins.a01.runtime-writer-roster-continuity
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 — runtime writer roster continuity and fail-closed invalidation

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.runtime-writer-roster-continuity | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.runtime-writer-roster-continuity.md;src/main/java/co/surumene/simplecoins/domain/WriterRosterWitness.java;src/main/java/co/surumene/simplecoins/paper/PaperWriterRosterMonitor.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/WriterRosterWitnessChecks.java;src/test/java/co/surumene/simplecoins/domain/WriterRosterWitnessTest.java;tests/paper-runtime-writer-roster-probe/src/main/java/co/surumene/simplecoins/a01probe/RuntimeWriterRosterProbe.java;.github/workflows/paper-runtime-writer-roster-probe.yml;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryHopperOutputWriterProbe.java | src/main/java/co/surumene/simplecoins/domain/WriterRosterWitness.java;src/main/java/co/surumene/simplecoins/paper/PaperWriterRosterMonitor.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/WriterRosterWitnessChecks.java;src/test/java/co/surumene/simplecoins/domain/WriterRosterWitnessTest.java;tests/paper-runtime-writer-roster-probe/src/main/java/co/surumene/simplecoins/a01probe/RuntimeWriterRosterProbe.java;.github/workflows/paper-runtime-writer-roster-probe.yml;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryHopperOutputWriterProbe.java |

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

## Authorized meaning and bounded implementation
The confirmed A01 publication PI requires a stable complete writer universe and immediate revocation on an invalidated generation; the user delegated technical implementation decisions. Existing actual runtime roster probes establish only point-in-time plugin identity. Wire an actual Paper lifecycle monitor into the product: register before ServerLoad, capture the complete registered plugin set on the next main-thread tick, including disabled entries, exact instance identity, classloader identity and version/enabled state. An enable/disable notification after capture invalidates even if the final roster is identical; periodic main-thread registry observation also detects unnotified membership/identity drift. Never reset an invalidated baseline. Close and plugin teardown invalidate the witness; callbacks are inert after close. A real transition revokes the actual product fence and publication; if an armed generation was active request guarded shutdown. This evidence is registration continuity ONLY, not loaded-code byte integrity, whole writer closure, admission or a replacement for the existing static and dynamic hook audit. No new readiness or permission path is introduced.

Red: the local domain regression cannot compile without a roster witness. Green: exact identity equality, disabled entry change, removed/extra/replaced instance and loader, transition ABA, foreign state and close fail closed. Existing genuine isolated Paper roster probe verifies the product sees a real PluginDisableEvent even when the observer itself is disabled. The existing five writer identity and byte checks are retained. The native Hopper regression retains the exact two-coin physical transfer and native event oracle before sealing; it polls a bounded 200 ticks rather than assuming completion at tick 50. Timeout remains failure, with source/destination counts and event diagnostics. No protected-epoch oracle is relaxed.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-roster-continuity-model.json | @work/.cat-flow/cache/a01-roster-continuity-vectors.json | @work/.cat-flow/cache/a01-roster-continuity-queue.json | @repo:SimpleCoins/tests/cat/a01-writer-epoch-junit-binding.json | @work/.cat-flow/generated/A01RosterContinuityGeneratedTest.java | @work/.cat-flow/cache/a01-roster-continuity-obligations.json | — | false |
