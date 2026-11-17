---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-empty-pause-writer-epoch
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
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

# A01/G0 — Paper無人時pauseと非協調filesystem writerの実測

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/435 |
| https://github.com/surumeneco/SimpleCoins/pull/471 |
| https://github.com/surumeneco/SimpleCoins/pull/472 |
| https://docs.papermc.io/paper/vanilla/ |
| https://jd.papermc.io/paper/26.2/org/bukkit/Server.html |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.paper-empty-pause-writer-epoch | simplecoins.a01.live-writer-coherence-model;simplecoins.a01.real-paper-live-saved-divergence;simplecoins.a02.verify-to-ack-writer-gap |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; pause-when-empty-seconds; Bukkit synchronous tick scheduler; isolated java.lang.Thread filesystem writer; FileChannel.force(true); CAT-TDD 896cae83 issue-work verifier |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-empty-pause-writer-epoch.md;tests/paper-empty-pause-probe/*;.github/workflows/paper-empty-pause-writer-probe.yml | — | tests/paper-empty-pause-probe/*;.github/workflows/paper-empty-pause-writer-probe.yml |

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

## Technical question and scope

The approved 02B/05 requirements demand a consistent LIVE + SAVED monetary generation while users are blocked and all relevant writers are fenced through publication / grant ACK. Actual Paper #435/#470/#471 establish that merely zero online players does **not** prevent third-party plugin writes. The next credible source of quiescence to evaluate is Paper's server-side no-player pause, not more timer/file fingerprint heuristics.

Paper's public `Server.setPauseWhenEmptyTime(int)` contract says values **less than 1 disable** the setting; `pause-when-empty-seconds=1` is positive, unlike `-1` (disabled). The official Paper release notes explicitly warn that its no-player pause is disabled by default and can be incompatible with plugins; it must not be adopted as an all-writer guarantee without measurement.

## Verification strategy

Boot **two independent disposable official Paper 26.2 JVMs** with the same actual SimpleCoins product and independent default-deny entrance guard, both 127.0.0.1, whitelist, no connected players, no external provider plugins:

1. Negative/control JVM: `server.properties pause-when-empty-seconds=-1`; real API `Bukkit.getPauseWhenEmptyTime()` must report `-1`.
2. Candidate JVM: identical server with `pause-when-empty-seconds=1`, real API `Bukkit.getPauseWhenEmptyTime()` must report `1`.
3. A test-only Paper plugin attaches a main-thread `runTaskTimer` counter and captures actual world `getFullTime` **only on Paper main thread**; these data are snapshotted from an independent thread (never call Bukkit API on background thread). The independent thread also writes and `FileChannel.force(true)` to a disposable marker file under the server's test plugin folder every 250ms, validating bytes via normal file reads. It records before/after counters over the same wall-time interval long enough to cross the pause threshold, writes a forced evidence report and **exits the disposable JVM** via `Runtime.halt(0)` (the paused tick scheduler may not service Bukkit.shutdown). This must never be installed or run on a real server.
4. Compare the observed world-time/tick-counter deltas and independent writer-file writes. Fail rather than fabricate pause behavior. For any claimed pause, demand that the candidate's main-thread/world-time progress is clearly less than the control's; the independent filesystem writer must still advance **during candidate's pause**. A result showing only world ticks reduced, but scheduler continuing, is precisely recorded and not mislabeled as global freeze. A result showing no main-thread slowdown is evidence that the chosen configuration is not an effective pause in this setup.
5. Original SimpleCoins product and admission-guard JARs and saved playerdata are not modified. The original background marker is **not** a Minecraft asset writer; its activity proves an independent Java thread still writes under pause, but says nothing about a particular installed plugin changing playerdata. Tested source semantics and runtime concurrency are distinct.

## Acceptance / decision relevance

- Correctly validated official build SHA, independent 2 isolated worlds/JVMs, 0 players, entrance default-deny and genuine configuration values.
- Control world-time advancement shown in real Paper, positive pause world-time slowdown (if observed), independent background write/forced readback in both modes; report exact deltas and limits without inventing events.
- Actual pinned CAT `cat_flow.py validate` Work structure and 3 workflows (Paper, Domain, Phase6) green on the **same exact HEAD** before develop squash merge.
- Fail-safe decision: whether successful or not, `pause-when-empty-seconds` alone cannot be accepted as the complete G0/G3 writer epoch barrier while independently running JVM threads exist. The issue-work is a **technical verification**, not an approved product design, definitive third-party writer audit, formal CAT Gate or admission permit.

## Follow-up: actual Paper LIVE currency changes while world time is paused

The initial [PR #497](https://github.com/surumeneco/SimpleCoins/pull/497) measured `pause-when-empty-seconds=-1`: main-thread timer=166/world time=166/forced raw-file writes=40, versus `pause-when-empty-seconds=1`: main-thread timer=161/world time=0/raw-file writes=40. Thus a plugin main-thread scheduled task remains runnable even during a world-time pause. This follow-up exercises a genuine Minecraft asset, not just a raw text file.

In each **separate throwaway Paper JVM only**, create a real Chest in a chunk protected by a plugin chunk ticket, and insert exactly one real `CoinItemCodec` 5000C PDC/model PAPER coin before observation. Inside the existing scheduled main-thread callback, after approximately 80 callbacks, replace the Chest inventory slot with two otherwise identical authentic coins, verifying the source ItemStack through the actual product codec both before and after. Sample inventory count and `World.getFullTime()` **on the same Paper main thread** and pass only atomic immutable numbers to the evidence worker. The test must read exactly one authentic coin at early ~2.5-second capture, two at late ~10.5-second capture, and record the world time when the actual mutation happened. In the paused condition, require the first, mutation and last world-time samples to be **identical**; the control condition must show world time progressing. This establishes actual LIVE inventory mutation during frozen world time, not just the possibility of filesystem writes.

The Chest and coin values exist exclusively in disposable CI worlds. No live VPS, production assets, saved playerdata, release, admission or real user is touched. The observer still aborts the isolated JVM with `Runtime.halt` and must not be deployed. The fact that a Paper task can change LIVE currency with paused world time **does not** prove another installed plugin did so, nor that the monetary change is durable. The existing raw file writer remains an orthogonal same-JVM concurrency witness.

Require official exact-head Paper CI to verify both true product PDC identities and the real 1→2 slot difference while pause=true and `worldTimeDelta=0`, retaining independent writer 40+ observations, Domain/Phase6 regressions and the existing CAT Draft no-Gate boundary. Update the original Work rather than create a parallel requirements file.
