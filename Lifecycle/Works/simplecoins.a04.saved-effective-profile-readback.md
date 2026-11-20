---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-effective-profile-readback
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

# A04 SAVED BlockLocker — effective saved sign profile readback

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
| simplecoins.blocklocker.saved-owner | simplecoins.a04.saved-effective-profile-readback | simplecoins.settlement;simplecoins.world-source-discovery;simplecoins.blocklocker.live-protection |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-effective-profile-readback.md;src/main/java/co/surumene/simplecoins/domain/BlockLockerSavedProfileResolution.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedBlockLockerSignText.java;src/test/java/co/surumene/simplecoins/domain/BlockLockerSavedProfileResolutionTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedBlockLockerSignTextTest.java | src/main/java/co/surumene/simplecoins/domain/BlockLockerSavedProfileResolution.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedBlockLockerSignText.java | src/test/java/co/surumene/simplecoins/domain/BlockLockerSavedProfileResolutionTest.java;src/test/java/co/surumene/simplecoins/paper/PaperSavedBlockLockerSignTextTest.java |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a04-green-f0 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a04_junit.py {report} case.f0 | case.f0 | — | — | 300 |
| a04-green-f1 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a04_junit.py {report} case.f1 | case.f1 | — | — | 300 |
| a04-green-o0 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a04_junit.py {report} case.o0 | case.o0 | — | — | 300 |
| a04-green-h0 | green | junit | @repo:SimpleCoins | python3 tools/run_cat_a04_junit.py {report} case.h0 | case.h0 | — | — | 300 |

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
- Implement one sign's SAVED effective profile readback under the already Confirmed Process observation capability, comparing actual displayed text and PDC according to BlockLocker 1.15 SignParserImpl and ProfileFactoryImpl. PDC UUID without exact first-slot visible name match is not authoritative; valid back face profiles count. Missing/malformed data aborts.
- Legacy Text1-4 must remain plain text; modern front_text/back_text are Minecraft JSON components to decode through Paper's Adventure runtime. These per-sign observations are not proof of physical attachment or all applicable signs, nor of source generation.
- The existing CAT model/Queue checks only the confirmed 10 boolean verdict; a passing observer unit test does NOT establish implementation-conformance of all Process observation/action capabilities. Keep admission blocked and snapshot unpublished.
- The Confirmed 10 boolean evidence fields have exact truth-table f0, f1, o0, h0 semantics; the output is local un**published** FREE, OWNER, HOLD candidate only.
- Upstream collector must independently verify every claimed positive input against BlockLocker v1.15 saved geometry, PDC/display and locale, source identity and physical deduplication. This local verdict cannot attest those capabilities by itself.
- 0/unknown or inconsistent evidence never becomes positive FREE or OWNER. BAN/playerdata and G0 writer quiescence/global publishing and G3 are separate future stages; admission stays blocked.
- Source-derived current-item JUnit Red/Green, Queue state and official CAT conformance as required. No unowned production changes.
