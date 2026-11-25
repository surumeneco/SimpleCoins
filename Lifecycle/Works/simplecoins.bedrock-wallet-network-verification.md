---
cat_version: '0.2'
kind: work
id: simplecoins.bedrock-wallet-network-verification
status: draft
process: simplecoins.wallet.paper-runtime
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1RVbNpheNNWTaN0WYxZsO__fm5H-0_nZh/view
spec_status: draft
---

# Work: Bedrock wallet network acceptance

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/commit/860f24acc9c799ba54faf85786952e7dbd156d4f |
| https://github.com/surumeneco/SimpleCoins/pull/688 |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.wallet.paper-runtime | simplecoins.bedrock-wallet-network-verification | simplecoins.currency.value-conservation;simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |
| Paper 26.2; Java 25; pinned MCProtocolLib 26.2; existing product startup acceptance fixture |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.bedrock-wallet-network-verification.md;tests/paper-wcl-services-probe/bedrock-join.cjs;tests/paper-wcl-services-probe/product-bedrock-acceptance.sh | — | tests/paper-wcl-services-probe/bedrock-join.cjs;tests/paper-wcl-services-probe/product-bedrock-acceptance.sh |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

Existing actual Paper/Geyser product CI. Not a CAT JUnit gate.

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |

## Acceptance

#423 / 05: after actual nine-coin BE inventory receipt, clear the coins and issue a normal wallet and one 100C coin. Use actual BE click-air and Geyser inventory requests with server-issued stack IDs; observe all nine wallet icons, deposit 100C, confirm real Java wallet PDC and balance, withdraw the same coin and verify wallet PDC 0 and personal balance 100C again. No direct event injection, production code modification, or substitute CAT evidence. No graphical render or authenticated production Floodgate claim.
