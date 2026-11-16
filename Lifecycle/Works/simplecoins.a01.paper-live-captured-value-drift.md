---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-live-captured-value-drift
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/460
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

# A01/G0 — 本物のPaper LIVE資産取得後の値ドリフトと保存ファイル不変

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/460 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperLiveUniverseAssets.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperLoadedWorldAssets.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.paper-live-captured-value-drift | simplecoins.a01.real-paper-live-saved-divergence;simplecoins.a01.zero-player-external-writer-probe |

## Technologies

| Technology |
| --- |
| Genuine Paper 26.2 build 134, Java 25, loaded Chunk with real Chest and physical coin PDC, production PaperCapturedHybridSettlement.capture, actual Anvil SHA-256/mtime witness |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-live-captured-value-drift.md;tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java;.github/workflows/paper-zero-player-writer-probe.yml | — | tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java;.github/workflows/paper-zero-player-writer-probe.yml |

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

## Accepted requirement and bounded test

Drive 02B requires a globally coherent settlement generation that cannot silently mix values from different time points. An empty player set does not freeze Chunk mutations; world data may be modified by the game or another plugin. The prior PR #460 measured the same-tick scenario with a physical coin Chest and unchanged disk region: a genuine loaded Chest goes from two to three 5000-value items, but disk SHA256 and mtime do not change before an explicit save.

This Work tests the next, more specific boundary with **the product's actual LIVE source-capture API**. In the third isolated official Paper JVM, first confirm the saved Chest contains two 5000-denomination coins and acquire a real `PaperCapturedHybridSettlement.capture(List.of(world), generous bounded budget)`; assert that the product LIVE report includes the true 10000 value and that the captured physical money slot is present. Then mutate only the real live Chest to three valid physical coins **in the same Paper-main-thread callback**, while also independently checking the saved MCA SHA256 and mtime are unchanged. Obtain a second distinct genuine LIVE capture: it must total 15000 and contain the same source key with value 15000. The first capture MUST retain value 10000 and its original physical slot, since it is immutable and not a source-freeze token. All three observations must show no connected players and default-deny remains intact.

This is **issue-work technical verification**. The acceptance is NOT to make a stale candidate publishable or to install a production monitor in this Work; the expected observation is that identical source set and file witness can coexist with differing LIVE amounts. Do not infer that source set checks alone prove monetary generation consistency. Any production freeze/revalidation technique must be separately specified, legitimately authorized and verified; this run does not settle how to handle external-plugin writers, concurrency, Paper save timing or full G0. Keep the existing 3 JVM test and all previous assertions; change only isolated probe, its CI marker and this Work. No admission toggle, release, network player or real server changes. Normative CAT Gates remain not-run while Process/PI/TCE are Draft.
