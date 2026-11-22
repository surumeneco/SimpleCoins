---
cat_version: '0.2'
kind: work
id: simplecoins.a01.wwc-www-owner-agent-writers
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7083476585
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.paper-pdc-inventory-agent-writers
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 — 製品AgentにWWC/WWWの実所有者保存入口を接続

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010 |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7083476585 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.wwc-www-owner-agent-writers | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.wwc-www-owner-agent-writers.md;src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/test/**;.github/workflows/a01-native-save-agent-bridge.yml | src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java | src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/test/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Normative boundary and evidence

既決のConfirmed A01/G0全writer受付・既入場排出・世代継続を維持する。前段で製品Agentに結線したPaper native Craft/NMS・低位PDC/Inventoryの共有`WriterEpochFence`に、現行配布WWC 2.1.1の`WonderfulChickenStore.save(Chicken, WonderfulChickenData)`、WWW 1.1.2の`WonderfulWolfLoadedIndividuals.saveAndRegister(Wolf, WonderfulWolfIndividual)`の**真正な2メソッド**を同一保護世代へ接続する。Java25 Agentはクラス名・メソッド名・完全記述子・件数を固定照合し、未確認バイトコード・例外・重複ロードでG0/G3の許可証拠を作らない。

WWC/WWWの保存が実際に個体の追従先・指揮者・所持品のowner/alias/PDCを変える反例は既存の[PR #603](https://github.com/surumeneco/SimpleCoins/pull/603)と[PR #606–#614](https://github.com/surumeneco/SimpleCoins/pull/614)で一部再現済み。これらは旧test-only Agentの局所証拠に限られ、新製品Agentへの接続後の実Paper/配布JARで初めて相当性を立証する。

製品AgentはこのWorkでもpassiveのまま。WWC/WWWと低位PDC/Inventoryの同期子処理だけが当該親の入場を引き継ぐ。親の無い新規rootは閉鎖後拒否する。非同期継続には同じThreadLocal親を流用しない。全24-JAR writerの静的候補・NMS直接保存・他のPaper Inventory/EntityEquipment・直接ファイルwriter・exceptional完了等は未閉包であるため、原子Snapshot切替・G3永続ACK・Admissionは禁止を維持する。CAT Implementation Conformanceは引き続きinconclusive。

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-wwcwww-owner-model.json | @work/.cat-flow/cache/a01-wwcwww-owner-vectors.json | @work/.cat-flow/cache/a01-wwcwww-owner-queue.json | — | — | @work/.cat-flow/cache/a01-wwcwww-owner-obligations.json | — | false |
