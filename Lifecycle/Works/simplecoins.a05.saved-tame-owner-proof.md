---
cat_version: '0.2'
kind: work
id: simplecoins.a05.saved-tame-owner-proof
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

# A05 — SAVED個体から独立した飼い主不在／存在証拠を取得する

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/548 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/a05-wcl-consumer-process.md |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.saved-tame-owner-proof | simplecoins.settlement;simplecoins.world-source-discovery |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.saved-tame-owner-proof.md;src/main/java/co/surumene/simplecoins/domain/SavedEntityOwner.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java;src/test/java/co/surumene/simplecoins/domain/SavedEntityOwnerTest.java;src/test/java/co/surumene/simplecoins/paper/PaperWonderfulSavedCandidateOwnerTest.java | src/main/java/co/surumene/simplecoins/domain/SavedEntityOwner.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulSavedChunkCandidates.java | src/test/java/co/surumene/simplecoins/domain/SavedEntityOwnerTest.java;src/test/java/co/surumene/simplecoins/paper/PaperWonderfulSavedCandidateOwnerTest.java |

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

## Technical boundary and acceptance

- No new owner preference or currency rule: use Confirmed 02A/A05 and the already merged #548 local WCL `OWNER/FREE/HOLD` qualification.
- For a caller-selected verified SAVED Entity chunk, locate the exact entity UUID **once**, verify the source species expected by WWC (Chicken) or WWW (Wolf), and independently resolve the *vanilla tame-owner* observation. A complete normal Chicken has no vanilla tame-owner capability: verified-absent. A Wolf's saved Owner UUID is positive evidence only if canonical 4-int and well formed; a truly absent Owner field in an otherwise verified Wolf is verified-absent.
- Unknown entity, duplicate UUID, bad saved schema, target/species mismatch must raise IOException; they cannot silently become verified-absent or FREE. This per-chunk proof cannot claim file authenticity, unchanged Anvil witness, WCL reader completeness or a frozen writer generation.
- Existing saved WCL Candidate may combine this positive saved vanilla observation with independently verified WCL relationships via #548. WCL relationship precedence, amount, physical key and other code behavior remain unchanged; conflict must yield HOLD. Higher-level BAN/playerdata and global writer exclusion remain unimplemented.
- New tests must demonstrate previously missing source-to-owner mapping as a genuine Red before minimal implementation, then verify zero/positive, mismatch, duplicate and malformed source behavior by JUnit; when the final feature HEAD passes Domain/Phase6/Paper CI it may be integrated to develop. Do not claim formal A05 Implementation Conformance, G0/G3 or Admission.
