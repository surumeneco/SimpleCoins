---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.world-root-identity-closure
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: world-root identity closure (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot | 保存source走査前後のroot物理同一性 |
| simplecoins.world | caller指定world root集合とalias判定 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| root-identity.changed | 走査中に保存source集合が不変でもworld rootの実体が置換されたとき | 走査結果を無効化し成功Reportを返さない | SC-ROOT-01 |
| root-identity.alias | 異なるworld名が同じ物理ディレクトリを指すとき | visitor前に拒否する | SC-ROOT-03 |
| root-identity.valid | world rootと保存sourceが不変で有効なとき | 既存の成功Reportを維持する | SC-ROOT-02 |
| root-identity.invalid | rootが存在しない、symlink、または物理identity不明のとき | visitor前にfail-closedで拒否する | SC-ROOT-04 |

## Process / PI

The caller supplies a bounded set of authoritative world roots. Matching the saved-file manifest before and after traversal does not establish that the same root directories were observed: an empty world root can be replaced while a different world's player callback runs, leaving the saved-source list unchanged.

At the traversal boundary, capture each world-root directory's physical identity before discovery. Verify that every supplied root is still the same non-symbolic directory after the preflight discovery and again after the postflight discovery. A root that disappears, becomes a symbolic link, changes identity or has no verifiable physical file key must invalidate the staged report. The caller must still provide a complete authoritative world-root inventory; this check does not prove completeness, freeze Paper memory or authorize settlement publication.

## TCE / acceptance vectors

- SC-ROOT-01 (Red): replacing an empty second world root during another world's playerdata callback must abort even if the discovered source-file set is unchanged.
- SC-ROOT-02 (Regression): unchanged roots and source files return the existing successful traversal Report.
- SC-ROOT-03 (Red): two differently spelled world roots resolving to the same physical directory must fail before any callback, even if both are empty.
- SC-ROOT-04 (Regression): a missing or symbolic world root fails closed before any visitor callback.

## Gate status

Formal CAT machine gates **not-run**. This Draft is a conservative root-identity check, not an atomic filesystem snapshot, complete live-world reconciliation or publication gate.
