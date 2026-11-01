# Phase 6: in-process settlement generation publication (CAT Draft)

Status: Draft, not Confirmed. Leaf TASK-20261101-005. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md.

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
