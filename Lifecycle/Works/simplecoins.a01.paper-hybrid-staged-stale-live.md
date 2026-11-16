---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-hybrid-staged-stale-live
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/470
  - https://github.com/surumeneco/SimpleCoins/pull/462
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

# A01/G0 — 実Paperの古いLIVE取得値を含むHybrid決算候補の保存証拠境界

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/470 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.paper-hybrid-staged-stale-live | simplecoins.a01.paper-live-captured-value-drift;simplecoins.a01.real-paper-live-saved-divergence |

## Technologies

| Technology |
| --- |
| Real Paper 26.2 build 134; Java 25; production hybrid staged settlement and manifest; existing physical Chest amount + same-tick mutable live/disk divergence |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-hybrid-staged-stale-live.md;tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java;.github/workflows/paper-zero-player-writer-probe.yml | — | tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java;.github/workflows/paper-zero-player-writer-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | ---: |

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

## Already-decided guarantee and acceptance

Drive 02B requires a globally coherent monetary accounting generation; the third Paper JVM in #470 demonstrated that a real physical currency Chest changes its LIVE value from 10000 to 15000 without changing the saved MCA file SHA256/mtime or the captured physical key. The product `PaperCapturedHybridSettlement` explicitly documents that its `Observed` values are captured once and never re-read after saved NBT staging, and that its staged candidate cannot by itself authorize accounting publication.

This Work answers the next narrower technical question: whether a bona fide **old real Paper LIVE capture** can be passed after a live-only monetary mutation into the existing production `PaperCapturedHybridSettlement.stageCandidate` as a complete **but unpublished** `SettlementScanRunner.Staged` candidate despite unchanged saved monetary source fingerprints. Reuse the third JVM already running in #470. After observing original valid two-coin LIVE capture and mutating the real Chest to three real PDC coins in the same primary-thread callback, discover the authoritative actual Paper 26.2 dimension region and shared playerdata roots through unmodified production `WorldStorageDiscovery.discover`. Call the **actual product** `stageCandidate` with this pre-mutation capture, exact source manifest and permissive free-ownership callbacks suitable only for a private, physically unprotected empty-world fixture. Assert the candidate's value remains 10000 while a fresh actual LIVE capture reports 15000, that the saved content witness remains unchanged, and that the staged candidate cannot be interpreted as a publishable snapshot. Verify the originally empty player set, source identities and default-deny admission remain unchanged.

The experiment must not create a production code path, modify any real user's inventory, alter world settlement ownership, relax admission or report a formal CAT Gate. The expected mismatch is a reproducible shortcoming of uncoordinated memory/disk generations, NOT authorization to commit the candidate to the product publication cache. A failure due to other unsupported saved source kinds is also a technical finding; it must not be suppressed by swapping to a forged empty Manifest or by silently ignoring incomplete files.
