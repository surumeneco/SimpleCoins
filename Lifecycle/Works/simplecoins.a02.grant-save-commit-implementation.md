---
cat_version: '0.2'
kind: work
id: simplecoins.a02.grant-save-commit-implementation
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: user-chat:2026-11-24:approved-revised-simplecoins-scope
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

# A02 — Confirmed PI/TCEを未公開の製品判定として実装

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.grant-save-commit-implementation | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.grant-save-commit-implementation.md;docs/cat/confirmed/a02-grant-delivery-process.md;docs/cat/confirmed/a02-grant-delivery-pi.md;docs/cat/confirmed/a02-grant-delivery-tce.md;src/main/java/co/surumene/simplecoins/domain/GrantDeliveryDecision.java;src/test/java/co/surumene/simplecoins/domain/A02GrantDeliveryDecisionDriver.java;src/test/java/co/surumene/simplecoins/domain/GrantDeliveryDecisionTest.java;src/test/java/co/surumene/simplecoins/domain/SimplifiedDecisionTest.java;tests/cat/a02-grant-delivery-junit-binding.json;.github/workflows/cat-a01-a02-contract-acceptance.yml | src/main/java/co/surumene/simplecoins/domain/GrantDeliveryDecision.java | src/test/java/co/surumene/simplecoins/domain/A02GrantDeliveryDecisionDriver.java;src/test/java/co/surumene/simplecoins/domain/GrantDeliveryDecisionTest.java;src/test/java/co/surumene/simplecoins/domain/SimplifiedDecisionTest.java;tests/cat/a02-grant-delivery-junit-binding.json;.github/workflows/cat-a01-a02-contract-acceptance.yml |

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

## Authorized meaning and bounded implementation
2026-11-24の改訂仕様に対応するConfirmed Process/PI/TCEを正本とする。既存の保護方式は互換アダプターに限定し、全writer閉包・強制fsync・全サーバー排他を独立した必須条件としない。局所判定の成功は実Paper上の観測、決算公開、予約配布、永続ACKや入室解放を証明しない。

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-grant-delivery-model.json | @work/.cat-flow/cache/a02-grant-delivery-vectors.json | @work/.cat-flow/cache/a02-grant-delivery-queue.json | @repo:SimpleCoins/tests/cat/a02-grant-delivery-junit-binding.json | @work/.cat-flow/generated/A02GrantDeliveryGeneratedTest.java | @work/.cat-flow/cache/a02-grant-delivery-obligations.json | — | false |
