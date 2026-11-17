---
cat_version: '0.2'
kind: work
id: simplecoins.a12.native-cartography-coin-input
status: draft
process: simplecoins.currency.station-consumption-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/pull/492
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

# A12 — 実NMS CartographyTableMenuで貨幣PAPERを通常紙として処理するか

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/489 |
| https://github.com/surumeneco/SimpleCoins/pull/490 |
| https://github.com/surumeneco/SimpleCoins/pull/491 |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.station-consumption-exclusion | simplecoins.a12.native-cartography-coin-input | simplecoins.a12.nms-merchant-itemcost-match;simplecoins.a12.paper-station-consumption-boundary |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; CraftItemStack/NMS ItemStack; genuine MapView/MapMeta; CartographyTableMenu; detached NMS ServerPlayer; fixed CAT-TDD 896cae83 issue-work validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.native-cartography-coin-input.md;tests/paper-station-guard-probe/src/main/java/co/surumene/simplecoins/a12probe/StationGuardProbe.java;.github/workflows/paper-station-guard-probe.yml | — | tests/paper-station-guard-probe/src/main/java/co/surumene/simplecoins/a12probe/StationGuardProbe.java;.github/workflows/paper-station-guard-probe.yml |

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

## Rationale and exact scope

Already accepted Drive 01/05 prohibit consumption of marked PAPER money outside 20 value-equivalent exchanges. A12 Paper PR #489 showed a **synthetic CartographyItemEvent** remains uncancelled for money in input 0 or 1; that event bypassed actual cartography recipes. Paper's actual `CartographyTableMenu` has specific item roles: slot 0 is a filled map, slot 1 is paper/empty map/glass; therefore the prior symmetric synthetic clicks cannot establish a native coin-consuming result for either slot. This Work replaces guessing with an independent authentic NMS menu test. PR #491 independently showed a genuine 5000C PDC PAPER is accepted/consumed by Minecraft MerchantOffer's isolated native primitive, not yet a player transaction.

On official Paper, create a genuine existing MapView and FILLED_MAP via Bukkit, and genuine CoinItemCodec 5000C PAPER. Use a detached ServerPlayer with zero network participants to construct a real NMS CartographyTableMenu using that world. Exercise setup results with actual maps and ordinary PAPER versus the genuine PDC money in second ingredient slot; separately test the coin in the *first* slot and a missing map control. Log concrete menu result type and item counts, rather than assume a native result is produced. Keep all operations in the disposable observer, and do not claim a real click or physical consumption if only the menu calculates an output.

The output observation is the evidence goal: real map + ordinary PAPER must yield the expected map result; first-slot non-map must be rejected. Whether actual PDC money in the second slot yields the same result as PAPER is the unknown this Work measures. If an API/source mismatch blocks proof, diagnose Java reflective errors and adapt to current fixed Paper; do not silently substitute a synthetic event.

No real player login, packet/click, map item distribution, durability ACK, admission, world economy accounting or normal journal is operated. No product classes, actual real player assets or CAT Confirmed artifacts change.

## Completion boundary

Actual NMS CartographyTableMenu object created; controls and coin identities verified; result marker in genuine Paper job log; no source coin modified; prior merchant NMS and cartography synthetic coverage remains green; product jar unchanged. This is issue-work verification, **not** a normative Confirmed/Process Closure/Model/Implementation Gate or a full cartography inventory transaction. If valid, merge the independent three-file test-only change into develop.

## Native result-slot callback follow-up (bounded verification extension)

The prior [PR #492](https://github.com/surumeneco/SimpleCoins/pull/492) established, on actual Paper 26.2 NMS `CartographyTableMenu`, that a real FILLED_MAP+ordinary PAPER and the same map+genuine 5000C PAPER both generate `FILLED_MAP` output, whereas money in input slot0 or no real map yields `AIR`. This is result computation only, not a completed result take.

This extension directly invokes the real NMS **result Slot.onTake(Player, ItemStack)** callback on a freshly constructed cartography menu for each of ordinary PAPER and genuine 5000C money, with an otherwise detached offline NMS player. Read actual input stack counts before and after the callback, separately from the original Bukkit-authored coin object, and compare the ordinary PAPER control. Whether this callback performs consumption is the observable unknown; do not infer success from a visual output or from a Bukkit synthetic event.

A direct result-slot callback is **not a network-originating InventoryClickEvent**. It does not prove the presence/order of Paper's pre-click CartographyItemEvent, permission checks, player reward transfer, Shift/drag, JE/BE interface, G0/G3 ledger correctness, or system-wide event interception. If the callback consumes the tagged NMS private money copy, report precisely that; do not claim a real user lost currency. Keep the feature-independent deny admission active. This remains a `work_kind: verification` Draft without any CAT normative confirmation.

Additional acceptance: exact same-head official Paper CI must record the observed native `onTake` before/after counts for real map+PAPER and real map+money, preserve original source currency PDC/model/count, and retain previous cartography result, merchant match/take, Domain and Phase6 checks. Merge the bounded verification unit only if all checks pass.
