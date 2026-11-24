---
cat_version: '0.2'
kind: work
id: simplecoins.trader-live
status: draft
process: simplecoins.paper.trader-live-issuance
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# Work: simplecoins.paper.trader-live-issuance

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/commit/f2701e59f5e840b9b4efebff9b6c1bd2c73c2df2 |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.paper.trader-live-issuance | simplecoins.paper.trader-live-issuance | simplecoins.accounting.memory-journal |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.trader-live.md;docs/cat/confirmed/trader-live-process.md;docs/cat/confirmed/trader-live-pi.md;docs/cat/confirmed/trader-live-tce.md;tests/cat/trader-live-junit-binding.json;src/main/java/co/surumene/simplecoins/domain/WanderingTraderDropPolicy.java;src/main/java/co/surumene/simplecoins/paper/TraderDeathIssuanceListener.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/TraderLiveDriver.java;src/test/java/co/surumene/simplecoins/domain/TraderLiveTest.java | src/main/java/co/surumene/simplecoins/domain/WanderingTraderDropPolicy.java;src/main/java/co/surumene/simplecoins/paper/TraderDeathIssuanceListener.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/TraderLiveDriver.java;src/test/java/co/surumene/simplecoins/domain/TraderLiveTest.java |

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
| @workspace/docs/cat/confirmed/trader-live-process.md | @workspace/docs/cat/confirmed/trader-live-pi.md | @workspace/docs/cat/confirmed/trader-live-tce.md | — | — | — | @work/.cat-flow/cache/trader-live-model.json | @work/.cat-flow/cache/trader-live-vectors.json | @work/.cat-flow/cache/trader-live-queue.json | @workspace/tests/cat/trader-live-junit-binding.json | @work/.cat-flow/generated/GeneratedTraderLiveTest.java | — | — | false |

決算及び通常仕訳が有効な状態で、キャンセルされていない行商人死亡だけを対象とする。抽選は03の設定を使用し、成功時のみ貨幣をドロップへ追加する。通常のItemSpawnEventで実際の貨幣発生を仕訳へ反映する。集計対象外ワールドでもゲーム上の抽選を止めない。全件決算で復旧可能な通常仕訳のため、死亡イベント毎の永続発行WALを必須としない。
