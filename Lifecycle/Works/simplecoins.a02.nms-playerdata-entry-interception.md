---
cat_version: '0.2'
kind: work
id: simplecoins.a02.nms-playerdata-entry-interception
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A02/G3 — Paper NMS PlayerDataStorage保存の別入口とAgent対応の実測

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/580 |
| https://github.com/surumeneco/SimpleCoins/pull/505 |
| https://github.com/surumeneco/SimpleCoins/pull/510 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.nms-playerdata-entry-interception | simplecoins.grants.paper-save-commit;simplecoins.settlement.writer-epoch-publication |

## Technologies
| Technology |
| --- |
| Java25 Class-File API agent/test-only bootstrap hook; real Paper26.2 build134; real InvSee++ JAR; playerdata NBT, fsync, abrupt halt and separate JVM |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.nms-playerdata-entry-interception.md;tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-nms-playerdata-entry-spike.yml | — | tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-nms-playerdata-entry-spike.yml |

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

## Verified question and bounded test plan

PR #580 established one genuine Paper `CraftPlayer.saveData()V` entrance that an isolated JVM Agent can block, but an alternate NMS `PlayerDataStorage.save(ServerPlayer)` is not captured by the same hook. Test **the installed real Paper runtime**, not a method-name guess or bytecode-only static scanner.

1. Start the established official Paper26.2+InvSee++ loopback, zero-online-player fixture. Persist 5000C authentic money, completed PDC receipt, unrelated STONE and exactly one **pending** durable grant. Wait for existing SimpleCoins saved-source discovery before creating playerdata, and retain the native Agent `CraftPlayer.saveData` hook closed after a fresh verified saved generation.
2. **Failing counterexample**: Reach actual `PlayerDataStorage` from the active Paper `PlayerList` by verifying exactly one assignable field and exactly one `save` entry with a real detached `ServerPlayer` argument. Invoke that NMS entry **without going through `CraftPlayer.saveData`** while the CraftPlayer hook remains closed. Show that it can independently overwrite the real saved coin to zero, preserve the unrelated STONE and receipt, leave `GrantLedger` pending and force a new readback to reject the obsolete proof. If the method does not exist, no data is written or source changes unexpectedly, the experiment fails rather than inventing a path. Verify after file/dir fsync, abrupt halt and cold restart.
3. **Positive candidate**: Enable a separate exact `PlayerDataStorage.save(ServerPlayer)` entry transformer using the existing standard Java25 Class-File API; require exactly one target method in the real Paper JVM or fail. Use the **same** detached NMS writer after a valid initial save and `a02.nativeAgent.closed=true`. The call must be refused before NMS method body, the real saved 5000C/receipt/STONE must remain, grant pending and no ACK. Force file/dir, halt in a distinct disposable world, cold restart **without any Java Agent** and prove unchanged saved source.
4. Preserve the original CraftPlayer-only success, original InvSee-only bypass and stale ACK negative tests. Refuse a double-counted saved source or an assumed ACK. Check current HEAD Domain/Phase6 and actual Paper regression.
5. **Limits**: This proves at most **two identified entrypoints**. Other NMS/InvSee/file/async/OS writers, already-admitted saves, Global G0 writer drain, G3 exactly-once coordinator, file generation, real BE/JE and complete all-writer coverage remain unproved. Do not ship the Agent or alter Paper/native classes on disk, production SimpleCoins plugin, `main`, VPS, admission or ACK. CAT A02 remains Draft; isolated issue-work check != formal CAT Gates.

Stop if a reached actual writer disproves this mechanism, and record the concrete entrypoint; do not add ever more speculative instrumentation without a product path toward full writer admission/drain. An eventual JVM Agent requirement or server-fork decision must be evaluated as a server architecture change rather than silently deploying it.
