---
cat_version: '0.2'
kind: work
id: simplecoins.a01.writer-exception-failclosed
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/624
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.mainhand-equipment-agent-writer
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 — Writer Agentの異常終了を同一製品世代へfail-closed通知

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/624 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.writer-exception-failclosed | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25 ClassFile API; Paper 26.2; JUnit 5; standalone JVM Agent |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.writer-exception-failclosed.md;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/test/java/co/surumene/simplecoins/writeragent/AgentExceptionalExitMain.java;writer-fence-agent/src/test/java/co/surumene/simplecoins/writeragent/NativeSaveFixture.java;.github/workflows/a01-native-save-agent-bridge.yml | writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java | writer-fence-agent/src/test/java/co/surumene/simplecoins/writeragent/AgentExceptionalExitMain.java;writer-fence-agent/src/test/java/co/surumene/simplecoins/writeragent/NativeSaveFixture.java;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Confirmed contract and implementation boundary

The already-Confirmed A01 Process/PI/TCE require any exceptional writer completion to irrevocably invalidate the protected accounting generation. Existing Agent interception calls bootstrap before a native writer and after a normal void return, but an escaping JVM Throwable can bypass normal return and leave the bootstrap or product generation apparently valid. This Work is a correction of that existing contract; no new gameplay semantics or authority is introduced.

Wrap only the seven already-confirmed exact writer families with a catch-all exceptional exit that signals the **same** bootstrap hook and product fence, then rethrows the **original Throwable**. Preserve the target method's existing user exception handlers: an exception handled *inside* the method is not an escaping exceptional exit. On an escaping Throwable, the bridge transitions to FAULTED and invokes its product invalidation callback, so a Seal cannot be considered drained/valid even when prior writer entries remain outstanding. Never turn a failure into a normal return or suppress the original failure. Fail closed even while passive, and preserve successful normal-return behavior for all existing Paper/WWC/WWW hooks.

TDD requires a Java25 actual transformed fixture in a separate JVM proving NMS writer throws through CraftPlayer, retains its exception identity, and invalidates both the bootstrap and cross-loader product generation. A direct prior-version expected-reason Red and updated Agent Green are distinct checks. CI shall also run existing Java25 bytecode, distributed WWC/WWW and real Paper26.2 smoke regressions. Neither this limited hook family nor Green tests establish the 24-JAR closed writer universe, A01 G0 atomic publication, G3 ACK or Admission. Product remains unarmed/passive, main/VPS/release untouched.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-exception-model.json | @work/.cat-flow/cache/a01-exception-vectors.json | @work/.cat-flow/cache/a01-exception-queue.json | — | — | @work/.cat-flow/cache/a01-exception-obligations.json | — | false |
