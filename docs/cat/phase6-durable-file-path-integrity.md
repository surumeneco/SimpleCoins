---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.durable-file-path-integrity
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs:
  - simplecoins.phase6.journal-root-integrity
semantic_contract: markdown-v2
language: ja
---

# Phase 6: durable file ancestor path integrity (CAT Draft)

Status: **Draft**, not Confirmed. Leaf TASK-20261101-005.

## 境界

`DurableFile` is a lower-level checksummed record writer/reader used by monetary journal and snapshot components. Existing `phase6-journal-root-integrity.md` requires a symlink-free issuance journal root and ancestors at its caller boundary. This Draft asks whether the lower-level record primitive must also reject a symlinked ancestor independently, so that future callers cannot silently redirect durable monetary records.

## 観測と期待

| Vector | Input | Expected observation | Status |
| --- | --- | --- | --- |
| SC-DURABLE-PATH-01 | `write` target's existing parent is a symlink to another directory | `IOException` before any record is created through the link | Draft, Red candidate |
| SC-DURABLE-PATH-02 | `read` target has a symlinked ancestor, while final file is regular | `IOException` instead of accepting redirected record | Draft, Red candidate |
| SC-DURABLE-PATH-03 | Ordinary real nested directory and record | Write/read round-trip preserves exact payload and checksum | Regression |

## Scope and unresolved decisions

- This is **not** a substitute for the caller's authoritative root validation, atomic file moves, file locks or coherent world snapshot. Path checks before I/O are best-effort and do not prevent concurrent ancestor replacement.
- The scope of a generic `DurableFile` ancestry guard versus caller-owned trusted-root policy requires normative review; the tests may remain intentionally Red until the Process/PI/TCE is confirmed and formal CAT machine Gates pass.
- No production implementation, journal replay, real-server validation, develop/main merge or release is authorized by this Draft alone.
