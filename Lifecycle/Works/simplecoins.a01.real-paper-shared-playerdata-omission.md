---
cat_version: '0.2'
kind: work
id: simplecoins.a01.real-paper-shared-playerdata-omission
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/429
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

# A01 — 実Paper生成の共有playerdataが現行WorldFolder探索から漏れる反例

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/429 |
| https://github.com/surumeneco/SimpleCoins/pull/450 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.real-paper-shared-playerdata-omission | simplecoins.a02.after-ack-crash-replay-verification |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; real saved playerdata with authentic coin + receipt; WorldStorageDiscovery; physical source-set provenance |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.real-paper-shared-playerdata-omission.md;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43830234467 (real Paper, prior tested HEAD `4163480`; formal CAT Gates not-run) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Source-grounded observation and acceptance

- Drive 02B already requires both per-dimension saved source sets and the shared `players/data` set. The unmerged #429 Draft proposes production discovery fixes; this Work is an **independent real Paper verification**, not approval/merge of that code.
- Reuse the three-JVM test-only Paper save/ACK recovery trial: after actual `CraftPlayer.saveData` and two SIGKILLs, the third isolated JVM verifies a legitimate `world/players/data/UUID.dat` containing a durable physical 5000-unit coin and matching receipt bytes. Preserve all original G3 oracle markers.
- In that third JVM, call the **unmodified product** `WorldStorageDiscovery.discover` using the actual currently loaded `World#getWorldFolder()` path, explicit WorldScope and visit budget. Require non-null provenance and a genuine regular shared saved playerdata file outside the dimension root. Capture whether the manifest contains that UUID, and compare physical canonical file paths.
- The expected current limitation is `playerFiles=0` and no resolved player UUID in the manifest **despite** the already-verified Paper playerdata on disk. If the current implementation does include it, record the actual result and revise the test rather than assert an invented omission. This observation must not treat the omitted 5000 coin as a valid zero-value source or a complete G0 snapshot.
- The source read remains loopback-only with no admitted players; no product code, financial state, actual owner settlement, deployment, formal CAT Gate or main release. The test does not claim all Multiverse paths, all playerdata/backup aliases, same-epoch LIVE/saved generation or an approved solution.

## Measured Paper 26.2 source closure counterexample

- In an isolated official Paper build 134, three separate JVM starts exercised `CraftPlayer.saveData/loadData`, durable inventory+receipt, SIGKILL before and after ACK, then the successful third-JVM idempotent recovery. Exact tested head `41634807ad26a3e39cf93dd571bad6bc8d7576f6` passed real Paper [#43830234467](https://github.com/surumeneco/SimpleCoins/actions/runs/43830234467), Domain [#43830234868](https://github.com/surumeneco/SimpleCoins/actions/runs/43830234868), Phase6 [#43830234356](https://github.com/surumeneco/SimpleCoins/actions/runs/43830234356). Tests validated the CAT Work structure; no Confirmed or formal normative CAT Gate was executed.
- The physical level-shared `world/players/data/00000000-0000-0000-0000-00000000a002.dat` was positively verified to contain authentic 5000-value currency and a matching receipt. The current production `WorldStorageDiscovery.discover` called on actual `World#getWorldFolder()` excluded that existing player path: `playerIndexed=false`, `observedPlayerSources=0`.
- Exact Paper line: `PAPER_A01_REAL_PLAYERDATA_DISCOVERY_BOUNDARY_OBSERVED existingMoneySaved=true playerDataOutsideWorldFolder=true playerIndexed=false observedPlayerSources=0`.
- The current product startup `PaperSavedSourceDiscovery` uses the same dimension-world-root-only discovery overload; this is a **verified real Paper missing-authoritative-source counterexample**, not merely a hypothetical path-layout claim. It does not establish that #429 is normatively approved or that its complete proposed fix passes required CAT Gates. G0 cannot regard the current zero-playerdata Manifest as complete.
