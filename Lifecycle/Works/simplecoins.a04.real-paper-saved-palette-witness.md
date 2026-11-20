---
cat_version: '0.2'
kind: work
id: simplecoins.a04.real-paper-saved-palette-witness
status: draft
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A04 — Actual Paper saved chest block/palette source identity proof

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/576 |
| https://github.com/surumeneco/SimpleCoins/pull/574 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.a04.real-paper-saved-palette-witness | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Official Paper 26.2 build 134; BlockLocker 1.15; Java25; JUnit; fixed CAT issue-work verifier |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.real-paper-saved-palette-witness.md;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;.github/workflows/paper-blocklocker-115-live-smoke.yml | — | src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;.github/workflows/paper-blocklocker-115-live-smoke.yml |

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

## Verification and limits

- In isolated Paper CI, obtain the genuine stopped-world Anvil REGION path and all selected Chunk NBT captured by the existing Paper/BlockLocker 1.15 fixture. Independently call WorldStorageDiscovery.discover on the actual disposable Paper world root and find its actual matching selected REGION SourceFile. Feed the selected saved Chunk and exact chest position to the product sourceScoped() classifier from #574/#576.
- Require positive real palette/chest/selected-source identity and UUID owner agreement for x=6 in the same observed fixture. For a controlled *mutated test-only copy* of that Chunk where chest BlockEntity remains but physical palette says stone, fail closed. Neither mutation nor test flags affect the genuine saved world.
- In order to enter the local unpublished owner index, the fixture supplies the four external proof flags as **explicit synthetic positive test inputs only**. These are NOT verified writer exclusivity or a valid G0 accounting generation; the original unknown-proof test must continue to HOLD. Keep statistics/Admission/Grant ACK blocked.
- Retain all previously passing saved-owner/Everyone/double-chest/other Paper restart markers, and independent Domain/Phase6 checks at final HEAD. This is issue-work environmental verification, not Confirmed normative upgrade, spec-test-sync suite replacement or full Implementation Conformance.
