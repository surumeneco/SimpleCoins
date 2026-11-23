---
cat_version: '0.2'
kind: work
id: simplecoins.a02.atomic-persisted-ack-lease
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/636
  - https://github.com/surumeneco/SimpleCoins/pull/637
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.protected-saved-ack-bridge
  - simplecoins.a01.atomic-sealed-publication
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — actual same-seal exclusion across slow saved readback and durable ACK

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/636 |
| https://github.com/surumeneco/SimpleCoins/pull/637 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.atomic-persisted-ack-lease | simplecoins.a02.protected-saved-ack-bridge;simplecoins.a01.atomic-sealed-publication |

## Technologies
| Technology |
| --- |
| Java25 JUnit; compressed real saved playerdata, synchronized GrantLedger; existing WriterEpochFence; Paper 26.2 contract; canonical CAT runtime 896cae8313a533a3c74073a8c3150daad314ff90 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.atomic-persisted-ack-lease.md;src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStage.java;src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;src/test/java/co/surumene/simplecoins/domain/WriterEpochFenceTest.java;.github/workflows/a02-atomic-persisted-ack-lease.yml | src/main/java/co/surumene/simplecoins/domain/WriterEpochFence.java;src/main/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStage.java | src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java;src/test/java/co/surumene/simplecoins/domain/WriterEpochFenceTest.java;.github/workflows/a02-atomic-persisted-ack-lease.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a02.atomic-g3-ack-lease | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.paper.SavedGrantProtectedAckStageTest | resumptionCannotEnterBetweenFreshReadbackAndDurableAck;successfullyFsyncsFreshProofAndAcksWithinOneSeal;refusesMissingPhysicalCoinOrChangedStorage | src/test/java/co/surumene/simplecoins/paper/SavedGrantProtectedAckStageTest.java | build/test-results/test/TEST-co.surumene.simplecoins.paper.SavedGrantProtectedAckStageTest.xml | 300 |

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

## Existing approved meaning and observed defect in bounded bridge

The user-confirmed A02 Process/PI/TCE and 02B/04 mandate that the same protected saved playerdata generation remains exclusive from the actual saved physical money + signed PDC receipt readback **through durable ACK**, and that a changed or unproven writer rejects the delivery. PR #636 introduced a real, package-private product assembler, but it uses repeated `WriterEpochFence.requireDrained(seal)` **point-in-time checks**. A concurrent `WriterEpochFence.resume(seal)` can occur while that assembler performs a real readback, temporarily reopening root admissions and violating the interval, even if the next stale-seal check eventually aborts.

This scoped correction is a **proven boundary fix**, not a new grant rule. Add `WriterEpochFence.withDrainedLease` to establish an **exclusive, active, thread-owned seal lease** while potentially blocking bounded file IO is performed *without holding the monitor*. A concurrently attempted `resume` must fail immediately rather than reopening OPEN; `enterRoot` must remain rejected. The lease cannot be nested, stolen, or used after a fault. Validate the exact seal and zero active writers before and after the callback. Any exception faults the fence, and the active lease is always removed. Allow only its owning thread to execute the preexisting **short synchronized** `withDrainedSeal` final step while the lease is active, to prevent `invalidate` racing *during* the durable `GrantLedger.acknowledge`. The final short synchronized step may run the journal's real durable write; this is a deliberately narrow, off-main-thread G3 exception to the G0 publication-only callback rule, not a path to arbitrary blocked IO under the monitor. Prohibit a `resume` from inside the same lease as well.

Update the existing internal `SavedGrantProtectedAckStage.finish` to place the current real origin-backed UUID.dat witness, file+directory force, exact physical+receipt readback and final durable ACK in one *continuous* `withDrainedLease` callback, with only the authentic single-batch complete grant accepted. Take a final generation/ledger integrity check before the protected durable ACK, which must run under `withDrainedSeal` on the same lease owner. If the ACK write partially commits then fails, only cold recovery can decide outcome; never reissue blindly. No public entrypoint or fake file proof may authorize this stage, and the runtime Agent still lacks a proven complete native/Plugin/async/direct-file writer roster.

## Deterministic Red/Green evidence and acceptance

Use real compressed UUID.dat and GrantLedger on disk, with an `AbstractMap` proxy around the legitimate pre-save NBT root to hold the reader **inside actual `SavedGrantDeliveryReadback.verifyTransition`** after it has passed file/dir force. In another thread call `fence.resume(seal)`. The current product method erroneously allows OPEN while readback is pending: the new JUnit must fail for that reason (not a missing class/CI setup). Green must establish `resume` throws immediately during the protected IO, `enterRoot` denied, release the proxy and observe exactly one real durable ACK and genuine saved 5000C+receipt, SEALED and zero in-flight. Other negative paths retain pending/faulted semantics. Add bounded domain test for lease release after success/fault and prevent nested leases.

Run A02 CAT Process Closure and Model Conformance against same Confirmed inputs; run full Java25 root regression, existing A01 fence CI, Domain and Phase6 CI on exact PR head and squash merge on success. This Work does **not** activate normal startup, Paper give, G0/G3 global authority, actual installed writer closure, G3 crash-wide exactly-once or Admission. `main`, VPS, release unchanged.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-atomic-lease-model.json | @work/.cat-flow/cache/a02-atomic-lease-vectors.json | @work/.cat-flow/cache/a02-atomic-lease-queue.json | — | — | @work/.cat-flow/cache/a02-atomic-lease-obligations.json | — | false |
