---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-pdc-inventory-agent-writers
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
  - simplecoins.a01.native-save-agent-bridge
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 — 既存製品AgentをPDC／Inventoryの実Paper writerに接続

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010 |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7083476585 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.paper-pdc-inventory-agent-writers | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-pdc-inventory-agent-writers.md;src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/test/**;.github/workflows/a01-native-save-agent-bridge.yml | src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java | src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/test/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

Confirmed A01/G0の全writer閉鎖／既入場排出／世代継続を維持し、既存Java25製品AgentのPaper native Craft/NMS 2入口へ、**実到達が過去のPaper26.2試験で確認されている低位PDC 2メソッドとInventory setItem 1メソッド**を同じbootstrap Hook・製品WriterEpochFenceへ追加する。API class/メソッド/descriptorが一致した場合のみ変換し、他target・duplicate・例外時には製品Fenceを失効する。同期的PDC/Inventory childは既入場親と同一ThreadLocal ancestryに限り入場を引き継ぎ、独立rootは閉鎖後拒否する。非同期の親継承は認めない。

変換確認はJava25 Class-File API/JUnit/実Paper・現行配布Plugin実行証拠に分離する。以前のPDC/Inventoryテスト専用Agentの結果を製品G0全writer成立と転記しない。全sourceの走査/帰属/BAN、未観測native/Plugin writer、非同期派生、直接ファイル、exceptional completion、原子Snapshot切替・通常仕訳引継・G3永続ACKは未結線。Agentはpassive・Admission全員拒否のまま。これらの不足はCAT Implementation Conformanceをinconclusiveに保つ理由であり、製品のG0公開やG3 ACK権限を意味しない。

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-pdc-inventory-model.json | @work/.cat-flow/cache/a01-pdc-inventory-vectors.json | @work/.cat-flow/cache/a01-pdc-inventory-queue.json | — | — | @work/.cat-flow/cache/a01-pdc-inventory-obligations.json | — | false |
