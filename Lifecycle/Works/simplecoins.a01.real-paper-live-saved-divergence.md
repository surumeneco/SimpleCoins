---
cat_version: '0.2'
kind: work
id: simplecoins.a01.real-paper-live-saved-divergence
status: draft
process: simplecoins.settlement.source-generation
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/435
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

# A01 — 実PaperでLIVE貨幣のみ変化し、保存ファイル証拠が不変な状態を観測

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/435 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.source-generation | simplecoins.a01.real-paper-live-saved-divergence | simplecoins.a01.zero-player-external-writer-probe |

## Technologies

| Technology |
| --- |
| Java 25; official Paper 26.2 build 134; authentic ItemStack/PDC; live Chest; on-disk Anvil region; SHA-256 |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.real-paper-live-saved-divergence.md;tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java;.github/workflows/paper-zero-player-writer-probe.yml | — | tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java;.github/workflows/paper-zero-player-writer-probe.yml |

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43829044370 (bounded observed Paper test HEAD `d156723`; formal CAT Gates not-run) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Bounded objective and acceptance

- The existing 02B requirement demands an authoritative source generation when LIVE Paper state and saved NBT coexist; its accepted warning says no-player admission alone does not suspend world writers. This Work measures a new, narrower counterexample rather than changing that requirement.
- Reuse the independent, no-network-player, three-JVM real Paper trial from #435. In the third (verify) JVM, confirm the authentic 5000-unit coin stack of count=2 persisted in the chest, with the expected actual Paper overworld region file present. Take SHA-256 of the full saved MCA and its last-modified time.
- On the Paper primary thread, **without calling world/chunk save**, have the test-only external plugin replace that loaded chest slot with the same authentic 5000-unit denomination, count=3. Require LIVE observation of count=3; immediately reread saved MCA bytes and modification time while the same synchronous event tick is still executing. Require the same checksum/time as before the LIVE mutation. Emit one dedicated marker before normal server shutdown can save that new in-memory state.
- This explicitly exhibits an interval where Paper's LIVE monetary authority differs from the unchanged saved-file witness. It is not a claim that a finalized snapshot can safely ignore LIVE state, and it does not prove disk durability of the transient count=3; Paper shutdown may subsequently save it.
- Keep original 3-JVM saved mutation/verification markers. Work only in disposable loopback CI worlds with admission denied; no real player, no main-line product code, no full G0 or CAT formal Gate. If autosaving or path identity makes the observation unrepeatable, reject the result; do not label it passed.

## Interpretation and decision boundary

- A stable file SHA/mtime, even when paired with source-discovery consistency, is evidence about **disk files**, not about currently loaded inventories or external plugin writers. Mixing those values without one established transition/commit epoch cannot prove G0.
- Whether to use a writer barrier, coordinated generation protocol, guarded startup/reconciliation, or a different mode remains a design decision with unapproved CAT Process/PI/TCE. This isolated verification does not choose it.

## Observed official Paper 26.2 result

- Source/CI head `d1567233be007e0eabac9ebf8960f90c1e89a870`: the actual third JVM reloaded an authentic 5000-unit paper coin stack with count 2 from the previously saved physical Paper region. The loopback server kept admission default-deny and had zero players. The independent Java 25 / official Paper build 134 trial [#43829044370](https://github.com/surumeneco/SimpleCoins/actions/runs/43829044370) succeeded; Domain [#43829044742](https://github.com/surumeneco/SimpleCoins/actions/runs/43829044742) and Phase6 [#43829044575](https://github.com/surumeneco/SimpleCoins/actions/runs/43829044575) also succeeded at that HEAD.
- Inside a single scheduled main-thread callback before any explicit save/shutdown, the test-only plugin changed that LIVE chest to three authentic coins. The LIVE check returned count=3, while the actual MCA file SHA-256 and last-modified time were identical before and after the LIVE change.
- Exact witnessed line: `PAPER_A01_LIVE_VS_SAVED_MONETARY_DIVERGENCE_PASSED savedCoinsAtLoad=2 liveCoinsNow=3 diskShaUnchanged=true diskMtimeUnchanged=true onlinePlayers=0`.
- This demonstrates **loaded-memory asset drift not represented by stable disk-side metadata/content witness** even with zero logged-in players. No assumption is made about how Paper's subsequent normal shutdown wrote that transient state. The trial does not show all-world consistency or guarantee general freeze/quiescence. It does not choose an A01 G0 architecture or establish CAT semantic approval.
