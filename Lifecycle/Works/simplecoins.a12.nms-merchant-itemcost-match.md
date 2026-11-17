---
cat_version: '0.2'
kind: work
id: simplecoins.a12.nms-merchant-itemcost-match
status: draft
process: simplecoins.currency.station-consumption-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
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

# A12 — 実Minecraft MerchantOfferのPDC貨幣PAPER受理／消費判定

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/489 |
| https://github.com/surumeneco/SimpleCoins/pull/490 |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.station-consumption-exclusion | simplecoins.a12.nms-merchant-itemcost-match | simplecoins.a12.paper-station-consumption-boundary;simplecoins.a12.station-consumption-spec |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; real MerchantRecipe; CraftMerchantRecipe→Minecraft MerchantOffer; NMS ItemStack conversion; test-only reflection; exact-head GitHub Actions; CAT-TDD 896cae83 issue-work |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.nms-merchant-itemcost-match.md;tests/paper-station-guard-probe/src/main/java/co/surumene/simplecoins/a12probe/StationGuardProbe.java;.github/workflows/paper-station-guard-probe.yml | — | tests/paper-station-guard-probe/src/main/java/co/surumene/simplecoins/a12probe/StationGuardProbe.java;.github/workflows/paper-station-guard-probe.yml |

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

## Decision basis and test boundary

Drive 01/05 already require a PDC-tagged PAPER coin to be ineligible as an ordinary material except for the 20 authorized equivalent currency exchanges. Previously [PR #489](https://github.com/surumeneco/SimpleCoins/pull/489) on official Paper proved **only** that the Bukkit MerchantRecipe for ordinary PAPER registers, without testing whether the genuine CoinItemCodec-authored item qualifies as a valid trade input.

This independent verification calls the actual Paper runtime's CraftMerchantRecipe conversion into Minecraft `net.minecraft.world.item.trading.MerchantOffer`, with a genuine `CoinItemCodec` 5000C PAPER, ordinary PAPER and STONE as controls, to measure the NMS `satisfiedBy` result. For each result, test the actual Minecraft `take` method on a **detached private NMS ItemStack copy**, proving exactly whether the native trade matcher would shrink that copy. Do not guess that a marked PAPER is accepted, or invent a successful trade before observing the real Paper runtime. Assert ordinary PAPER is accepted and STONE is rejected as controls.

The test never opens a merchant GUI, sends a click or packet, connects a player, spawns trade rewards, modifies real player inventories, or performs a complete trading transaction. It does not establish trade event interception, transaction cancellation or JE/BE compatibility. It may provide the precise missing core merchant *primitive* witness necessary to design a guard, but cannot alone prove physical currency loss in production.

Use existing disposable `SimpleCoinsStationGuardProbe` JAR, 127.0.0.1 loopback, independent closed admission guard, zero online players, fixed official Paper build 134 and proven SHA256. Only add the isolated NMS observation method and exact named CI evidence. The pure helper in old PR #316 still lacks a wired production listener and must not be auto-merged.

## Acceptance and authority

- Exact Paper source build boots and ordinary PAPER predicate holds; STONE predicate rejects; 5000C PDC/model roundtrips through CraftItemStack→NMS; genuine currency predicate is **observed**, not predetermined.
- `MerchantOffer.take` returns the same boolean as `satisfiedBy` and decrements only accepted **detached** copies by the configured one-paper cost; source coin remains untouched and recognizable.
- Existing synthetic Cartography event, MerchantRecipe roundtrip and independent default-deny checks remain green.
- CAT `cat_flow.py validate` (issue-work structure only) and unrelated Domain/Phase6 CI succeed at the same feature HEAD. This is **not CAT confirmed, formal Gates or Paper player-transaction acceptance**.
- After passing all checks, merge the independent verification-only unit into develop and record its bounded evidence in Issue #423, without changing production or main.
