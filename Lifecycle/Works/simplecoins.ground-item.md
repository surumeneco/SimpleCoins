---
cat_version: '0.2'
kind: work
id: simplecoins.ground-item
status: draft
process: simplecoins.accounting.ground-item
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# Work: simplecoins.accounting.ground-item

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/commit/f2701e59f5e840b9b4efebff9b6c1bd2c73c2df2 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.ground-item | simplecoins.accounting.ground-item | simplecoins.accounting.memory-journal |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.ground-item.md;docs/cat/confirmed/ground-item-process.md;docs/cat/confirmed/ground-item-pi.md;docs/cat/confirmed/ground-item-tce.md;tests/cat/ground-item-junit-binding.json;src/main/java/co/surumene/simplecoins/domain/GroundItemMovement.java;src/main/java/co/surumene/simplecoins/paper/PaperGroundItemAccounting.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/GroundItemMovementDriver.java;src/test/java/co/surumene/simplecoins/domain/GroundItemMovementTest.java | src/main/java/co/surumene/simplecoins/domain/GroundItemMovement.java;src/main/java/co/surumene/simplecoins/paper/PaperGroundItemAccounting.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/GroundItemMovementDriver.java;src/test/java/co/surumene/simplecoins/domain/GroundItemMovementTest.java |

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
| @workspace/docs/cat/confirmed/ground-item-process.md | @workspace/docs/cat/confirmed/ground-item-pi.md | @workspace/docs/cat/confirmed/ground-item-tce.md | — | — | — | @work/.cat-flow/cache/ground-item-model.json | @work/.cat-flow/cache/ground-item-vectors.json | @work/.cat-flow/cache/ground-item-queue.json | @workspace/tests/cat/ground-item-junit-binding.json | @work/.cat-flow/generated/GeneratedGroundItemMovementTest.java | — | — | false |

地面アイテム生成は集計対象ワールドのF増、消失はF減。プレイヤー投棄はP減（生成は別に観測し同tickの差分を一括適用）、回収はP増と対象ワールドならF減。非対象ワールドの地面は集計しないがプレイヤーは常時対象。キャンセル・未確定は反映しない。入れ子と財布を含む実価値、実回収枚数を用いる。Mob・ホッパー回収、死亡、容器、所有変更はこの部分契約の外側で、全体の追跡が完成するまでは入室を解除しない。
