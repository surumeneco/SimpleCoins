---
cat_version: '0.2'
kind: work
id: simplecoins.storage-exchange-network-verification
status: draft
process: simplecoins.accounting.ground-item
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view
spec_status: confirmed
---

# Work: product storage and exchange acceptance

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/695 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/ground-item-process.md |
| https://github.com/surumeneco/SimpleCoins/issues/5 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.ground-item | simplecoins.storage-exchange-network-verification | simplecoins.currency.value-conservation;simplecoins.wallet.paper-runtime |

## Technologies

| Technology |
| --- |
| Paper 26.2; Java 25; MCProtocolLib 26.2; actual existing WCL/BlockLocker startup fixture |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.storage-exchange-network-verification.md;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/LoopbackClient.java;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/WalletNetworkScenario.java;tests/paper-wcl-services-probe/product-startup-acceptance.sh | — | tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/LoopbackClient.java;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/WalletNetworkScenario.java;tests/paper-wcl-services-probe/product-startup-acceptance.sh |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

Actual product acceptance in existing Paper CI; not a JUnit suite or CAT Gate.

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |

## Acceptance

After the existing actual wallet/death/ground regression, use genuine JE block interaction and inventory packets to deposit the actual 200C wallet into an unprotected chest and recover it. Assert P/F/T with fresh server stats and actual wallet PDC. Issue two 10C coins, open a real crafting table, verify the registered 10C to ten 1C recipe preview and shift-craft twice from the same input stack, verify actual result inventory model and unchanged P/F/T, clear the 20C and retain the original restart assertions. No production edits, synthetic event calls, new CAT oracle or claim of all client operations. 05 acceptance, #5/#6. Combined regression of #693 and #695 on latest develop.
