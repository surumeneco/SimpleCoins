---
cat_version: '0.2'
kind: work
id: simplecoins.a12.station-local-decision-implementation
status: draft
process: simplecoins.currency.station-result-local-decision
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/pull/532
decision_ref: user-chat:2026-11-18:A12-station-result-local-decision-approved
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

# A12 — 確定済みステーション結果取得の局所純粋判定

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/532 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/529 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.station-result-local-decision | simplecoins.a12.station-local-decision-implementation | simplecoins.currency.station-consumption-exclusion;simplecoins.settlement.default-deny |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.station-local-decision-implementation.md;Lifecycle/CAT/Reviews/simplecoins.a12.station-local-result-conditional.md;src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java;src/test/java/co/surumene/simplecoins/paper/A12StationResultDecisionDriver.java;src/test/java/co/surumene/simplecoins/paper/generated/*;tests/cat/a12-station-local-junit-binding.json;.github/workflows/a12-local-station-cat-junit.yml | src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java | src/test/java/co/surumene/simplecoins/paper/A12StationResultDecisionDriver.java;src/test/java/co/surumene/simplecoins/paper/generated/*;.github/workflows/a12-local-station-cat-junit.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| green-s0 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.GeneratedA12StationLocalCase270ea3584cb1Test && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.GeneratedA12StationLocalCase270ea3584cb1Test.xml {report}' | case.s0 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA12StationLocalCase270ea3584cb1Test.java | — | 600 |
| green-s1 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.GeneratedA12StationLocalCase34c89fbe779cTest && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.GeneratedA12StationLocalCase34c89fbe779cTest.xml {report}' | case.s1 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA12StationLocalCase34c89fbe779cTest.java | — | 600 |
| green-s2 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.GeneratedA12StationLocalCasee292a6c220f2Test && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.GeneratedA12StationLocalCasee292a6c220f2Test.xml {report}' | case.s2 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA12StationLocalCasee292a6c220f2Test.java | — | 600 |
| green-s3 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.GeneratedA12StationLocalCase123a32c34595Test && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.GeneratedA12StationLocalCase123a32c34595Test.xml {report}' | case.s3 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA12StationLocalCase123a32c34595Test.java | — | 600 |

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
| @repo:SimpleCoins/docs/cat/confirmed/a12-station-local-decision-process.md | @repo:SimpleCoins/docs/cat/confirmed/a12-station-local-decision-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a12-station-local-decision-tce.md | — | — | — | @work/.cat-flow/cache/a12-station-local-model.json | @work/.cat-flow/cache/a12-station-local-vectors.json | @work/.cat-flow/cache/a12-station-local-queue.json | @repo:SimpleCoins/tests/cat/a12-station-local-junit-binding.json | @work/.cat-flow/generated/A12StationLocalGeneratedTest.java | @work/.cat-flow/cache/a12-station-local-obligations.json | — | false |

## Scope preservation

The confirmed Process covers only a pure function of three pre-normalized Boolean inputs. The production Java method returns an additional cancellation request. The test driver calls that actual production method and observes its returned decision; it does not recalculate the expected CAT output. Product Paper event input normalization and event cancellation remain independently verified by Paper probes and are NOT established by this pure local TestModel.

No artificial Red is created: the Paper handler already existed before extracting the pure function, and the original inline behavior already matched the four-case truth table. Execute actual source-derived JUnit Green and CAT Queue receipts. The global open-world A12, all other asset consumption paths, G0/G3, admission, release and authenticated JE/BE tests remain unresolved.
