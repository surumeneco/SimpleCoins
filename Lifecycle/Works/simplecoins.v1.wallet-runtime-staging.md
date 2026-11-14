---
cat_version: '0.2'
kind: work
id: simplecoins.v1.wallet-runtime-staging
status: draft
process: simplecoins.wallet.paper-runtime
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.v1.wallet-runtime-staging

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |
| https://github.com/surumeneco/SimpleCoins/pull/319 |
| https://github.com/surumeneco/SimpleCoins/blob/bf1237dbb937cb5405b8344898c1e6c8d1ff018c/src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.wallet.paper-runtime | simplecoins.v1.wallet-runtime-staging | simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/paper/Wallet*;src/main/java/co/surumene/simplecoins/domain/WalletTransferTransaction.java;src/main/resources/config.yml;src/test/java/co/surumene/simplecoins/paper/*;src/test/java/co/surumene/simplecoins/domain/WalletTransferTransactionTest.java;.github/workflows/*;build.gradle.kts | src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/paper/Wallet*;src/main/java/co/surumene/simplecoins/domain/WalletTransferTransaction.java;src/main/resources/config.yml | src/test/java/co/surumene/simplecoins/paper/*;src/test/java/co/surumene/simplecoins/domain/WalletTransferTransactionTest.java;.github/workflows/* |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

External/semantic gates are reported evidence. `passed` or `not-applicable` requires a concrete evidence/rationale reference.

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43568887746 (isolated real Paper wallet registry); https://github.com/surumeneco/SimpleCoins/actions/runs/43568887813 (JUnit); https://github.com/surumeneco/SimpleCoins/actions/runs/43568887761 (guard + corrupt journal); https://github.com/surumeneco/SimpleCoins/actions/runs/43568887725 (coin), https://github.com/surumeneco/SimpleCoins/actions/runs/43568887809 (profile BAN), https://github.com/surumeneco/SimpleCoins/actions/runs/43568887733 (static) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Work scope and checked source state

- Source requirement: user-approved design `01_貨幣・財布・レジ.md` fixes wallet recipe (leather N/S/E/W and central iron), nine-slot denomination GUI, deposit/withdraw, one-item stack, configurable default 640000 maximum, and dye preserving UUID/balance. `03_運営設定・コマンド.md` fixes currency unit/name configuration.
- Checked `develop@bf1237dbb937cb5405b8344898c1e6c8d1ff018c`: `WalletBalance` domain exists; `SimpleCoinsPlugin` now registers 20 coin recipes, but no wallet codec, crafting, GUI listener, or wallet config is in develop. Old unmerged PR #319 includes candidate classes to review and adapt; its absence from develop is not proof of successful behavior.
- Runtime scope: adapt selected wallet classes from PR #319 after source review, register them **only after journal startup validation**, preserve independent default-deny admission (never call `publication` commit), and atomically rollback recipes on bootstrap exception. Stage wallet codec and GUI for future physical journal integration; no claim of crash-safe in-game transfers before the general settlement is completed.
- Require a test-only Paper 26.2 build 134 probe that observes wallet crafting registry, native ordinary bundle rejection, stable PDC wallet semantics and shutdown cleanup; independent JUnit for rollback/compensation. Keep the probe OUT of production JAR.

## Observed test-first evidence

- Initial wallet test-only PR head `b2211d112ddc3788d2e3cd85d82444148fb74a6a`: isolated Paper build 134 run [43568275362](https://github.com/surumeneco/SimpleCoins/actions/runs/43568275362) reached the real server and failed for the expected reason: `Missing SimpleCoins wallet recipe`. This is observed Red, **not** script-owned normative CAT TDD Red.
- Integrated source head `1b53aa30737ca5191a15fd4506eee27facfa96f7`: [Paper wallet registry](https://github.com/surumeneco/SimpleCoins/actions/runs/43568887746) **passed** with the marker `PAPER_WALLET_ISOLATED_SUCCESS`. Test checks the actual 3×3 ingredient grid (Paper generates symbolic letters on retrieval), 1 creation + 16 dyed variations, PDC UUID, numerical zero initial balance, max stack 1, marked-wallet-only ingredients and preserved UUID + 12345 balance across Paper recolor.
- The source reuses PR #319 only after review; the current plugin first verifies its persistent normal monetary journal, then validates currency names/unit and wallet cap, registers both coin and wallet recipes and GUI, and keeps the independent guard default-deny. Shutdown/bootstrap failure removes owned recipes; no finished snapshot is published.
- Independent [Gradle/JUnit](https://github.com/surumeneco/SimpleCoins/actions/runs/43568887813), [guarded startup + corrupt journal](https://github.com/surumeneco/SimpleCoins/actions/runs/43568887761), [coin recipes](https://github.com/surumeneco/SimpleCoins/actions/runs/43568887725), [profile BAN](https://github.com/surumeneco/SimpleCoins/actions/runs/43568887809), [Phase6 static](https://github.com/surumeneco/SimpleCoins/actions/runs/43568887733) all passed at that code head.
- Wallet transaction compensation is **in-memory only**. JE/BE GUI player input, shutdown/crash durability, offline currency mutations, general journal integration and product CAT formal Gates remain **not-run**. This is not a production-ready SimpleCoins.

## Acceptance boundaries

1. Paper startup has exactly 1 wallet creation recipe and 16 dye recipes in addition to exactly 20 coin recipes, with no unexpected recipe namespace collisions. No players may enter during settlement.
2. New wallet UUID is fresh per materialized wallet, amount/max-stack 1, balance starts zero; old wallet balance is not truncated on a configured-cap reduction; recolor retains id/balance. Ordinary bundles do not become wallets.
3. GUI uses exactly 9 denomination slots, refuses unsupported/ambiguous click and Shift batch crafting; rollback remains explicit and fail-closed; **not certified as crash-durable**.
4. Test code is separate, exact Paper/JUnit/guard tests must pass; JE/BE actual player interaction, Nexo, external plugins, full monetary mutation audit and CAT product gates remain independent pending acceptance.
5. Draft Work is not normatively Confirmed; do not fabricate Process/PI/TCE gates or user sign-off. No main release.
