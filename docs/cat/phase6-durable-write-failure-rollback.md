---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.durable-write-rollback
status: draft
process: simplecoins.journal.durable-write
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Durable write rollback observation

TASK-20261101-005. Code-to-spec Draft, **not Confirmed**.
Source: `DurableFile.java` Git blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8` on `develop`.

Test vectors:

1. Replacing a valid record with a null payload raises an error, keeps the previously readable record, and cleans temporary files.
2. Writing to a path already occupied by a nonempty directory fails without removing that directory or its contents and without leaving temporary files.
3. Modifying a caller-owned byte array after a successful write does not modify the stored checksummed payload.

These are observations of the existing implementation on a Linux filesystem, not a new approved journal design. Passing JUnit tests are scoped regression evidence only. CAT normative gates, full settlement, restart/crash recovery and Paper integration remain unverified.
