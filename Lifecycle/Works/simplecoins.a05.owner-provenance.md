---
cat_version: '0.2'
kind: work
id: simplecoins.a05.owner-provenance
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

# A05 — WCL関係者証拠の未公開所有候補への変換

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-pi.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-wcl-provider-contract.md |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.owner-provenance | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.owner-provenance.md;src/main/java/co/surumene/simplecoins/domain/WclOwnerEvidence.java;src/test/java/co/surumene/simplecoins/domain/WclOwnerEvidenceTest.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;src/test/java/co/surumene/simplecoins/paper/PaperWonderfulSavedCandidateOwnerTest.java | src/main/java/co/surumene/simplecoins/domain/WclOwnerEvidence.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java | src/test/java/co/surumene/simplecoins/domain/WclOwnerEvidenceTest.java;src/test/java/co/surumene/simplecoins/paper/PaperWonderfulSavedCandidateOwnerTest.java |

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

## Approved source and implementation scope

- Confirmed 02A/A05: priority is WWC follow-target/WWW commander → tame owner → Free; present BAN and playerdata classification belongs to upper settlement. An unverified source or absent relationship key alone cannot prove Free.
- Verified shipped WCL provider relationship keys: WWC `follow_target`; WWW `commander`, `owner`. Unknown provider/key is not evidence. Only complete and independently verified reader results may be used.
- Return a **local** outcome (owner UUID / explicitly verified Free / HOLD), never a published P/F/D balance. Positive commander outranks owner. An independently verified tame owner can supply WWC's fallback; missing fallback evidence must HOLD. For WWW, contradictory WCL owner versus independently verified vanilla tame owner must HOLD.
- Connect this evidence qualification to existing unpublished saved WCL Candidate without changing its physical slots, amounts or provenance. The caller must still verify source generation, BAN, playerdata, LIVE/SAVED dedup and all writer exclusion.
- Tests: owner priority, positive WWW fallback, WWC follow-target, UNKNOWN→HOLD, explicit absent→Free, conflicting owners→HOLD, unsupported keys→reject, invalid provider proof→HOLD. Direct Java execution and JUnit CI are independent of formal CAT implementation conformance; do not release G0/G3/admission.
