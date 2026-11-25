---
cat_version: '0.2'
kind: work
id: simplecoins.a05.pending-founder-marker
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
project_entry_ref: https://drive.google.com/file/d/1RVbNpheNNWTaN0WYxZsO__fm5H-0_nZh/view
spec_status: confirmed
---

# Work: classify verified pending WWC natural founders

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/694 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/45571787174 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/45575478196 |
| https://github.com/surumeneco/MineCraftXPlayServer/commit/79e4afdbc530c9f8151c44bb390d06f7ff8fcf6e |

Pinned WWC 2.1.1 JAR blob 77831eafab4d0fa55ae76d03e4249f69f472d1cf and coordinator source confirm the LONG pending_natural_founder_seed before initialized storage. Existing classifier blob 5aecb0bebe273facc61eb06a5a52c78faac76e5e reproduced IOException in direct Java 21; candidate blob a38597160c46ff8efaf0651122f39e5e01196aaf passed 12 direct cases. No claim this is the exact prior intermittent BE failure. Existing A05 s1 semantics unchanged.

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.pending-founder-marker | simplecoins.settlement |

## Technologies
| Technology |
| --- |
| Java; JUnit; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | src/main/java/co/surumene/simplecoins/domain/WclLiveMarker.java;src/main/java/co/surumene/simplecoins/paper/PaperEntityAccounting.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveEntityDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;src/test/java/co/surumene/simplecoins/domain/WclLiveMarkerTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;Lifecycle/Works/simplecoins.a05.pending-founder-marker.md | src/main/java/co/surumene/simplecoins/domain/WclLiveMarker.java;src/main/java/co/surumene/simplecoins/paper/PaperEntityAccounting.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveEntityDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java | src/test/java/co/surumene/simplecoins/domain/WclLiveMarkerTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

Direct classifier reproduction and regressions, JUnit, real Paper workflow (including synthetic saved-NBT discriminators and normal BE product admission); these are not automatically formal implementation-conformance evidence.

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |

Canonical runtime fb3bd562 was staged and validated. Existing confirmed A05 source subset process-closure/model/model-conformance passed locally; complete product work closure and formal implementation conformance remain unverified. The existing generated PI Boolean driver does not observe the Java adapter directly.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/marker-model.json | @work/.cat-flow/cache/marker-vectors.json | @work/.cat-flow/cache/marker-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/MarkerGeneratedTest.java | @work/.cat-flow/cache/marker-obligations.json | — | false |

## Acceptance

Only a chicken with verified LONG pending seed and no other WWC/WWW marker is a known non-storage target. Unknown, malformed, contradictory and actual initialized evidence must retain existing classification. Verify startup LIVE/SAVED, runtime entity loading, saved typed/incorrect/partial cases, and combined normal product acceptance before merge. Do not weaken fail-closed checks or change approved monetary semantics.
