---
cat_version: '0.2'
kind: work
id: simplecoins.a05.live-tame-owner-proof
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

# A05 — LIVE WWC/WWWの独立飼い主根拠を未公開候補へ結ぶ

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
| simplecoins.external-storage-provider-integration | simplecoins.a05.live-tame-owner-proof | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.live-tame-owner-proof.md;src/main/java/co/surumene/simplecoins/domain/WclLiveTameObservation.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveOwnerEvidence.java;src/test/java/co/surumene/simplecoins/domain/WclLiveTameObservationTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java | src/main/java/co/surumene/simplecoins/domain/WclLiveTameObservation.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveOwnerEvidence.java | src/test/java/co/surumene/simplecoins/domain/WclLiveTameObservationTest.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java |

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
- Use the already Confirmed A05 Process/PI/TCE and Drive 02A. No change to commander → tame owner → FREE precedence, BAN, playerdata or asset value.
- Evaluate a *caller-selected, independently authoritative* loaded Entity on the main thread, only after the real WCL registration and successful `readLoaded` snapshot identity checks.
- WWC: verified regular Chicken has no Bukkit vanilla tame-owner capability; absent WCL follow-target may then be explicitly FREE. WWW: a real Wolf provides verified tame owner when tamed with non-null owner, or verified absence when untamed with null owner. A contradictory tamed/owner pair or missing evidence is UNKNOWN, never an automatic FREE.
- Require the source Entity to remain the same valid Bukkit object before and after its WCL read. Species/provider mismatch or unsuccessful storage read means HOLD. Never infer a verified owner from a synthetic map or an unknown Entity.
- Output only the local OWNER/FREE/HOLD decision (and evidence status); no settlement publication, same-epoch guarantee, BAN/playerdata decision, G3, Admission or configuration changes. No admission is opened.
- Tests: Chicken explicit absence vs unverified; Wolf positive / absent / contradictory; WCL owner conflict and commander precedence inherited from existing WclOwnerEvidenceTest. Java direct Red/Green and repository JUnit/CI must be kept separate from formal CAT Implementation Conformance.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-process.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a05-wcl-consumer-tce.md | — | — | — | @work/.cat-flow/cache/a05-live-owner-model.json | @work/.cat-flow/cache/a05-live-owner-vectors.json | @work/.cat-flow/cache/a05-live-owner-queue.json | @repo:SimpleCoins/tests/cat/a05-wcl-consumer-junit-binding.json | @work/.cat-flow/generated/A05LiveOwnerGeneratedTest.java | @work/.cat-flow/cache/a05-live-owner-obligations.json | — | false |

## Formal boundary
Existing Confirmed s0–s7 describe local verdicts. This independent technical bridge cannot prove external WCL observation, all-writer epoch, global identity, ownership BAN/playerdata, or the implementation of the complete Process. A Java/JUnit Green must not be relabeled as formal implementation-conformance passed.
