---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.untracked-next-grant
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: prevent overwriting untracked issuance records (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: `TASK-20261101-005`.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.grant | 未追跡の次sequence grant記録の保護 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| grant.next.existing | 起動後に次sequenceのgrant pathが現れたとき | 既存bytesを上書きせずledgerをunhealthyにして拒否する | SC-JOURNAL-NEXT-01 |
| grant.next.link | 次sequence pathがdangling symlinkのとき | NOFOLLOW_LINKSで存在を検出し予約を拒否する | SC-JOURNAL-NEXT-02 |
| grant.next.absent | 次sequence pathが正当に不在のとき | 一回のreservationを許可する | SC-JOURNAL-NEXT-03 |

## Process / PI

The live `GrantLedger` computes the next sequence from its in-memory records. An unexpected `<next>.grant` file may have appeared after startup (for example, from an interrupted independent writer or recovery). The durable write helper replaces its target atomically, so writing without checking would destroy that untracked reservation.

Before reserving the next sequence, reject any existing directory entry at the target path using `NOFOLLOW_LINKS`. Latch the ledger unhealthy and require an authoritative restart/reconciliation rather than overwriting the record. A genuinely absent target may be created normally.

## TCE / acceptance vectors

- SC-JOURNAL-NEXT-01: after initialization, an untracked next `.grant` path appears; reservation throws and leaves the existing bytes unchanged.
- SC-JOURNAL-NEXT-02: a dangling symlink at the next record path also blocks reservation.
- SC-JOURNAL-NEXT-03: a normal absent next path permits exactly one reservation.

This does not provide multi-process transactional locking or replace a full journal replay after an ambiguous crash.
