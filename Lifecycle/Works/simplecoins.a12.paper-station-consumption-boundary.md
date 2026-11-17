---
cat_version: '0.2'
kind: work
id: simplecoins.a12.paper-station-consumption-boundary
status: draft
process: simplecoins.currency.crafting-exclusion
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

# A12 — Paper 26.2 Cartography / Merchant 消費イベントの未保護境界

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/316 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.crafting-exclusion | simplecoins.a12.paper-station-consumption-boundary | simplecoins.a12.vanilla-paper-choice-boundary;simplecoins.a12.actual-exchange-result-guard |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 build 134; Paper API CartographyItemEvent, MerchantRecipe; independent test-only Paper plugin; CAT fixed 896cae83 issue-work validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.paper-station-consumption-boundary.md;tests/paper-station-guard-probe/*;.github/workflows/paper-station-guard-probe.yml | — | tests/paper-station-guard-probe/*;.github/workflows/paper-station-guard-probe.yml |

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

## Normative input and current source divergence

Drive 01 and 05 explicitly require genuine, PDC-identified PAPER money to be consumed **only** by equal-value SimpleCoins exchanges, not in ordinary recipes or station transactions. Current `CoinExchangeRecipes` registers PrepareItemCraftEvent/CraftItemEvent/CrafterCraftEvent only; `WalletMenu` owns wallet GUI, not general merchant or cartography transactions. The 2026-11-17 develop tree does not contain `PaperCurrencyConsumption.java` or its JUnit, which exist uniquely in the superseded stacked PR #316. Their `blocksResult(station,rawSlot,coinInFirstInput,coinInSecondInput)` method is only a pure candidate predicate, **not a production listener**.

## Bounded independent Paper verification

- Boot official Paper 26.2 build 134 with the real SimpleCoins and independent default-deny guard, loopback only, zero connected players; no production data, commands, or login.
- Create a genuine fixed-PDC 5000 coin using the actual product CoinItemCodec. Use actual Paper's `Bukkit.createInventory(CARTOGRAPHY)`; for each input slot (0 and 1), preserve coin identity, construct a **synthetic InventoryView and CartographyItemEvent** targeting result raw slot 2, and dispatch via the real Paper PluginManager event bus. Assert that the event reaches a MONITOR witness and remains uncancelled by current production. This verifies **registered listener coverage for a simulated event only**, not server packet handling, resulting map, recipe resolution or actual input consumption.
- Separately register one real `MerchantRecipe` requiring ordinary PAPER in an isolated `Merchant`. Verify recipe roundtrip; do **not** infer that a PDC coin satisfies that ingredient, or that a trade was performed. A real merchant view and physical JE/BE trading require later verification.
- To determine the next technically executable experiment, inspect the **actual Paper build 134** `ServerGamePacketListenerImpl.handleContainerClick(ServerboundContainerClickPacket)` public dispatch and the runtime constructor signatures for packet, connection and handler. This is *read-only ABI discovery*: do not instantiate a network session, presume packet listener attached to a detached test player, or count these signatures as successful event cancellation. Keep the probe within the existing disposable test plugin and record the exact signatures in its archived CI log. These observations are inputs to a later, genuinely network-handler-bound click test; they do not change the already Draft Process/PI/TCE semantics.
- No synthetic event should physically remove currency or move player assets. Keep this test code in the disposable `tests/` project, never in the production JAR. No changing product plugins, admission, ledger or `main`.
- Follow-up production protection requires distinct CARTOGRAPHY/MERCHANT event boundaries and tests of **actual consumption**, not only `blocksResult` helper. For normative semantic behavior changes, use the already Draft A12 Process/PI/TCE and legitimate CAT Confirmed/process/model/implementation Gates, not this verification result.

## Acceptance / stop condition

- CAT Work structure valid; isolated Paper plugin actually compiles and runs on official Paper build 134, produces both cartography input event markers plus merchant recipe marker; fails if either event becomes unexpectedly blocked or physical money changes.
- Retains the API-path limitation in reported evidence. No duplicate claim of actual user trading or physical consumption.
- Require an actual single `handleContainerClick` signature plus the packet, connection and handler constructor inventory from the Paper runtime; absence/failure is an evidence gap, not a fabricated event-bus result.
- After CI success, integrate the independent verification unit to develop. Keep legacy PR #316 open until its two unique A12 source/test files have an appropriately authorized integration (or are safely superseded by a proven replacement).
