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
| https://github.com/surumeneco/SimpleCoins/pull/518 |
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
- **Paperの実サーバークリック経路と反例**：本物のbuild134から、`ServerGamePacketListenerImpl.handleContainerClick(ServerboundContainerClickPacket)`、`(int,int,short,byte,ContainerInput,Int2ObjectMap<HashedStack>,HashedStack)`のpacket constructorと実プレイヤー状態境界を確定。直接`CartographyTableMenu.clicked`呼出しはBukkitイベントを発火させない一方、実`handleContainerClick`呼出しは**プレイヤーが操作可能な場合に**`InventoryClickEvent`と`CartographyItemEvent`を発火する。実NMSの`Player.isImmobile`は`Entity.valid`を要求し、未登録の合成プレイヤーでは初期値が`false`で入口から早期returnする。これをネットワークイベント非発火と誤認しないよう、隔離テスト内だけ`valid=true`を設定し、`joining=true`かつ外部socket未接続で同じメニューの結果raw slot 2へパケットを入力。**通常PAPERは1→0、実CoinItemCodec製の正規5000C貨幣はテスト専用`InventoryClickEvent`取消により1→1**。両ケースとも`InventoryClickEvent=1`、`CartographyItemEvent=1`、貨幣ケースの取消回数は1。実ログ証拠：[#44144828512](https://github.com/surumeneco/SimpleCoins/actions/runs/44144828512)の隔離Paperログ（この回次のワークフロー結果はログassertionの旧期待値によって失敗、実処理結果の成功と区別）。正式受入は訂正済みワークフローの**最新HEAD成功**で別途確認する。
- **重要な残存境界**：これは実Paper packet handlerの内側を経由するが、正常な認証/ログイン・実ネットワークsocket・JE/BEクライアント・外部pluginによる後段状態変更は再現しない。キャンセルリスナーは**使い捨て試験プラグインにしか存在しない**。製品`CoinExchangeRecipes`・他のリスナーへは未接続。Merchantについては実NMS costの5000C消費とMerchantRecipe登録までしか確認しておらず、結果クリックのイベント取消と現物不変は未検証。Draft Process/PI/TCEをConfirmedへ昇格させたり、両ステーションの保護済み・製品リリース可と解釈しない。
- No synthetic event should physically remove currency or move player assets. Keep this test code in the disposable `tests/` project, never in the production JAR. No changing product plugins, admission, ledger or `main`.
- Follow-up production protection requires distinct CARTOGRAPHY/MERCHANT event boundaries and tests of **actual consumption**, not only `blocksResult` helper. For normative semantic behavior changes, use the already Draft A12 Process/PI/TCE and legitimate CAT Confirmed/process/model/implementation Gates, not this verification result.

## Product listener absence counterexample (bounded verification scope)

- The existing Merchant server packet-handler test executes two cases under a **disposable test-only** InventoryClickEvent guard, so it shows that cancellation is technically possible, not whether SimpleCoins production already protects currency. Extend that same fixed-Paper test with a **third, otherwise identical genuine-5000C result click**, explicitly switch off only the disposable guard, keep SimpleCoins and independent default-deny guard loaded, and record whether the native input decreases from one to zero. This directly measures the missing product Merchant protection without new instrumentation or speculative Plugin permutations.
- Require an actual InventoryClickEvent and result-slot consumption (1→0) in the no-probe-guard case, while the guarded case remains 1→1 and ordinary PAPER remains 1→0. No connected player, external socket, playerdata persistence, JE/BE client or monetary service is exercised; do not conflate this with a network-authenticated trade or final product release.
- **This is an observation test, not a proposed production fix**: success should establish the current gap for the existing A12 Draft and enable a narrowly scoped product listener after legitimate specification confirmation. If production protection already exists, fail the characterization and investigate before recording a defect. No product code, Draft status, security guard or main is changed.

- **Cartographyの独立した未保護対照（#520と混同しない）**：Merchantの非保護実消費は#520で確認済み。次は、#518と同じ本物の`ServerGamePacketListenerImpl.handleContainerClick`・製図台raw slot2に対して、通常PAPER（1→0）、正規5000C＋試験専用取消（1→1）、同じ正規5000C＋取消を無効化（**期待する観測値は1→0**）の三ケースを同じ隔離CIで比較する。最後の実現物減少、`InventoryClickEvent`/`CartographyItemEvent`各1件、試験取消0件、元Bukkit貨幣不変をすべて機械的にassertする。もし現物が減らなければ現在の製品Listener実装／別要因を調査し、ギャップを仮定して成功扱いしない。これは試験用NMSコピーの損失を立証する技術観測であり、実JE/BE接続や製品修正ではない。

## Acceptance / stop condition

- CAT Work structure valid; isolated Paper plugin actually compiles and runs on official Paper build 134, produces both cartography input event markers plus merchant recipe marker; fails if either event becomes unexpectedly blocked or physical money changes.
- Retains the API-path limitation in reported evidence. No duplicate claim of actual user trading or physical consumption.
- Require an actual single `handleContainerClick` signature plus the packet, connection and handler constructor inventory from the Paper runtime; absence/failure is an evidence gap, not a fabricated event-bus result.
- Require distinct log markers for the actual server-side packet-handler dispatch and the test-only cancellation of a PDC-money result; when detached-player or unconnected-transport limitations prevent the handler from executing, fail the focused CI rather than silently substituting `AbstractContainerMenu.clicked` or a synthetic Bukkit `callEvent`.
- After CI success, integrate the independent verification unit to develop. Keep legacy PR #316 open until its two unique A12 source/test files have an appropriately authorized integration (or are safely superseded by a proven replacement).
