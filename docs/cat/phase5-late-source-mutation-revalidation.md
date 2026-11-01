---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.late-source-mutation-revalidation
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: late saved-source mutation revalidation (CAT Draft)

Status: **Draft** (not Confirmed). Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.


## 対象
| Ref | Aspect |
| --- | --- |
| WorldSnapshotTraversal | 後続callbackによる既読保存sourceの変更検出 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| mutation.region | 後続player callbackが先に読み終えたregionを変更したとき | 成功Reportを返さず拒否する | SC-LATE-MUTATION-01 |
| mutation.player | 後続player callbackが先に読み終えたplayerdataを変更したとき | 成功Reportを返さず拒否する | SC-LATE-MUTATION-02 |
| mutation.unchanged | 全sourceが変更されず読取可能なとき | 既存の完全Reportを維持する | SC-LATE-MUTATION-03 |
| mutation.prior-guards | sourceを読む前と直後の検査を行うとき | 物理aliasと局所mutationの既存guardを維持する | SC-LATE-MUTATION-04 |

## Process / PI

WorldSnapshotTraversal reads a bounded list of authoritative Anvil and playerdata files and stages values through visitor callbacks. A file that passed its own immediate post-read stamp may be changed by a callback for a later source. A successful traversal report must not be returned if an already-read mandatory source has observably changed before the traversal finishes.

Retain the existing no-follow file identity/size/creation/modified-time stamp from before each read, compare it immediately after its own callbacks, and **recheck every completed mandatory source after the final callback and before returning Report**. Any mismatch or inaccessible/non-regular source fails closed with IOException. This is best-effort mutation detection, not a coherent world-memory/disk snapshot or authorization to publish settlement totals.

## TCE / acceptance vectors

- SC-LATE-MUTATION-01 (Red): after a region chunk was read, a later player callback appends to the earlier region. Traversal must throw rather than report success.
- SC-LATE-MUTATION-02 (Red): after the first playerdata was read, a later player's callback changes the earlier playerdata. Traversal must throw rather than report success.
- SC-LATE-MUTATION-03 (Regression): unchanged region and playerdata sources still yield the expected complete report.
- SC-LATE-MUTATION-04 (Safety): existing pre-callback physical-source alias checks and per-source mutation checks remain in force.

## Gate status

Formal CAT machine gates: **not-run**. This Draft is not Confirmed and does not authorize merge, deployment, publication, or a claim of full loaded/saved source reconciliation.
