---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.settlement-generation-publication
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: in-process settlement generation publication (CAT Draft)

Status: Draft, not Confirmed. Leaf TASK-20261101-005. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.settlement | in-process generationとdurable publication |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| generation.incomplete | stagingが未完了または失敗したとき | 既存の公開済みgenerationを変更しない | SETTLEMENT-GEN-01 |
| generation.success | 完了したstagingからpublishするとき | 世代を一度だけ進め完全な結果を原子的に公開する | SETTLEMENT-GEN-02 |
| generation.concurrent | 期待世代がstaleか同時publisherが競合するとき | 双方成功や不正な世代更新を拒否する | SETTLEMENT-GEN-03 |
| generation.invalid | null入力またはgeneration overflowのとき | 公開済み結果を変更せず拒否する | SETTLEMENT-GEN-04 |
| generation.durable | durable callbackを介して公開するとき | 永続化成功後のみ可視化し、callback前の検証失敗は再試行可能とする | SETTLEMENT-GEN-05, SETTLEMENT-GEN-06 |
| generation.uncertain | durable callbackがcommit後に例外を投げた可能性があるとき | publisherをpoisonしrestartで権威generationを再確認するまで更新しない | SETTLEMENT-GEN-07 |

## Process / PI

A completed `SettlementAccumulator` produces one immutable `Result` only after every declared source is complete and no input/source failure occurred. A live in-process reader must see either the prior complete result or the new complete result, never intermediate personal/free/dead balances. A caller supplies the expected generation; stale concurrent publication is rejected. A failed or incomplete staging transaction does not alter the published generation.

This is an **in-memory publication boundary**, not persistent storage, a world freeze, disk/memory snapshot reconciliation, or a crash-recovery mechanism. Restart-safe generation persistence and source-consistency coordination remain separate required work.

## Test model / TCE

- SETTLEMENT-GEN-01 Red: an incomplete/failed accumulator cannot replace a previously published snapshot.
- SETTLEMENT-GEN-02: a completed staging transaction advances the generation exactly once and readers see its complete result.
- SETTLEMENT-GEN-03: stale expected generations are rejected; concurrent contenders with the same expected generation cannot both publish.
- SETTLEMENT-GEN-04: no generation overflow or null staging input can silently replace a published result.

Formal CAT machine Gates and Paper/live-world integration are not-run.

## Persist-before-visibility boundary (Draft vectors)

- SETTLEMENT-GEN-05: a caller-supplied durable persistence operation receives only a fully committed candidate generation; readers do not observe it until persistence returns successfully. An IOException leaves the prior generation intact.
- SETTLEMENT-GEN-06: stale expected generations and null persistence reject before the callback; a subsequent fresh, independently staged generation can be persisted and published exactly once.
- This callback boundary does not itself implement disk serialization, fsync, restart replay, multi-process locking or source freeze. Only a verified durable writer can supply the durability guarantee; in-memory `publish` remains explicitly non-durable.

## Ambiguous durable-writer failure (Draft vector)

- SETTLEMENT-GEN-07: a persistence callback may commit to disk and then throw before returning. After any callback IOException/runtime failure, the publisher must enter an uncertain/poisoned state and reject all subsequent publications, including in-memory publication, until authoritative restart recovery reconciles the durable generation. A validation failure or incomplete accumulator before the callback does not poison the publisher.
