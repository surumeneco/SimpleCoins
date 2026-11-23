---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-g1-staged-seal-publication
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/639
  - https://github.com/surumeneco/SimpleCoins/pull/600
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a01.saved-staged-publish-bridge
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 / G1 — actual Paper BlockLocker + WWC/WWW + BAN source stage into bounded protected Snapshot

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/639 |
| https://github.com/surumeneco/SimpleCoins/pull/600 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.paper-g1-staged-seal-publication | simplecoins.a01.saved-staged-publish-bridge;simplecoins.a05.saved-wcl-source-stage |

## Technologies
| Technology |
| --- |
| Java25; genuine Paper 26.2 build134; distributed BlockLocker 1.15, WWC2.1.1, WWW1.1.2, WCL/WGL, actual saved Anvil, playerdata, Paper profile BAN; test-only independent blocked AdmissionBarrier and fenced final publisher |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-g1-staged-seal-publication.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a01.real-paper-g1-stage | verification | external | @repo:SimpleCoins | github-actions-paper-wcl-services-probe | PAPER_A01_G1_REAL_SAVED_STAGE_ATOMIC_PUBLISH_BOUNDED_PASS | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java | Actions run logs | 720 |

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

## Existing product contract and bounded test purpose

The genuine G1 source stage implemented in the existing `PaperCapturedHybridSettlement.stageCandidateWithCapturedBlockLockerAndSavedWclAndBanCoverage` already emits a `SettlementScanRunner.Staged` under the same physical saved Manifest, WCL money/owner, BlockLocker chest sign, actual profile BAN, and preserved `WorldSnapshotTraversal.ContentWitness`. PR #639 wired that Staged type to the package-private, currently unregistered product `SettlementProtectedPublisher.evaluateAndPublishStaged`, but its positive Java test included only actual UUID.dat money and no real Paper/WWC/WWW/BlockLocker context. This independent verification exercises that product link against the **existing real Paper distributed 56,000C fixture** rather than adding another artificial source or another production wrapper.

In the existing `WclServicesProbe.verifyWholeHybridSavedWclCandidate` after physically constructing the genuine persisted `Staged`, when and only when a dedicated `-Da01.probe.realSourcePublish=true` test flag is set for the `reload-saved` phase, use reflection (because the product class is intentionally package-private) to construct an isolated `WriterEpochFence` and **separate test-only** `AdmissionBarrier`, arm then seal the fence, and invoke `evaluateAndPublishStaged` with the real staged source, the **same exact** Manifest and test-only all-TRUE A01 evidence. Verify the returned verdict, physical P/F/D values and source/ContentWitness again; verify post-fault that `current()` becomes empty, gate becomes FAILED, and the product's real registered Admission Guard **remains denied**. The arbitrary TRUE evidence is **solely an isolated downstream integration test fixture**; it does not establish the installed writer roster, active normal journal handoff, IP BAN UUID mapping, or universal world source closure and MUST NOT be connected to startup.

Keep old `verifyCompleteUnpublishedWclCandidate` asserts (all values/slots, wallet duplicates and BAN/BlockLocker negative tests) by refactoring it to inspect either the local unregistered Result or the newly protected Result, without a second conflicting `commit()` call on the same `Staged`. The separate `smoke` baseline 55,000C and `smoke-blocklocker` 56,000C real-distribution JVM cycles should both emit an explicit **bounded-only** marker, and CI must fail if absent in `reload-saved` logs. No marker in production or in user-facing client.

Acceptance requires exact pinned current XPlayServer packaged JAR blobs, real official Paper build134, zero players, real Ban/BlockLocker/WCL fixtures, true 25,000C personal/30,000C dead/1,000C free in the BlockLocker profile-BAN case and 50,000C dead/5,000C free in the baseline. All other existing Paper markers must remain Green; separate CAT Work validation must be checked, but this independent `issue-work` verification claims no new normative closure, TestModel, implementation-conformance or actual G0 authority.

**Do not turn this test into production code or register the internal publisher:** global installed Java/Bedrock, Plugin/NMS/direct file/async writer closure and full A01/G0/G3 remain blocked and cannot be inferred from this test-only fence, even when all outputs match. No `main`, VPS, release, actual published statistics or Admission enabling.
