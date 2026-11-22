---
cat_version: '0.2'
kind: work
id: simplecoins.a01.shared-writer-epoch-fence
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7077640456
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

# A01 — 共有writer入場・排出の製品用同期プリミティブ

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010 |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7077640456 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.shared-writer-epoch-fence | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.shared-writer-epoch-fence.md;src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/test/java/co/surumene/simplecoins/domain/WriterEpochFenceTest.java;.github/workflows/a01-shared-writer-epoch-fence.yml | src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java | src/test/java/co/surumene/simplecoins/domain/WriterEpochFenceTest.java;.github/workflows/a01-shared-writer-epoch-fence.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

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

## Authorized scope and limitations

Source of meaning is Confirmed A01/G0 (Process/PI/TCE) and the user's approved Java 25 launch-time Agent technical candidate. This Work adds **one independently testable, shared product synchronization primitive**: initial default-deny, a linearizable close-vs-root-entry decision, tracking admitted synchronous nested writers, complete normal return drain, generation-bound seals, and irreversible fail-closed behavior on exceptional/mismatched completion. It does not add new CAT semantics.

Do not connect it to G0 publication, G3 ACK, or Admission based on this Work. This class cannot establish that all Paper native/24-JAR/async/disk writers are actually hooked. Coverage, bytecode transformation success, mutation reachability, actual saved generation, Journal handoff, atomic publication, and durability require **separate independent runtime evidence**. The user is not being asked to relax the already-Confirmed guarantees. Presently this primitive may be integrated as a non-operational partial product component only, while default-deny stays in force.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-writer-fence-model.json | @work/.cat-flow/cache/a01-writer-fence-vectors.json | @work/.cat-flow/cache/a01-writer-fence-queue.json | — | — | @work/.cat-flow/cache/a01-writer-fence-obligations.json | — | false |
