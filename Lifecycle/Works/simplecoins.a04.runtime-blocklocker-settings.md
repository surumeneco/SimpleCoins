---
cat_version: '0.2'
kind: work
id: simplecoins.a04.runtime-blocklocker-settings
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

# A04 — capture actual BlockLocker runtime protection settings

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
| simplecoins.blocklocker.saved-owner | simplecoins.a04.runtime-blocklocker-settings | simplecoins.blocklocker.saved-owner.pi.classify;simplecoins.blocklocker.saved-owner.tce.classify;simplecoins.settlement |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.runtime-blocklocker-settings.md;src/main/java/co/surumene/simplecoins/paper/PaperBlockLockerSettings.java;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerChestSigns.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservationsTest.java;.github/workflows/paper-blocklocker-115-live-smoke.yml;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperBlockLockerSettings.java;src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java;src/main/java/co/surumene/simplecoins/domain/SavedBlockLockerChestSigns.java | src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;src/test/java/co/surumene/simplecoins/domain/SavedBlockLockerTerrainObservationsTest.java;.github/workflows/paper-blocklocker-115-live-smoke.yml;.github/workflows/paper-wcl-services-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| real-use | not-run | — |

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-process.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a04-saved-blocklocker-owner-tce.md | — | — | — | @work/.cat-flow/cache/a04-runtime-settings-model.json | @work/.cat-flow/cache/a04-runtime-settings-vectors.json | @work/.cat-flow/cache/a04-runtime-settings-queue.json | @repo:SimpleCoins/tests/cat/a04-saved-owner-junit-binding.json | @work/.cat-flow/generated/A04RuntimeSettingsGeneratedTest.java | @work/.cat-flow/cache/a04-runtime-settings-obligations.json | — | false |

## Acceptance
- Reuse Confirmed A04, unchanged normative classification. Capture installed BlockLocker 1.15's configured protection types, live sign aliases and Everyone aliases on the Paper primary thread. For the deployed connectContainers=false, no expiration and no extra protectables configuration, pass immutable captured settings into the existing SAVED terrain reader and combined off-main stage. No hard-coded chest-only list or English-only assumption for installed BlockLocker.
- Missing, incompatible, ambiguous or unsupported provider observations fail; do not substitute guessed FREE or publish anything. Actual server config and version: https://github.com/surumeneco/MineCraftXPlayServer/blob/develop/plugins/BlockLocker/config.yml ; public APIs: https://github.com/rutgerkok/BlockLocker/tree/v1.15/src/main/java/nl/rutgerkok/blocklocker .
- Real BlockLocker Paper probe asserts non-chest configured containers and actual aliases, including localized aliases; WCL/BlockLocker SAVED hybrid worker uses the captured settings, preserves known 55,000C/56,000C fixture totals and does not open admission. Actual startup publication still depends on complete ownership/BAN/tracking handoff. Canonical A04 PI evidence is separate from actual provider observations.
