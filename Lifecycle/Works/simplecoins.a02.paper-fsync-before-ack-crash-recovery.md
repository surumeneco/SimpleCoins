---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-fsync-before-ack-crash-recovery
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/483
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

# A02/G3 — 実Paper playerdata同期完了後・台帳ACK前の非正常終了とACK-only復旧

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/468 |
| https://github.com/surumeneco/SimpleCoins/pull/473 |
| https://github.com/surumeneco/SimpleCoins/pull/483 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-fsync-before-ack-crash-recovery | simplecoins.a02.paper-playerdata-fsync-abrupt-recovery;simplecoins.a02.paper-invsee-actual-offline-writer |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134, Java 25, real CraftPlayer.saveData(), Linux FileChannel.force(true), true compressed UUID.dat, SimpleCoins GrantLedger, 2 independent JVMs |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-fsync-before-ack-crash-recovery.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-fsync-before-ack-crash-recovery.yml | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-fsync-before-ack-crash-recovery.yml |

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

## Rationale

[#483](https://github.com/surumeneco/SimpleCoins/pull/483) showed the ordering playerdata save → file and parent directory fsync → fresh product file readback → durable ACK → Runtime.halt(137) → cold restart, but did not isolate **the crash window just before ACK** when the playerdata and receipt are already persisted. [#468](https://github.com/surumeneco/SimpleCoins/pull/468) exercises that scenario without explicit force synchronization. This Work tests the missing ordering intersection using genuine Paper 26.2 and the real SimpleCoins persisted ledger.

Two independent JVMs start on the same newly created temporary Paper world. JVM #1 reserves 5000C, saves original unrelated STONE and a new coin + receipt in playerdata (storage index 1), calls file and parent-directory `FileChannel.force(true)`, requires fresh production saved-file readback to prove the same physical coin and receipt, then checks the original reservation is **pending** and terminates via `Runtime.halt(138)` **before any ACK**. The workflow requires exit code 138; a graceful shutdown must not accidentally pass.

JVM #2 boots the SAME world and real product journal. It must verify **exactly one genuine pending reservation**, its recorded sequence/recipient/amount, matching saved receipt (complete, not active) and physical coin (5000C), unrelated stone still present, then reconstruct the candidate grant plan from the known originally vacant slot 1 and use production saved-root `SavedGrantDeliveryReadback.verify`. No new coin is materialized or `saveData` called. It then ACKs this existing reservation once, reopens the persisted journal, confirms delivered=true/pending empty and unchanged physical saved coin+receipt; it must also verify default-deny admission is still active. A second ACK attempt must be idempotent without changing physical bytes.

Do not change product behavior, `main`, admissions or real VPS. The previous 3 InvSee++ variants and #483's post-ACK crash flow must remain green; this Work is independent `issue-work` verification.

## Interpretation

A passing result proves an **isolated real Paper filesystem-to-journal ordering** can recover ACK without creating duplicate money when JVM halts after an explicit file/directory force but before ACK. It does **not** imply a kernel crash or power-loss guarantee, automatic game reconnect delivery, protection against a writer changing the saved root between inspection and ACK, or a complete end-to-end G3 coordinator. Recovery with a known empty baseline is not sufficient to recover every arbitrary partially delivered reservation. CAT Process/PI/TCE remain Draft and no normative Gate or production release is authorized.

## Acceptance

- 1st real JVM: valid playerdata coin+receipt, real file and directory forced, independently verified, reservation still pending, abnormal exit 138 before journal ACK.
- 2nd JVM: exact previous grant is still pending, saved 5000C and receipt retained, no physical re-materialization, single durable ACK recovered, reread stable, guard closed.
- Prior post-ACK fsync #483 CI and InvSee regressions pass; exact-head CI and readback verified.
