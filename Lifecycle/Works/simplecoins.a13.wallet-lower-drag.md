---
cat_version: '0.2'
kind: work
id: simplecoins.a13.wallet-lower-drag
status: draft
process: simplecoins.wallet.drag-isolation
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: user-chat:2026-11-18:A13-lower-only-drag-correction-approved
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

# A13 — 財布GUIの下段ドラッグ通常操作を回復

## Evidence
| URI |
| --- |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.wallet.drag-isolation | simplecoins.a13.wallet-lower-drag | simplecoins.wallet.balance;simplecoins.wallet.paper-gui |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a13.wallet-lower-drag.md;Lifecycle/CAT/Reviews/simplecoins.a13.wallet-lower-drag-conditional.md;docs/cat/confirmed/a13-wallet-drag-*.md;src/main/java/co/surumene/simplecoins/paper/WalletMenu.java;src/main/java/co/surumene/simplecoins/paper/WalletDragPolicy.java;src/test/java/co/surumene/simplecoins/paper/WalletDragPolicyTest.java;src/test/java/co/surumene/simplecoins/paper/generated/*;src/test/java/co/surumene/simplecoins/paper/A13WalletDragDriver.java;tests/cat/a13-wallet-drag-junit-binding.json;.github/workflows/a13-wallet-drag.yml | src/main/java/co/surumene/simplecoins/paper/WalletMenu.java;src/main/java/co/surumene/simplecoins/paper/WalletDragPolicy.java | src/test/java/co/surumene/simplecoins/paper/WalletDragPolicyTest.java;src/test/java/co/surumene/simplecoins/paper/generated/*;src/test/java/co/surumene/simplecoins/paper/A13WalletDragDriver.java;tests/cat/a13-wallet-drag-junit-binding.json;.github/workflows/a13-wallet-drag.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| green-s0 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCase270ea3584cb1Test && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCase270ea3584cb1Test.xml {report}' | case.s0 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA13WalletDragCase270ea3584cb1Test.java | — | 600 |
| green-s1 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCase34c89fbe779cTest && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCase34c89fbe779cTest.xml {report}' | case.s1 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA13WalletDragCase34c89fbe779cTest.java | — | 600 |
| green-s2 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCasee292a6c220f2Test && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCasee292a6c220f2Test.xml {report}' | case.s2 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA13WalletDragCasee292a6c220f2Test.java | — | 600 |
| green-s3 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCase123a32c34595Test && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCase123a32c34595Test.xml {report}' | case.s3 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA13WalletDragCase123a32c34595Test.java | — | 600 |
| green-s4 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCased9873f54793dTest && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCased9873f54793dTest.xml {report}' | case.s4 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA13WalletDragCased9873f54793dTest.java | — | 600 |
| green-s5 | green | junit | @repo:SimpleCoins | bash -c 'gradle --no-daemon :test --tests co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCase5df8ddaf0846Test && cp build/test-results/test/TEST-co.surumene.simplecoins.paper.generated.GeneratedA13WalletDragCase5df8ddaf0846Test.xml {report}' | case.s5 | src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA13WalletDragCase5df8ddaf0846Test.java | — | 600 |

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
| @repo:SimpleCoins/docs/cat/confirmed/a13-wallet-drag-process.md | @repo:SimpleCoins/docs/cat/confirmed/a13-wallet-drag-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a13-wallet-drag-tce.md | — | — | — | @work/.cat-flow/cache/a13-wallet-drag-model.json | @work/.cat-flow/cache/a13-wallet-drag-vectors.json | @work/.cat-flow/cache/a13-wallet-drag-queue.json | @repo:SimpleCoins/tests/cat/a13-wallet-drag-junit-binding.json | @repo:SimpleCoins/src/test/java/co/surumene/simplecoins/paper/generated/GeneratedA13WalletDragTest.java | @work/.cat-flow/cache/a13-wallet-drag-obligations.json | — | false |

## Acceptance

- 下段だけの普通のドラッグは既存のキャンセルを尊重したうえで通す。
- 上段、財布の保持元、ドラッグ元が財布、セッション識別不良は拒否し、金銭状態を変えない。
- Event view の raw -> local 変換を使用して財布ホットバー枠を同定する。
- JE/BE全ドラッグ経路、耐クラッシュ決算G0/G3、上段ドラッグでの入出金実装は別の未達項目として残す。
