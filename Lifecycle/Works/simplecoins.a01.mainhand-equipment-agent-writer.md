---
cat_version: '0.2'
kind: work
id: simplecoins.a01.mainhand-equipment-agent-writer
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010
  - https://github.com/surumeneco/SimpleCoins/pull/609
  - https://github.com/surumeneco/SimpleCoins/pull/623
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a01.wwc-www-owner-agent-writers
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 — native mainhand setterを同一製品WriterEpochFenceへ接続

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010 |
| https://github.com/surumeneco/SimpleCoins/pull/609 |
| https://github.com/surumeneco/SimpleCoins/pull/623 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.mainhand-equipment-agent-writer | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.mainhand-equipment-agent-writer.md;src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java;writer-fence-agent/src/test/**;.github/workflows/a01-native-save-agent-bridge.yml | src/main/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapter.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/WriterEpochHook.java;writer-fence-agent/src/main/java/co/surumene/simplecoins/writeragent/PaperNativeWriterAgent.java | src/test/java/co/surumene/simplecoins/domain/PaperNativeSaveFenceAdapterTest.java;writer-fence-agent/src/test/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

Confirmed A01の全writer新規受付拒否・既入場排出・同一世代保持の保証条件を変更しない。既存の製品Java25 Agentにて、実Paper 26.2 `org.bukkit.craftbukkit.inventory.CraftEntityEquipment` の `setItemInMainHand(ItemStack)` と `setItemInMainHand(ItemStack,boolean)` の双方を厳密method descriptorで見付けて入口/正常returnを**同一** `WriterEpochFence` へ伝達する。WWC/WWWの貨幣ownerが同じでも、実WWWのnative装備の内容だけを変える実到達反例は[PR #609](https://github.com/surumeneco/SimpleCoins/pull/609)で再現済み。追加のAgentが対象2メソッドを実Paperで変換できるか検証する。

装備setterは独立rootの場合は閉鎖後拒否、既入場WWW/PDCなどの同期的下位操作は同一thread内の親を継承して排出を待つ。非同期経由へThreadLocal親を流用しない。exception path/未識別writerは正常排出とみなさない。既存の原子公開・G3 durable ACK・Admissionは結線しない。専用Test fixture/実Paperの限定証拠を全配布24-JARの閉包と混同しない。

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-equipment-model.json | @work/.cat-flow/cache/a01-equipment-vectors.json | @work/.cat-flow/cache/a01-equipment-queue.json | — | — | @work/.cat-flow/cache/a01-equipment-obligations.json | — | false |
