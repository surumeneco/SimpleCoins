---
cat_version: '0.2'
kind: work
id: simplecoins.a02.verify-to-ack-writer-gap
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/474
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

# A02/G3 — real compressed playerdata verify-to-ACK writer-gap counterexample

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/425 |
| https://github.com/surumeneco/SimpleCoins/pull/463 |
| https://github.com/surumeneco/SimpleCoins/pull/473 |
| https://github.com/surumeneco/SimpleCoins/pull/474 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.verify-to-ack-writer-gap | simplecoins.a02.paper-before-save-sigkill-recovery;simplecoins.grants.paper-save-commit |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; authentic GrantLedger journal files; compressed Minecraft playerdata NBT; bounded WorldStorageDiscovery; Paper coin/PDC codec; Github Actions Domain foundation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.verify-to-ack-writer-gap.md;src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadbackTest.java | — | src/test/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadbackTest.java |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| A02-WRITER-READBACK | verification | JUnit 5 | . | gradle --no-daemon test --tests co.surumene.simplecoins.paper.SavedGrantDeliveryFileReadbackTest | verifiedGrantCanBeAcknowledgedAgainstStaleReceiptWhenWriterRemovesSavedCoin;rollbackAfterProofStillFailsStartupAudit | verified develop and untouched test oracle | Domain foundation CI receipt | 300 |

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

## Bounded independent verification

Production A02 currently separates successful `SavedGrantDeliveryFileReadback.verify` from `GrantLedger.acknowledge(grant, sequence)`. The ledger API only authenticates the grant handle and validates receipt *sequence* against the journal. It cannot prove that a caller's previously verified `Proof` still corresponds to the current physical saved source.

Reproduce the exact *logical* writer race using real on-disk compressed NBT and the production classes: (1) persist a complete physical coin and matching receipt, (2) obtain a valid readback proof, (3) have an independent file writer overwrite the same UUID.dat with an otherwise valid matching receipt but **no issued coin**, (4) invoke the raw, previously existing journal ACK with the old verified sequence, (5) reboot the journal and run the startup saved-receipt universe audit and inspect saved physical assets. A successful test is **a negative, unsafe counterexample**, not proof of an approved implementation. This work does not claim a Paper JVM concurrent writer test or show a legitimate game event removing the coin; it demonstrates that if such a filesystem interleaving exists, the present two independent calls cannot preserve G3.

Compare with a second interruption that rolls back the *receipt* itself: the startup audit is expected to detect that mismatch, unlike an equal receipt with a lost physical coin. Do not change production code, open the admission guard, call `main`, or promote CAT Draft to Confirmed. The observed counterexample is not a formal CAT Gate. Preserve the separate unresolved decision on legal writer envelope, OS crash durability, main-thread/async boundary, and a genuinely atomic verify-to-ACK protocol.

## Acceptance

- The existing five after-save readback tests continue to pass; two new independent filesystem/witness tests pass on current code.
- The test proves an actual post-proof overwrite of the same UUID.dat and a persisted false-positive ACK that the startup *receipt-only* audit fails to notice; the assertion explicitly treats this as a **known dangerous gap**.
- The receipt-rollback contrast produces an IOException from startup audit.
- Exact feature HEAD CI and merged develop readback are recorded separately from normative confirmation and actual online Paper acceptance.
