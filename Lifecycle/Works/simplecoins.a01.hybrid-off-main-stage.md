---
cat_version: '0.2'
kind: work
id: simplecoins.a01.hybrid-off-main-stage
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
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.hybrid-off-main-stage | simplecoins.a01.atomic-sealed-publication;simplecoins.a01.saved-staged-publish-bridge;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit; real saved NBT and durable NormalMutationJournal; exclusive epoch and immutable accounting snapshot |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.hybrid-off-main-stage.md;src/main/java/co/surumene/simplecoins/domain/SettlementReadDispatch.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java;src/test/java/co/surumene/simplecoins/domain/SettlementReadDispatchChecks.java;src/test/java/co/surumene/simplecoins/domain/SettlementReadDispatchTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/domain/SettlementReadDispatch.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java | src/test/java/co/surumene/simplecoins/domain/SettlementReadDispatchChecks.java;src/test/java/co/surumene/simplecoins/domain/SettlementReadDispatchTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

Use the actual one-shot combined SAVED/LIVE BlockLocker/WCL/BAN scan as an off-primary stage. Keep source discovery, fingerprints, Anvil/NBT traversal and source-scoped owner evidence on worker; route Paper world identity, provider reads, LIVE ownership and final WCL readback to the actual primary thread with a single outstanding bounded request. Preserve all source and wallet identity checks and the synchronous compatibility path. Cancel a timed-out or interrupted request, discard the whole capture, and never publish stale or partially classified values. Limit per-dispatch live ownership batches and use immutable observations; no worker may wait on a primary callback while the primary waits on that worker. The isolated test is not an installed-writer closure certificate.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-hybrid-off-main-model.json | @work/.cat-flow/cache/a01-hybrid-off-main-vectors.json | @work/.cat-flow/cache/a01-hybrid-off-main-queue.json | @repo:SimpleCoins/tests/cat/a01-writer-epoch-junit-binding.json | @work/.cat-flow/generated/A01HybridOffMainGeneratedTest.java | @work/.cat-flow/cache/a01-hybrid-off-main-obligations.json | — | false |
