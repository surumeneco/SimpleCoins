---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-owner-candidate-bridge
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

# A04 SAVED BlockLocker — locally observed evidence to unpublished verdict

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/563 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-owner-candidate-bridge | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.blocklocker.live-protection |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-owner-candidate-bridge.md;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerOwnerCandidate.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerOwnerCandidateTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java;.github/workflows/paper-blocklocker-115-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerOwnerCandidate.java | src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerOwnerCandidateTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedProfileAnvilFixtureTest.java |

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

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-process.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-tce.md | — | — | — | @work/.cat-flow/cache/a04-saved-owner-model.json | @work/.cat-flow/cache/a04-saved-owner-vectors.json | @work/.cat-flow/cache/a04-saved-owner-queue.json | @repo:SimpleCoins/tests/cat/a04-saved-owner-junit-binding.json | @work/.cat-flow/generated/A04SavedOwnerGeneratedTest.java | @work/.cat-flow/cache/a04-saved-owner-obligations.json | — | false |

## Acceptance

- Consume the read-only SAVED physical attachment observation from PR #568 and effective multi-sign profile summary from PR #569. Map only observed positive protection, main UUID and Everyone presence/absence into the user-confirmed A04 10-input PI/TCE classifier; preserve the precedence that verified Everyone yields FREE and contradictory/unknown inputs HOLD.
- The four independent external conditions—selected saved source, complete applicable-sign scan, physical storage deduplication, and locally coherent saved generation—are **not** inferable from the local sign data. Require separately verified witnesses from the caller, default all false; never silently equate locally read NBT with all-writer source epoch proof. No global generation publishing or admission.
- Explicitly distinguish proven non-protection from empty sign observations: empty signs alone HOLD, and FREE only with independently verified non-protection plus all four required witness flags. If protection observed and non-protection concurrently asserted, HOLD. Main UUID conflict holds even when Everyone otherwise would appear effective; an unknown main owner with effective Everyone can yield unpublished FREE only when every source predicate is independently met.
- Return only unpublished candidate verdict and a UUID attached solely to OWNER_UNPUBLISHED; never publish a snapshot, perform P/D BAN classification or write the world. The resulting diagnostics are bounded/local and do not satisfy the whole A04 implementation conformance or G0 writer exclusion.
- Red first in one class-focused JUnit; Green with minimal pure composition of existing product primitives; add paper-saved Anvil fixture confirmation that unverified generation never produces a positive candidate.
