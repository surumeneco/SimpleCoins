---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.durable-settlement-snapshot
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: restart-safe settlement snapshot generations (CAT Draft)

Status: **Draft**, not Confirmed. Leaf `TASK-20261101-005`.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.settlement | 永続snapshotのgeneration・検証・再起動 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| snapshot.restart | commit後にpublisher/storeを再生成するとき | 全残高・dead locationsを復元し統計を再計算する | SETTLEMENT-STORE-01 |
| snapshot.sequence | successor生成または同時writer競合があるとき | stale/飛越generation・同時commitを拒否し有界lockを維持する | SETTLEMENT-STORE-02, SETTLEMENT-STORE-07, SETTLEMENT-STORE-08 |
| snapshot.invalid | 永続recordが破損・未知版・不正合計・重複UUID・不正pathのとき | 空世代へのfallbackを許さずfail-closedとする | SETTLEMENT-STORE-03, SETTLEMENT-STORE-05, SETTLEMENT-STORE-06 |
| snapshot.staging | 未完了stagingまたは永続化後のmemory-only publicationがあるとき | 完了した入力だけ永続化し以後の非永続publicationを禁止する | SETTLEMENT-STORE-04, SETTLEMENT-STORE-09 |
| snapshot.initialization | 初回世代のmarkerとsnapshotを書き込むとき | marker先行durabilityを要求し、marker欠損や失敗後の世代0再初期化を拒否する | SETTLEMENT-STORE-10, SETTLEMENT-STORE-11, SETTLEMENT-STORE-12 |
| snapshot.ambiguous | writerがcommit後にErrorを投げるか再入publicationがあるとき | 元Errorを伝播してpublisherをpoisonし、同一publisherで再入更新を許さない | SETTLEMENT-STORE-13, SETTLEMENT-STORE-14 |

## Process / PI

A complete `SettlementAccumulator.Result` can be persisted as one versioned, checksummed generation. Publication is visible to in-process readers only after the writer atomically replaces and fsyncs the complete snapshot and verifies its readback. On restart, a new publisher must recover the last committed generation before accepting a successor. Missing data means generation zero only if no snapshot file exists; corrupt, truncated, unknown-version, or semantically inconsistent files must fail closed. A writer rejects stale/nonsequential generations and protects a single-host write sequence with a file lock.

Serialize all personal balances, Free/Dead totals, and positive dead-asset locations; recompute statistics on load rather than trusting serialized derived statistics. Verify that the sum of dead locations equals the dead total, reject duplicate UUIDs, negative values, excess sizes, and unknown dead reasons. The result is a durable *completed input set*, not proof that Minecraft's loaded/unloaded world sources were frozen or complete.

For the first generation, write and durably confirm the initialization marker before attempting the snapshot replacement. If the snapshot write then fails, the marker-without-snapshot state must fail closed rather than allow a later generation-zero reset. This store does not itself provide a distributed lock, authoritative source closure, Paper join barrier, external owner evidence, or a world-consistent snapshot. An IOException after a write may be ambiguous: the in-process publisher stays poisoned until a fresh instance reloads the durable record.

## TCE / Test model

- SETTLEMENT-STORE-01 (Red): a committed generation survives store/publisher restart with exact balances, dead locations and recomputed statistics.
- SETTLEMENT-STORE-02: generation 1→2 succeeds; a stale or skipped generation cannot overwrite the durable head.
- SETTLEMENT-STORE-03: corrupt/truncated or unknown-version bytes cause checked IOException and block further publication; no fallback to empty generation.
- SETTLEMENT-STORE-04: incomplete/failed staging cannot write a snapshot.
- SETTLEMENT-STORE-05: symlinked ancestor/root and non-regular snapshot paths fail closed.
- SETTLEMENT-STORE-06: a snapshot with invalid totals or duplicated identity cannot be accepted as a committed candidate.
- SETTLEMENT-STORE-07: concurrent writers using the same store contract cannot both commit the same generation.
- SETTLEMENT-STORE-08: an occupied local writer lock rejects promptly rather than waiting indefinitely.
- SETTLEMENT-STORE-09: after durable publication or verified restart recovery, memory-only publication is forbidden; new generations must pass the durable writer.
- SETTLEMENT-STORE-10: a durable initialization marker prevents a deleted snapshot from being misread as a fresh generation-zero store.
- SETTLEMENT-STORE-11 (Red): initialization marker is durable before the first snapshot write; a failed initial snapshot replacement cannot silently reset to generation zero after the obstructing file is removed.
- SETTLEMENT-STORE-12 (Red): a present snapshot with its required initialization marker missing must fail closed on load and successor persist.
- SETTLEMENT-STORE-13 (Red): if a durable writer commits and then throws an Error, the publisher must rethrow that Error and poison further in-process publication until a fresh durable recovery; the failure must never be treated as a successful acknowledgement.
- SETTLEMENT-STORE-14 (Red): while a durable writer callback is active, reentrant in-memory or durable publication on the same publisher is rejected; the outer publication must not be overwritten by a nested generation.

Formal CAT machine Gates, real-world source consistency, and Paper fault-injection integration: **not-run**. Do not promote this Draft to Confirmed without an authorized decision.
