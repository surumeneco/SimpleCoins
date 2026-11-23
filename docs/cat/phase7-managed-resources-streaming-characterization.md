---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.managed-resources-streaming-characterization
status: candidate
process: simplecoins.generation.resources
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
  - https://github.com/surumeneco/SimpleCoins/blob/8348b390f33c707aa1e1a1b00aa7d62374ad8872/src/main/java/co/surumene/simplecoins/domain/ManagedResources.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# ManagedResources — existing-resource SHA-256 memory characterization

## Scope and authority

TASK-20261101-006 / Chat-mode `code-to-spec` observation and a proposed
non-functional resource-consumption improvement. The baseline source is
`ManagedResources.java` Git blob at `develop` commit
`8348b390f33c707aa1e1a1b00aa7d62374ad8872`.

**Candidate, not Confirmed.** The fixed 9-denomination resource/pack ownership
contract is the product requirement. This document is not normative approval,
CAT process closure, model conformance or deployment acceptance.

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.generation.resources.managed-integrity | Existing SHA-256 ownership check for previously installed Geyser pack and mapping files |

## 観測

| ID | 現行実装の観測条件 | 観測結果 | 証拠 |
| --- | --- | --- | --- |
| RES-STREAM-OBS-01 | Existing file size is 1 byte, ordinary replace and delete flow | Existing owner hash is checked and owned outputs replace; modified or unowned outputs are rejected | JDK21 local baseline 9/9 direct assertions, source blob verified from GitHub |
| RES-STREAM-OBS-02 | Existing owned regular pack is 72 MiB, Java child heap `-Xmx64m` | `Files.readAllBytes` throws `OutOfMemoryError` at `ManagedResources.replace` before a replacement is committed | JDK21 local failure exit 1, stack trace at baseline line 37 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| RES-STREAM-PRESERVE-01 | Existing owned pack/mapping file is unchanged | Compare exactly the same SHA-256 digest against saved ownership manifest; no unowned file overwrite | Existing `ManagedResourcesTest` + direct 9 checks, before/after |
| RES-STREAM-PRESERVE-02 | Existing owned file has been modified, removed or replaced by a link | Reject update and preserve protected file; no silent ownership adoption | Existing `ManagedResourcesTest` + direct 9 checks |
| RES-STREAM-PERF-01 | Existing legitimate owned pack is larger than configured Java heap | Validate old hash in bounded memory, without `readAllBytes`, so update itself does not fail due to whole-file buffering | Dedicated child-JVM test with 72 MiB sparse pack / 64 MiB heap |

A streaming digest proposal would change the memory complexity of the existing
SHA-256 validation from O(file size) to O(8 KiB), without changing the hash,
file ownership, atomic move, or manifest format. The large-file improvement is
an engineering proposal and requires normal refactor authorization and regression
before production integration. Formal CAT semantics and Paper/Geyser real-server
acceptance are not-run.
