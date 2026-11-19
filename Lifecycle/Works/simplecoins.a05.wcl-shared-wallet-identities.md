---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wcl-shared-wallet-identities
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md
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

# A05 — WCL収納の財布UUID同一性を上位決算世代と共有

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/pull/540 |
| https://github.com/surumeneco/SimpleCoins/pull/542 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wcl-shared-wallet-identities | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| Paper |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wcl-shared-wallet-identities.md;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSnapshotAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSnapshotAssets.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Acceptance and safety boundary

- Use the user-approved 02A/02B and A05 Process/PI/TCE requirement of **one physical wallet UUID counted once** across all selected sources. The pure TCE s0–s7 decision stays unchanged.
- Retain the already verified `StoredItemValue.value` nested money parser; add caller-owned `Set<UUID>` sharing for each WCL snapshot and saved-chunk collection, so duplicated wallet IDs across distinct WCL slots, Entity providers or an eventual vanilla/LIVE saved source cannot silently become two balances. A missing/invalid wallet should still fail as before.
- Callers must supply one fresh wallet identity set for an explicitly selected pending accounting generation, including other saved/live sources. When one read fails, the entire generation and its mutated set must be discarded. No claim that a caller actually enumerated all sources or used a correct world/writer epoch.
- Preserve the existing single-source valuation method only as explicitly **local and non-authoritative** (for diagnostics). The new saved-chunk collector requires a caller-supplied shared wallet set rather than secretly allocating a new one per holder.
- Actual official Paper 26.2 with shipped current WWC/WWW/WCL/WGL probes a real PDC wallet Bundle with 7,000C: two separate provider snapshots containing the same wallet must be rejected by the shared context (while a new independent context can value a real non-duplicate wallet). Existing 25,000C saved source proof, no far chunk load and unchanged content witness continue to pass.
- No novel TCE is promoted, formal A05 Implementation Conformance remains inconclusive until all obligations are proven. The bridge does not publish or admit players, resolve owner/BAN, freeze writers, close global source identity, persist grants, or touch `main`/VPS.
