---
cat_version: '0.2'
kind: work
id: simplecoins.a02.advisory-playerdata-lock-invsee
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/478
  - https://github.com/surumeneco/SimpleCoins/pull/483
  - https://github.com/surumeneco/SimpleCoins/pull/484
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A02/G3 — playerdata実ファイルに対するOS advisory lockの排他有効性

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/475 |
| https://github.com/surumeneco/SimpleCoins/pull/478 |
| https://github.com/surumeneco/SimpleCoins/pull/479 |
| https://github.com/surumeneco/SimpleCoins/pull/483 |
| https://github.com/surumeneco/SimpleCoins/pull/484 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.advisory-playerdata-lock-invsee | simplecoins.a02.paper-fsync-before-ack-crash-recovery;simplecoins.a02.paper-invsee-actual-offline-writer |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134, Java 25, real pinned InvSee++ 0.31.22 JAR, native Linux advisory FileLock via FileChannel.lock(), compressed UUID.dat, original saved grant proof and journal |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.advisory-playerdata-lock-invsee.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-invsee-filelock-bypass.yml | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-invsee-filelock-bypass.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Problem

[#475](https://github.com/surumeneco/SimpleCoins/pull/475) and [#478](https://github.com/surumeneco/SimpleCoins/pull/478) prove a valid persisted 5000C receipt+physical coin pair may be changed by an unrelated InvSee++ offline save after proof and before durable ACK; blind ACK can then permanently lose money. [#479](https://github.com/surumeneco/SimpleCoins/pull/479) proves a cooperative InvSee++ save event veto for one UUID, but not all Paper writers. [#483](https://github.com/surumeneco/SimpleCoins/pull/483) and [#484](https://github.com/surumeneco/SimpleCoins/pull/484) prove fsync and crash ordering on the selected Paper data file; they do **not** prevent concurrent writer replacement. Could a `FileChannel.lock()` on the verified playerdata's existing inode protect that same path against another plugin's save?

## Bounded experiment

Use a new disposable loopback-only Paper 26.2 server with actual pinned InvSee++ and original product/independent guard. First create a genuine offline UUID.dat with one unrelated STONE, one fully saved 5000C grant and matching signed receipt. Obtain a positive production saved-file readback while the ledger remains pending. Then open **the actual saved file**, acquire a blocking exclusive OS advisory `FileLock` and retain the channel and lock throughout **a real invocation of InvSeeAPI.saveInventory** on the same UUID, which removes the coin but retains unrelated STONE and receipt. Require a genuine `SaveResponse.saved`; if the save is blocked or fails, explicitly report that result rather than falsely claiming bypass.

Immediately after the save response, and **before releasing the original lock**, verify (1) the old lock remains valid; (2) a fresh read of the authoritative path has lost the coin while keeping the old receipt; (3) a fresh product saved-file proof rejects the stale physical grant; (4) journal reservation remains unACKed; and (5) optionally record whether the filesystem path fileKey/inode changed, without requiring rename as the only mechanism. No ACK is executed. Release lock and close channel before shutting down this test JVM.

**Interpretation:** If the saved path changes despite the held old-file lock, an advisory lock on the pre-save UUID.dat inode is **not** an effective writer exclusion protocol against the deployed InvSee++ save path. It cannot alone supply A02's `保存世代証拠有効` or A01's LIVE/saved immutable epoch. This is not proof that no higher-level cooperative lock (directory-level barrier, scheduler fencing, service-owned lease) can work. It is not an operator deployment and never opens admissions.

## Acceptance

- Exact pinned Paper/InvSee++ JAR, existing valid 5000C saved receipt/coin proof, exclusive FileLock held before the InvSee++ API save.
- A real InvSee save returns success while lock remains valid, but saved authoritative UUID.dat no longer contains physical money and still has the original receipt.
- Fresh readback refuses stale money proof, ledger remains pending, no false ACK or new coin, and guard stays default-deny.
- The existing #478/#479/#480 and #483/#484 tests remain green on their own CI jobs.
- Readback and CI success recorded as issue-work evidence only, not CAT normative Gates or product readiness.
