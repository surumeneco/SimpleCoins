---
cat_version: '0.2'
kind: work
id: simplecoins.a01.runtime-settlement-handoff
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010
  - https://github.com/surumeneco/SimpleCoins/pull/637
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.atomic-sealed-publication
  - simplecoins.a01.saved-staged-publish-bridge
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01/A11 — atomic protected startup to normal journal-backed accounting

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/pull/637 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.runtime-settlement-handoff | simplecoins.a01.atomic-sealed-publication;simplecoins.a01.saved-staged-publish-bridge;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit; real saved NBT and durable NormalMutationJournal; exclusive epoch and immutable accounting snapshot |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.runtime-settlement-handoff.md;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/paper/PaperProtectedSettlementService.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/SettlementAdmissionListener.java;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeChecks.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeTest.java;.github/workflows/a01-atomic-sealed-publication.yml;tests/paper-protected-settlement-runtime/**;.github/workflows/paper-protected-settlement-runtime.yml | src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/paper/PaperProtectedSettlementService.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/SettlementAdmissionListener.java;src/main/java/co/surumene/simplecoins/domain/SettlementProtectedPublisher.java | src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeChecks.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeTest.java;.github/workflows/a01-atomic-sealed-publication.yml;tests/paper-protected-settlement-runtime/**;.github/workflows/paper-protected-settlement-runtime.yml |

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

## Technical scope under approved A01 and Drive 02B

Bind one real protected startup-to-normal session to both the independent guard permit and the Paper login listener. The primary-thread lifecycle owner accepts only a drained independently audited seal, starts a single bounded off-main immutable source stage and genuine journal checkpoint, and polls without blocking. The same seal remains closed until an exact one-shot prepared result and final external A01 evidence are consumed on primary. Timeout, stop, incomplete evidence, stale token, source failure or tracker loss deny admission and fault the fence. The idle bootstrap remains disarmed; this work does not prove all writer coverage or install a full source collector. No new semantic authority or bypass is introduced. The isolated Paper probe uses actual detached saved playerdata, worker/primary thread handoff, verifies the real product remains denied, and revokes the fixture permit on close.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-runtime-handoff-model.json | @work/.cat-flow/cache/a01-runtime-handoff-vectors.json | @work/.cat-flow/cache/a01-runtime-handoff-queue.json | @repo:SimpleCoins/tests/cat/a01-writer-epoch-junit-binding.json | @work/.cat-flow/generated/A01RuntimeHandoffGeneratedTest.java | @work/.cat-flow/cache/a01-runtime-handoff-obligations.json | — | false |
