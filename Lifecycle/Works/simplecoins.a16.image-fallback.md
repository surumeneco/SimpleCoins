---
cat_version: '0.2'
kind: work
id: simplecoins.a16.image-fallback
status: draft
process: simplecoins.presentation.currency-image-selection
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A16-image-fallback-approved
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

# A16 — 表示名のみ変更時の額面別内蔵標準画像フォールバック

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.presentation.currency-image-selection | simplecoins.a16.image-fallback | simplecoins.v2.currency-pack-generation |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2; CAT-TDD JUnit renderer (`develop` fd3f5c7); JUnit 5; JE/BE pack archives |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a16.image-fallback.md;docs/cat/confirmed/a16-image-fallback-*.md;.github/workflows/a16-image-fallback-confirmed.yml;src/main/java/co/surumene/simplecoins/domain/JavaCurrencyPack.java;src/test/java/co/surumene/simplecoins/domain/JavaCurrencyPackTest.java;src/test/java/co/surumene/simplecoins/domain/A16ImageSelectionDriver.java;src/test/java/co/surumene/simplecoins/domain/generated/*;.github/workflows/a16-cat-junit-proof.yml | src/main/java/co/surumene/simplecoins/domain/JavaCurrencyPack.java | src/test/java/co/surumene/simplecoins/domain/JavaCurrencyPackTest.java;src/test/java/co/surumene/simplecoins/domain/A16ImageSelectionDriver.java;src/test/java/co/surumene/simplecoins/domain/generated/*;.github/workflows/a16-cat-junit-proof.yml |

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
| @repo:SimpleCoins/docs/cat/confirmed/a16-image-fallback-process.md | @repo:SimpleCoins/docs/cat/confirmed/a16-image-fallback-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a16-image-fallback-tce.md | — | — | — | @work/.cat-flow/cache/a16-image-fallback-model.json | @work/.cat-flow/cache/a16-image-fallback-vectors.json | @work/.cat-flow/cache/a16-image-fallback-queue.json | — | — | @work/.cat-flow/cache/a16-image-fallback-obligations.json | — | false |

## Acceptance

- Display name alone changes without matching PNG: use denomination-specific built-in standard image and generate JE/BE candidate archives successfully; do not create an unwanted `New Name.png` file.
- Exactly one matching valid PNG: use it. Multiple normalized matches or malformed candidate: reject without replacing the archives with an unknown candidate or altering existing images.
- Existing denomination-fixed PDC/item_model and the independent default-deny admission remain unaffected.
- CAT-TDD upstream PR #101/#102 implemented a genuine bounded JUnit renderer. Four independently selected source-derived model cases have been emitted into the `generated/` test namespace, with a separate fixture driver that inspects actual JE+BE bytes rather than guessing the expected outcome. The generated tests are external verification artifacts, **not** evidence that `queue-state` has advanced or that all normative Red/Green and implementation-conformance Gates passed.
- Real JUnit CI must observe `case.s3` expected Red on unchanged pre-PR product code, then Green with the proposed source, and all four cases with `gradle test`. Preserve the exact generated test source and all historical model/vectors and decision provenance. Do not modify generated oracles to satisfy implementation.
- Run formal CAT Queue state transitions and script-owned Gates separately; pre-existing cases s0–s2 already pass pre-change, so do not fake expected Red failures or improperly mark them done. Deployment/admission remain denied.
