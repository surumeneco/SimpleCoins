---
cat_version: '0.2'
kind: work
id: simplecoins.a01.real-startup-owner-resolution
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
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

# Startup settlement source with actual owner attribution

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a01.real-startup-owner-resolution | simplecoins.external-storage-provider-integration.pi.capture;simplecoins.settlement.writer-epoch-publication |

## Technologies
| Technology |
| --- |
| Java 25; Paper 26.2; WCL WWC WWW; BlockLocker 1.15 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.real-startup-owner-resolution.md;src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSnapshotAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/paper/PaperStartupSettlementSource.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java | src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedSettlementSource.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSnapshotAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/paper/PaperStartupSettlementSource.java | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-real-owner-model.json | @work/.cat-flow/cache/a05-real-owner-vectors.json | @work/.cat-flow/cache/a05-real-owner-queue.json | — | — | — | — | false |

## Acceptance
- Reuse the actual captured WCL snapshot for ownership of SAVED native equipment and extra storage; commander takes precedence over tame owner. Read provider once per selected saved chunk. Existing owner callbacks remain the fallback for non-WCL entities. Native mirror value and item bytes must agree and wallet UUIDs must be counted once.
- Capture selected LIVE worlds and live WCL on primary; classify their positive currency from actual source keys, live protection and captured WCL or vanilla tame owner. Resolve SAVED ordinary entity ownership from actual UUID and tame evidence, nonconfigured block storage as Free only after physical type verification. Use actual BlockLocker configuration and saved terrain/sign observations, not test fixture coordinates. Complete BAN evidence is supplied by the caller; never infer a missing source.
- The existing real isolated protected Paper worker test must use the same new production source assembler, not hardcoded owner UUIDs or nonprotection callbacks. Expect P=25,000, F=1,000, D=30,000, T=26,000 and no chunk load, with known actual providers and stable source. Retain existing negative tests.
- This source stage is an input for startMemory, not a complete normal-tracking implementation or a permit. Do not open admission or release until actual handoff prerequisites are satisfied.
