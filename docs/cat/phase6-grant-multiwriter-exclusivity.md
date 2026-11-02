---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.grant.cross-process-writer-exclusivity
status: draft
process: simplecoins.grant
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
  - https://github.com/surumeneco/SimpleCoins/pull/10
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 6 — issuance journal cross-process writer exclusivity (CAT Draft)

## Scope and decision status

The existing `GrantLedger` calls itself a **single-writer issuance journal** and checks durable head, tail and physical root identity before operations. Those checks do not by themselves constitute an atomic inter-process exclusion protocol: two instances could pass their independent pre-write checks before either commits a new sequence. This document records a **candidate contract and test design**, not proof that a collision has occurred, not a confirmed need for multiple JVM writers, and not permission to modify production code.

The decision between a caller-enforced exclusive server lifecycle and a journal-owned operating-system lock remains **open**. The requirement to avoid double issuance and silent loss applies regardless of that choice.

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.grant.single-writer | Exclusive authority for a durable issuance journal root across processes or explicitly verified host ownership |
| simplecoins.grant.recovery | No silent overwrite or duplicate sequence after competing writers or abnormal termination |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| GRANT-WRITER-01 | Two independent live journal instances target the same physical root | At most one instance may reserve/acknowledge; the other fails closed before any conflicting write | GW-01 |
| GRANT-WRITER-02 | A second process opens the same journal while the first owns its writer authority | Reject or wait under an explicitly bounded policy; never infer exclusivity from an in-process synchronized method | GW-02 |
| GRANT-WRITER-03 | The active writer terminates and the durable journal is intact | A new instance may recover authoritative head and records without losing or duplicating grants | GW-03 |
| GRANT-WRITER-04 | The previous writer's outcome is ambiguous or the root identity changed | Require authoritative replay/reconciliation before further issuance | GW-04 |
| GRANT-WRITER-05 | Single verified writer performs ordinary sequential reservations | Preserve normal monotonic sequence, checksum and receipt behavior | GW-05 |

## Test vectors

| ID | Setup | Required result |
| --- | --- | --- |
| GW-01 | Two independent JVMs or classloader-isolated ledgers, with a deterministic barrier before conflicting sequence writes | Exactly one durable reservation; no overwritten sequence or two successful distinct grants with one durable record |
| GW-02 | Writer A holds authority while writer B attempts to initialize or reserve | B cannot issue until ownership is safely transferred |
| GW-03 | A exits or crashes after a durable record but before head/acknowledgement; B restarts | B replays the durable state and applies the existing crash recovery contract once |
| GW-04 | Root inode or durable head changes during ownership transition | Fail closed, require authoritative restart/reconciliation |
| GW-05 | One writer reserves and acknowledges normally | Existing successful flow unchanged |

## Existing evidence and unresolved design

At PR #10 HEAD `e293a7b9f9ebaf1ad5559d45c0204046e1d04494`, `GrantLedger` uses per-instance synchronization, a root file-key check and durable head/tail checks; its constructor does not acquire an operating-system lock. This is **static code evidence**, not a reproduced two-writer collision. Sequential second-writer access can already detect changed durable head; only an overlapping check/write window is the unresolved concurrency case.

A deterministic multi-process fault-injection test must be designed before deciding whether the correct fix is an OS file lock, an explicit caller-owned single-writer lifecycle, or another proven exclusion mechanism. Avoid timing-only flaky concurrency tests. Existing Drafts for journal root, durable symlink boundaries, non-tail receipts and snapshot publication remain separate.

Status: Draft, not Confirmed. Formal CAT Process/PI/TCE, machine Gates, concurrent JVM fault injection, Paper live acceptance and production change are **not-run**. This Draft must not be used to assert that inter-process safety has already been implemented.
