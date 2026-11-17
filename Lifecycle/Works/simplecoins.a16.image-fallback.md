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
| Java 25; JUnit 5; Paper 26.2; pinned CAT 896cae83; JE/BE pack archives |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a16.image-fallback.md;docs/cat/confirmed/a16-image-fallback-*.md;.github/workflows/a16-image-fallback-confirmed.yml;src/main/java/co/surumene/simplecoins/domain/JavaCurrencyPack.java;src/test/java/co/surumene/simplecoins/domain/JavaCurrencyPackTest.java | src/main/java/co/surumene/simplecoins/domain/JavaCurrencyPack.java | src/test/java/co/surumene/simplecoins/domain/JavaCurrencyPackTest.java |

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
- Deterministic CAT model/vectors and independently observed Red/Green evidence; code edits only when the selected canonical Work Gate grants permission. Java renderer availability/limitation must be represented honestly rather than faking normative `passed`.
