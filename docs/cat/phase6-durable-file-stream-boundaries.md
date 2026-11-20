---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.durable-stream-boundaries
status: draft
process: simplecoins.settlement-journal
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6: checksummed journal stream boundaries (CAT Draft)

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.journal | DurableFile checksummed payload read/write across fixed-size I/O buffers |
| simplecoins.recovery | Reject torn, extended, and corrupted durable records before replay |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| journal.stream.boundary | payload length straddles 8192-byte read chunks | Entire checksummed record round-trips without omission or duplication | DURABLE-STREAM-01 |
| journal.stream.truncate | a committed record loses its final payload byte | Reject as incomplete or invalid, never replay partial payload | DURABLE-STREAM-02 |
| journal.stream.extend | a committed record gains trailing bytes | Reject mismatched checksum/size; do not accept an extended payload | DURABLE-STREAM-03 |
| journal.stream.corrupt | a byte near a read-chunk boundary is altered | Reject the record by checksum before any consumer replay | DURABLE-STREAM-04 |
| journal.stream.budget | the bounded reader receives more bytes than its declared budget | Reject rather than truncate the stream to an apparently valid record | DURABLE-STREAM-05 |

## Process / PI

These are independent regression vectors for the existing `DurableFile` primitive on `develop`, not a new settlement source, snapshot generation, journal schema, or crash-recovery algorithm. They exercise the 8192-byte read loop and SHA-256 record boundary. A passing round-trip does not prove durable directory identity, atomic world freeze, power-loss safety, loaded/offline consistency, or end-to-end grant replay.

## Test model / TCE

- DURABLE-STREAM-01: 8191, 8192, 8193 and 16384 payload bytes all survive a checksummed write/read.
- DURABLE-STREAM-02: removing one byte from a valid record fails closed even when the read budget is sufficient.
- DURABLE-STREAM-03: appending one byte to a valid record fails closed.
- DURABLE-STREAM-04: changing a byte adjacent to an I/O chunk boundary fails checksum validation.
- DURABLE-STREAM-05: `readBounded` rejects one byte over the budget and returns the complete exact-budget input.

## State / Gate

**Draft, not Confirmed.** This is additional coverage for existing `DurableFile.java` (develop blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8`). Formal CAT process-closure, TestModel, model-conformance, vectors, Queue, current-item and implementation-conformance are not-run. No production change or permission to merge a broader Phase 6 implementation is inferred.
