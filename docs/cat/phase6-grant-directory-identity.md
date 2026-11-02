---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.grant-directory-identity
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: issuance journal root directory identity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-005.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.grant | 永続grant rootの物理identity |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| grant.root.replaced | ledger起動後にjournal rootを別ディレクトリへ置換したとき | 古いinstanceでpending/reserveを拒否し再検証済みreplayを要求する | SC-GRANT-ROOT-ID-01 |
| grant.root.valid | 元のjournal rootが不変で有効なとき | restart replay・reservation・ackを維持する | SC-GRANT-ROOT-ID-02 |
| grant.root.invalid | journal rootが欠損・非directory・symlinkのとき | fail-closedで拒否する | SC-GRANT-ROOT-ID-03 |

## Process / PI

An in-memory `GrantLedger` must remain bound to the same physical durable journal directory validated during construction. A directory moved away and replaced by another ordinary directory at the same path (even one containing a copied, superficially valid `head` file) is **not** the original issuance journal. Continuing `pending`, receipt validation or new reservations against stale in-memory grants risks duplicate issuance or lost acknowledgements.

Record the original directory's stable file-system identity and compare it in every health check. If no stable identity can be obtained, reject the durable ledger. A detected mismatch marks the instance unhealthy until a fresh instance replays a verified journal. This is a best-effort single-host identity guard, not an atomic cross-process transaction or protection against file-key reuse.

## TCE / acceptance vectors

- SC-GRANT-ROOT-ID-01 (Red): after moving the journal root, recreating the same path and copying a valid `head` file, `pending` and `reserve` must reject the old instance rather than operating on stale in-memory state.
- SC-GRANT-ROOT-ID-02 (Regression): a newly constructed ledger on the unchanged original directory replays pending grants normally; healthy reservations and acknowledgements remain supported.
- SC-GRANT-ROOT-ID-03 (Regression): a missing, non-directory or symbolic journal root remains fail-closed.

## Gate and integration boundary

CAT Draft only. Formal CAT machine gates and Paper crash/restart fault injection **not-run**. A verified physical root is necessary but not sufficient for end-to-end exactly-once offline grants.
