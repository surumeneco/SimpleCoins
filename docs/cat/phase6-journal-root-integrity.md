---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.journal-root-integrity
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: issuance journal root directory integrity (CAT Draft)

Status: **Draft** (not Confirmed). Leaf TASK-20261101-005.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.grant | journal rootとancestorのsymlink拒否 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| journal.root.symlink | journal rootがsymlinkのとき | read/replay/writeより前にIOExceptionで拒否する | SC-JOURNAL-ROOT-01 |
| journal.root.valid | 通常のreal directoryを使うとき | restartを跨いだ正常なjournal操作を維持する | SC-JOURNAL-ROOT-02 |
| journal.root.ancestor | journal rootまでのancestorにsymlinkがあるとき | directory/head生成前に拒否し通常のnested pathは維持する | SC-JOURNAL-ROOT-03 |

## Process / PI

The single-writer monetary grant journal must reside in the explicitly selected directory, not a symbolic link that redirects reads and atomic writes to an unverified directory. Java `Files.createDirectories` accepts an existing symlink to a directory; therefore, after ensuring the journal root exists, verify it is a real directory using `LinkOption.NOFOLLOW_LINKS`. Reject a symbolic/non-directory root with IOException before listing, replaying, or writing journal records.

This is a local root-path check, not a proof that every ancestor is symlink-free or that concurrent filesystem mutation is impossible. Snapshot ownership and multi-process locking remain separate work.

## TCE

- SC-JOURNAL-ROOT-01 (Red): a symlink pointing to an otherwise valid journal directory cannot be opened as a grant ledger.
- SC-JOURNAL-ROOT-02 (Regression): an ordinary real journal directory remains writable and replayable after restart.

Formal CAT machine Gates not-run; Draft not Confirmed. No release or merge.

## Ancestor-path integrity (additional Draft vector)

- SC-JOURNAL-ROOT-03: a symlink in **any ancestor** of the issuance journal root is rejected before creating the final journal directory or head file, even if the final path itself is a real directory. Validate the absolute normalized path both before and after creating missing directories. A normal nested path remains writable and replayable.

This prevents accidental traversal through a pre-existing symlink. It does not claim atomic protection against an adversary concurrently replacing path components; secure directory-handle traversal/locking remains a separate integration requirement.
