---
cat_version: '0.2'
kind: work
id: simplecoins.domain.money-wallet-conservation-verification
status: draft
process: simplecoins.currency.value-conservation
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/pull/299
  - https://github.com/surumeneco/SimpleCoins/pull/292
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Domain money/wallet保存価値の独立したアルゴリズム・状態遷移オラクル

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/299 |
| https://github.com/surumeneco/SimpleCoins/pull/292 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/CoinMoney.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WalletBalance.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.value-conservation;simplecoins.wallet.balance | simplecoins.domain.money-wallet-conservation-verification | simplecoins.currency.pdc-stable;simplecoins.settlement.default-deny |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; CoinMoney.minimumCoins/totalValue; independent unbounded dynamic programming; WalletBalance immutable transitions, cap reductions and overflow; CAT pinned issue-work structural validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.domain.money-wallet-conservation-verification.md;src/test/java/co/surumene/simplecoins/domain/CoinMoneyExhaustiveConservationCharacterizationTest.java;src/test/java/co/surumene/simplecoins/domain/WalletBalanceStatefulConservationCandidateTest.java;.github/workflows/domain-money-wallet-conservation-oracles.yml | — | src/test/java/co/surumene/simplecoins/domain/CoinMoneyExhaustiveConservationCharacterizationTest.java;src/test/java/co/surumene/simplecoins/domain/WalletBalanceStatefulConservationCandidateTest.java;.github/workflows/domain-money-wallet-conservation-oracles.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| money-conservation | regression | junit | @repo:SimpleCoins | gradle :test --tests co.surumene.simplecoins.domain.CoinMoneyExhaustiveConservationCharacterizationTest | 100000-amounts;minimal-counts;long-boundaries | CoinMoney;CoinDenomination | JUnit XML | 180 |
| wallet-transitions | regression | junit | @repo:SimpleCoins | gradle :test --tests co.surumene.simplecoins.domain.WalletBalanceStatefulConservationCandidateTest | 12000-state-transitions;reduced-caps;overflow;rejected-immutability | WalletBalance | JUnit XML | 180 |

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

## Confirmed requirements vs technical observation

Google Drive 01/05 approves immutable denomination values, value conservation, nine coin denominations, wallet balance bounds, cap reductions that **must preserve existing above-cap balances** and allow withdrawals, and no overflow/value creation. This source-specific Work does **not** change product behavior, accepted values, wallet GUI or new issuance policy.

Old unmerged stacked PR [#299](https://github.com/surumeneco/SimpleCoins/pull/299) contains a unique 100,000-amount coin decomposition characterization not present on current develop. It computes the minimum coin count by an independent dynamic-programming oracle, checks all amounts 1..100000 against current greedy `CoinMoney.minimumCoins`, verifies total `BigInteger` monetary value, and separately evaluates near-`Long.MAX_VALUE` valid values. The DP algorithm does not use the production greedy function as the test oracle.

Old unmerged stacked PR [#292](https://github.com/surumeneco/SimpleCoins/pull/292) contains a unique deterministic 12,000-step wallet transition sequence: periodically lower/raise cap while preserving already higher balance, attempt both deposits and withdrawals, assert invalid or over-cap requests are rejected without mutating the previous immutable instance, and verify arithmetic overflow/negative/zero/insufficient-funds bounds.

These tests were **copied into a fresh feature branch from the current develop**, after direct source/blobs were inspected; do not merge either obsolete stacked branch. Existing `CoinMoneyTest` and `WalletBalanceTest` remain; these are additional independent algorithm/state-sequence oracles. Old issue/queue history is provenance only; retired TaskQueue is not invoked.

## Acceptance

Dedicated JDK25 JUnit CI must confirm the two test classes separately on the same exact PR HEAD, with JUnit XML >0 tests and no failures/errors/skips. In addition check existing full Domain and Phase6 CI at that HEAD. Only scope Work/tests/CI to develop after green and independent ref/diff readback, then comment/close old #292 and #299 **without merging**. If a current source behavior differs, preserve the Red evidence for review rather than changing settled user semantics or silently editing test oracles.

**Limits:** JUnit checks pure Domain methods, not actual Paper GUIs, JE/BE, wallet PDC consistency, atomic inventory transfers, saved G3 ACK, world G0, Nexo, actual player actions, or CAT Confirmed/formal Gate. No production source/config/admission/main writes.
