---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.journal.durable-file-payload-boundaries
status: draft
process: simplecoins.journal
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DurableFile payload boundaries (existing-code characterization)

## Scope and authority

TASK-20261101-005, code-to-spec observation of `DurableFile.java` on
`develop` commit `f92d36288916ffc47f8806d4845d4b85ae981ad6`,
blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8`.

**Draft / not Confirmed**: the checks characterize existing low-level durable-file behavior; they do not authorize a change in monetary or crash-recovery semantics, nor do they prove full GrantLedger consistency.

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.journal.durable-file-payload-boundaries | `DurableFile.java` の既存コードにおけるrecord・checksum・size・filesystem境界のDraft観測 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| DURABLE-PAYLOAD-01 | 当該境界入力（右記の直接検証条件を参照） | A checksummed payload whose length equals `maximumBytes` can be read; a smaller bound raises `IOException` | 4-byte payload at budgets 4 and 3 |
| DURABLE-PAYLOAD-02 | 当該境界入力（右記の直接検証条件を参照） | A zero-length payload can be round-tripped with budget 0; a negative budget raises `IllegalArgumentException` | empty data, 0 and -1 |
| DURABLE-PAYLOAD-03 | 当該境界入力（右記の直接検証条件を参照） | A symbolic link to an **existing valid** record, and a directory masquerading as a record, are rejected | `NOFOLLOW_LINKS` positive-target alias and folder |
| DURABLE-PAYLOAD-04 | 当該境界入力（右記の直接検証条件を参照） | A successful atomic replacement exposes only the new checksummed contents and does not leave `.pending-*.tmp` in the journal directory | write then replace, read new payload, list temporary entries |
| DURABLE-PAYLOAD-05 | 当該境界入力（右記の直接検証条件を参照） | A one-byte payload alteration with an unchanged persisted digest is rejected as `IOException`; rejection does not rewrite the file | flip a payload byte after `DurableFile.write`, call `read`, compare disk bytes |
| DURABLE-PAYLOAD-06 | 当該境界入力（右記の直接検証条件を参照） | A one-byte change in the stored SHA-256 header is rejected while the payload remains unchanged | flip one digest byte, expect `IOException` |
| DURABLE-PAYLOAD-07 | 当該境界入力（右記の直接検証条件を参照） | Incomplete digest and truncated payload records cannot be returned as empty or partial monetary data | truncate signed record to 31 bytes and then to one missing payload byte |
| DURABLE-PAYLOAD-08 | 当該境界入力（右記の直接検証条件を参照） | Appended bytes under an otherwise permissive size budget invalidate the digest, and restoring the original signed bytes restores readability | append one byte; expect `IOException`, restore bytes and read |

These are independent of the preexisting short-read/late-I/O tests, missing/dangling-symlink tests, and grant acknowledgement/untracked-future Draft Red candidates. Target suites: `DurableFilePayloadBoundaryTest`, `DurableFileChecksumFaultTest`. The new vectors describe observed existing-code behavior, not an approved change to monetary semantics.

## Evidence and limits

- Direct Java 21 execution against the exact `DurableFile.java` blob: 8/8 assertions/expected exceptions passed in a temporary directory, using `javac` and `java`.
- Follow-up direct Java 21 checksum-fault harness: 8/8 checks against independently SHA-verified `DurableFile.java` Git blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8` (payload/digest bit flips, two truncation shapes, extension, recovery). Pending JUnit/native project execution remains separate.
- Direct harness is not JUnit execution. The repository JUnit class and its CI result are separate verification steps.
- No process-closure/model-conformance/vectors/Queue/current-item/implementation-conformance, JDK25 CI, Paper live crash/restart or GrantLedger recovery claims follow from this evidence.
- No production source is modified by this test-only code-to-spec work.
