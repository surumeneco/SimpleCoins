---
cat_version: '0.2'
kind: work
id: simplecoins.a01.native-save-agent-bridge
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7077640456
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
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

# A01 — Java25起動Agentと製品WriterEpochFenceの隔離クラスローダー接続

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010 |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7077640456 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.native-save-agent-bridge | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.native-save-agent-bridge.md;src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;writer-fence-agent/**;.github/workflows/a01-native-save-agent-bridge.yml | src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;writer-fence-agent/src/main/java/** | src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/test/**;.github/workflows/a01-native-save-agent-bridge.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

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

## Authorized scope and limitations

正本はConfirmed A01/G0（Process/PI/TCE）、技術方式は利用者委任済みJava 25 launch-time Agent。目的は既存の`WriterEpochFence`へ、別classloaderから**真正なCraftPlayer.saveData()VとPlayerDataStorage.save(Player)Vのentry/return**を伝達する最小の製品接続を作り、実際のbytecode変換の対象クラス/descriptor数を検査すること。Java25 `ClassFile` APIで対象を限定し、Bootstrap側のHook JARとAgent JARを分ける。単独のtest-only Agentの永続採用ではない。

初期はpassive観測のみ。SimpleCoins起動時の接続だけでは`WriterEpochFence.arm()`もHookの限定追跡開始も行わず、通常保存を妨げない。Hookが欠損・二重・後発変換・入出場不一致を検知した場合の結果を未証明として止める。非同期子処理は同一スレッドの親から認可せず独立root入場を要する。exceptional returnを未計数のまま正常returnと見なさず排出完了不可とする（catch-allを実装・証明するまではinconclusive）。

**受入境界**：製品classloaderとbootstrapのcross-loader callback、実装済みの2つのnative入口のbytecode exact match、入口拒否・同期子の継承・正常return追跡・未終了が排出を拒否することをJava25で確認。実Paper 26.2 + 24配布JARの全writer・非同期・実保存同期・原子的Snapshot公開・配布ACK・Admissionの成立はこの変更では未証明。全員拒否を維持し、確認済み2入口だけからA01証拠のBooleanを真にしない。CAT `implementation-conformance` は独立の製品証明がない間 inconclusive。

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-native-bridge-model.json | @work/.cat-flow/cache/a01-native-bridge-vectors.json | @work/.cat-flow/cache/a01-native-bridge-queue.json | — | — | @work/.cat-flow/cache/a01-native-bridge-obligations.json | — | false |
