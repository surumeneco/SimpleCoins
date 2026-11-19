---
cat_version: '0.2'
kind: work
id: simplecoins.a05.single-read-live-wcl-candidate
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

# A05 — LIVE WCLの額・物理別名・帰属根拠を単一の読取結果から未公開候補化する

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-pi.md |
| https://github.com/surumeneco/SimpleCoins/pull/548 |
| https://github.com/surumeneco/SimpleCoins/pull/549 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.single-read-live-wcl-candidate | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.single-read-live-wcl-candidate.md;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveOwnerEvidence.java;src/test/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidateTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveOwnerEvidence.java | src/test/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidateTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Approved source and local scope
- Reuse the approved 02A/02B and Confirmed A05 source. This does not introduce new monetary ownership, currency, BAN, playerdata, deduplication or publication semantics.
- For an independently selected, authoritative loaded WWC/WWW Entity, call the existing registered WCL readLoaded only **once**; from that exact successful provider snapshot derive the per-slot currency values, canonical WCL physical-slot alias keys, relationships and local OWNER/FREE/HOLD judgment. Do not obtain each value from separate reads.
- Reuse the existing shared physical-wallet UUID set (not a local-only set) and the exact WclPhysicalSlotIdentity mapping; duplicate currency identity and unknown provider slots must fail closed. On error, the whole caller generation and mutated shared identities must be discarded.
- Refactor PaperWonderfulLiveOwnerEvidence only enough to allow qualifying the already verified WCL snapshot; retain the existing public readLoaded contract and all source/entity authority checks. No bypass for incomplete/failed WCL registry or read.
- Unreadable or non-target WCL source is **not** zero or FREE: reject the candidate. Unknown owner is a distinct HOLD result and may not be published.
- Add direct JUnit contract tests and a real official Paper 26.2 probe using the actual distributed WWC loaded shulker 10000C; verify owner, slots, alias identity, duplicate physical-wallet rejection, and the non-target fail-closed case.
- Integration is a **read-only, unpublished, one-Entity candidate**. It is not an all-Entity discovery, source/writer-generation freeze, LIVE/SAVED selection, G0/G3/Admission or statistics publication.
- Preserve the existing A05 formal Process/Model references; report Implementation Conformance as inconclusive/not-run where the full external observation/generation contract cannot be proven.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-live-owner-model.json | @work/.cat-flow/cache/a05-live-owner-vectors.json | @work/.cat-flow/cache/a05-live-owner-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05LiveOwnerGeneratedTest.java | @work/.cat-flow/cache/a05-live-owner-obligations.json | — | false |

## Formal boundary
Existing Confirmed s0–s7 describe local verdicts. This independent technical bridge cannot prove external WCL observation, all-writer epoch, global identity, ownership BAN/playerdata, or the implementation of the complete Process. A Java/JUnit Green must not be relabeled as formal implementation-conformance passed.
