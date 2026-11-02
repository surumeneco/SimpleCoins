---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.journal.durable-file-record-boundaries
status: draft
process: simplecoins.journal
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6 — checksummed durable record boundary regressions (CAT Draft)

Status: Draft, not Confirmed. Test preparation for the existing `DurableFile` primitive already in `develop`; no production change or CAT semantic Gate pass is claimed.

## Boundary and vectors

| ID | Given | Expected |
| --- | --- | --- |
| DURABLE-BOUNDARY-01 | Empty payload and read budget zero | SHA-256 header-only record round-trips as empty bytes |
| DURABLE-BOUNDARY-02 | Nonempty payload and exact read budget | Exact bound succeeds; one byte less fails with IOException |
| DURABLE-BOUNDARY-03 | Atomic replacement of a valid existing record | New bytes read back, old bytes not returned, temporary pending file absent |
| DURABLE-BOUNDARY-04 | Nested target directory absent | Write creates required parent directory and record round-trips |
| DURABLE-BOUNDARY-05 | Symbolic link to a valid regular record, or directory used as target | Read rejects both rather than following a link or counting an empty record |
| DURABLE-BOUNDARY-06 | Negative read budget, null bounded stream, negative bounded budget | Invalid arguments fail rather than becoming zero-length journal reads |

These tests do not establish symlink-free **ancestor directories** (separately tracked in Draft PR #35), cross-ledger grant receipt identity (Draft PR #19), crash atomicity across a real filesystem, or complete Paper settlement generation. No production Gate or release follows from a passing pure Java test.
