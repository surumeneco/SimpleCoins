---
cat_version: '0.2'
kind: work
id: simplecoins.a04.reject-unscoped-settlement-owner
status: draft
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-20:A04-saved-BlockLocker-CAT-approved
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

# A04 — 非source-scoped保存所有者Lookupの決算誤接続を拒否

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/573 |
| https://github.com/surumeneco/SimpleCoins/pull/574 |
| https://github.com/surumeneco/SimpleCoins/pull/577 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.a04.reject-unscoped-settlement-owner | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.reject-unscoped-settlement-owner.md;src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookupTest.java | src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java | src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerSettlementOwnerLookupTest.java |

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

## Acceptance
- Reuse the previously Confirmed A04 rule: a SAVED BlockLocker owner is a local, unpublished candidate only if the actual chosen REGION source, exact Chunk, BlockEntity and terrain palette agree; coordinate-only inference must not silently substitute for this observation.
- Preserve the public legacy SavedBlockLockerSettlementOwnerLookup.resolve(world,position) for already-verified bounded callers; also preserve all other generic BlockOwnerLookup implementations. Reject only passing that particular legacy adapter directly as the ownership callback of SettlementScanRunner.stage() or stageWithBanCoverage(), before any saved import. Require explicit sourceScoped() opt-in for this adapter.
- Expected-reason Red before changing production code: the source-unaware adapter is currently accepted by both entrypoints despite missing actual source/chunk binding. Green: both reject IOException, sourceScoped() still stages exactly the same previously tested local candidate and retains BAN revalidation.
- No global Snapshot publish, writer-entry control, admission, G3 durable ACK or new CAT Confirmed meaning. Passing this guard is not full A04 implementation conformance; avoid presenting it as G0 complete.
