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
| ci | passed | [Real Paper #43881696160](https://github.com/surumeneco/SimpleCoins/actions/runs/43881696160); [Domain #43881695984](https://github.com/surumeneco/SimpleCoins/actions/runs/43881695984); [Phase6 #43881696118](https://github.com/surumeneco/SimpleCoins/actions/runs/43881696118), tested HEAD `e2c6831b51d6be1366a57efe95e460149f812de7` (ordinary CI, not CAT formal Gate; final-head recheck pending) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Accepted requirement and bounded test

Drive 02B requires a globally coherent settlement generation that cannot silently mix values from different time points. An empty player set does not freeze Chunk mutations; world data may be modified by the game or another plugin. The prior PR #460 measured the same-tick scenario with a physical coin Chest and unchanged disk region: a genuine loaded Chest goes from two to three 5000-value items, but disk SHA256 and mtime do not change before an explicit save.

This Work tests the next, more specific boundary with **the product's actual LIVE source-capture API**. In the third isolated official Paper JVM, first confirm the saved Chest contains two 5000-denomination coins and acquire a real `PaperCapturedHybridSettlement.capture(List.of(world), generous bounded budget)`; assert that the product LIVE report includes the true 10000 value and that the captured physical money slot is present. Then mutate only the real live Chest to three valid physical coins **in the same Paper-main-thread callback**, while also independently checking the saved MCA SHA256 and mtime are unchanged. Obtain a second distinct genuine LIVE capture: it must total 15000 and contain the same source key with value 15000. The first capture MUST retain value 10000 and its original physical slot, since it is immutable and not a source-freeze token. All three observations must show no connected players and default-deny remains intact.

This is **issue-work technical verification**. The acceptance is NOT to make a stale candidate publishable or to install a production monitor in this Work; the expected observation is that identical source set and file witness can coexist with differing LIVE amounts. Do not infer that source set checks alone prove monetary generation consistency. Any production freeze/revalidation technique must be separately specified, legitimately authorized and verified; this run does not settle how to handle external-plugin writers, concurrency, Paper save timing or full G0. Keep the existing 3 JVM test and all previous assertions; change only isolated probe, its CI marker and this Work. No admission toggle, release, network player or real server changes. Normative CAT Gates remain not-run while Process/PI/TCE are Draft.

## Evidence: original actual Paper source capture (2026-11-16)

- Pinned CAT-TDD `896cae8313a533a3c74073a8c3150daad314ff90` Work structural validator returned `contract_valid:true`, `status:passed`; this is not Process Closure or any formal model Gate. Host pinned CAT ZIP SHA256 `6358c632cd56bbb85d03c4af8699ffbabaea2f6387c31c42a6cc1a6406a10785` was separately staged and authenticated in Chat with all 96 files and core tree `8d9049a0d0d2b732772329ba869f983f241868dd` (host staging only).
- First two official Paper 26.2 build 134 JVMs saved a real PDC-identified 5000-denomination chest stack of two coins and reloaded it in a **third independent Paper JVM**, with loopback-only server and zero connected users.
- Inside **one primary-thread scheduled callback** before any explicit world save, the production `PaperCapturedHybridSettlement.capture` independently called the actual `PaperLiveUniverseAssets.scanWithSources` on the loaded World, observing the specific `world/world/32,120,32:0` value 10000.
- The test mutated that real live Chest to three 5000-value coins; a second real `PaperCapturedHybridSettlement.capture` observed the **identical physical source key** value 15000. The first immutable capture still reported 10000. This is an observed time-of-capture divergence, not a forged snapshot.
- The same scheduled callback SHA-256-hashed the real disk `world/dimensions/minecraft/overworld/region/r.0.0.mca` and read its mtime both before and after LIVE mutation; **both remained byte/time identical**. Explicit marker in [real Paper run #43881696160](https://github.com/surumeneco/SimpleCoins/actions/runs/43881696160): `PAPER_A01_PRODUCT_LIVE_CAPTURE_VALUE_DRIFT_OBSERVED originalCapture=10000 freshCapture=15000 immutableSourceKey=true diskShaUnchanged=true onlinePlayers=0`, together with older `PAPER_A01_LIVE_VS_SAVED_MONETARY_DIVERGENCE_PASSED savedCoinsAtLoad=2 liveCoinsNow=3 diskShaUnchanged=true diskMtimeUnchanged=true onlinePlayers=0`.
- [Domain #43881695984](https://github.com/surumeneco/SimpleCoins/actions/runs/43881695984), [Phase6 #43881696118](https://github.com/surumeneco/SimpleCoins/actions/runs/43881696118), and Paper all passed on exact tested feature HEAD `e2c6831b51d6be1366a57efe95e460149f812de7`. The additional Work evidence commit changes HEAD, so final merged HEAD must be checked anew.
- **Technical inference strictly bounded to this evidence**: source-file checksums and loaded-Chunk/source-id closure can all remain stable despite a real changed LIVE monetary quantity. A genuinely coherent full G0 capture therefore needs an additional prevention or post-capture semantic validation / event-generation replay mechanism, not just static source IDs or disk fingerprints. Existing methods already state this lack of freeze explicitly; this Work does not introduce a runtime block, change production settlement, validate every external plugin, or publish accounting. Default-deny remains.
