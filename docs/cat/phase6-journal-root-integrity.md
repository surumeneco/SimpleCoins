# Phase 6: issuance journal root directory integrity (CAT Draft)

Status: **Draft** (not Confirmed). Leaf TASK-20261101-005.

## Process / PI

The single-writer monetary grant journal must reside in the explicitly selected directory, not a symbolic link that redirects reads and atomic writes to an unverified directory. Java `Files.createDirectories` accepts an existing symlink to a directory; therefore, after ensuring the journal root exists, verify it is a real directory using `LinkOption.NOFOLLOW_LINKS`. Reject a symbolic/non-directory root with IOException before listing, replaying, or writing journal records.

This is a local root-path check, not a proof that every ancestor is symlink-free or that concurrent filesystem mutation is impossible. Snapshot ownership and multi-process locking remain separate work.

## TCE

- SC-JOURNAL-ROOT-01 (Red): a symlink pointing to an otherwise valid journal directory cannot be opened as a grant ledger.
- SC-JOURNAL-ROOT-02 (Regression): an ordinary real journal directory remains writable and replayable after restart.

Formal CAT machine Gates not-run; Draft not Confirmed. No release or merge.
