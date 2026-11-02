---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.snapshot-directory-identity
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: durable settlement root directory identity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-005.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.settlement | durable snapshot rootの物理identity |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| snapshot.root.replaced | 既存rootを別directoryへ置換したとき | 古いstoreのloadは空世代を返さずIOExceptionで拒否する | SC-SNAPSHOT-ROOT-ID-01 |
| snapshot.root.persist | 既存rootを置換した後にpersistするとき | 代替rootへsnapshot/markerを書かず拒否する | SC-SNAPSHOT-ROOT-ID-02 |
| snapshot.root.fresh | 正規のrootにfresh storeを構築したとき | 空rootと永続化済みgenerationを正しく区別する | SC-SNAPSHOT-ROOT-ID-03 |
| snapshot.root.race | pre-write hook中にrootが置換されたとき | 代替rootへpayloadを書き込む前に拒否する | SC-SNAPSHOT-ROOT-ID-04 |

## Process / PI

A `SettlementSnapshotStore` instance is bound to the **physical directory** validated at construction, not merely a reusable path string. Deleting or moving the original root and creating another ordinary directory at the same pathname must not make the existing instance treat a previously initialized store as generation zero. This is distinct from the existing tests for a missing directory or replacement by a regular file.

At construction, record a stable file-system directory identity from `BasicFileAttributes.fileKey()`. Require the same identity before every load/persist operation. If the provider cannot supply a stable key, fail closed rather than claiming a durable accounting guarantee. This check is an in-process guard, not a defense against adversarial file-system identity reuse, a cross-process transaction, or a frozen Paper world.

## TCE / acceptance vectors

- SC-SNAPSHOT-ROOT-ID-01 (Red): after moving the original directory and creating a new empty directory at the same pathname, `load()` must throw `IOException`, not return `Optional.empty()`.
- SC-SNAPSHOT-ROOT-ID-02 (Red): `persist(generation=1)` on the old store instance must reject the substituted directory without writing a snapshot or initialization marker.
- SC-SNAPSHOT-ROOT-ID-03 (Regression): a fresh instance bound to a genuine empty directory returns no generation; a normal committed generation survives restart using a newly constructed store on the same physical directory.
- SC-SNAPSHOT-ROOT-ID-04 (Red): a root substitution during the deterministic pre-write hook aborts before writing the generation payload into the substitute directory.

## Gate and deployment

CAT Draft only; formal machine gates, real Paper lifecycle fault injection and cross-process storage tests **not-run**. The change does not authorize settlement publication.
