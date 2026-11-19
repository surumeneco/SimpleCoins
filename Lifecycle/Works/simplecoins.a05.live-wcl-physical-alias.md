---
cat_version: '0.2'
kind: work
id: simplecoins.a05.live-wcl-physical-alias
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/544
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

# A05 — LIVE装備とWCLミラー武器の会計用衝突キー統一

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/544 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/44459555349 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.live-wcl-physical-alias | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.live-wcl-physical-alias.md;src/main/java/co/surumene/simplecoins/domain/SettlementAccumulator.java;src/test/java/co/surumene/simplecoins/domain/SettlementAccumulatorTest.java;src/test/java/co/surumene/simplecoins/domain/LiveWclPhysicalAliasTest.java;src/main/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentity.java | src/main/java/co/surumene/simplecoins/domain/SettlementAccumulator.java;src/main/java/co/surumene/simplecoins/domain/WclPhysicalSlotIdentity.java | src/test/java/co/surumene/simplecoins/domain/SettlementAccumulatorTest.java;src/test/java/co/surumene/simplecoins/domain/LiveWclPhysicalAliasTest.java |

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

## Scope and acceptance

- The user-approved A05 confirmed Process/PI/TCE and 02A/02B require one physical economic asset to be counted once. This Work fixes an observed mismatch in the technical physical-key bridge, not the approved TCE s0–s7 or the accounting definition.
- Existing saved Entity staging and pinned WWW WCL weapon candidate use `entity/<UUID>/equipment/mainhand`; the actual Paper LIVE read uses `world/<world-id>/entity/<UUID>/equipment/mainhand`. Without canonicalization, both amount candidates can enter SettlementAccumulator and commit as distinct assets.
- Keep the genuine raw Paper LIVE source/provenance validation unchanged. At the accumulator's physical collision boundary only, recognize the authentic, world-matched and canonical Entity UUID live mainhand representation as the same collision identity as its saved/WCL counterpart. Wrong-world, malformed or unrelated slots are not guessed into another identity.
- Two independent sources with the same physical weapon must cause rejection of the generation, not sum or arbitrarily drop one value. A successful coalescing policy would separately need proof of same content, ownership, source generation and writer exclusion.
- Record an actual direct Red on the prior code, then Green with the smallest behavioral change, JDK25 JUnit and existing Domain/Phase6 CI. No production asset is modified. This does not wire WCL into the global settlement, prove all writer epochs or G0/G3, run new formal capability Gate or authorize admission.
