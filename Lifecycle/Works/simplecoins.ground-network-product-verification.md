---
cat_version: '0.2'
kind: work
id: simplecoins.ground-network-product-verification
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

# Work: product drop, pickup and death acceptance

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/691 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/confirmed/ground-item-process.md |
| https://github.com/surumeneco/SimpleCoins/issues/5 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.ground-item | simplecoins.ground-network-product-verification | simplecoins.currency.value-conservation;simplecoins.wallet.paper-runtime |

## Technologies

| Technology |
| --- |
| Paper 26.2; Java 25; MCProtocolLib 26.2; actual existing WCL/BlockLocker startup fixture |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.ground-network-product-verification.md;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/LoopbackClient.java;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/WalletNetworkScenario.java;tests/paper-wcl-services-probe/product-startup-acceptance.sh | — | tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/LoopbackClient.java;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/WalletNetworkScenario.java;tests/paper-wcl-services-probe/product-startup-acceptance.sh |

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

After #691 wallet deposit/withdraw/redeposit, drop the actual wallet (200C) by JE protocol; verify P25200/F1000 -> P25000/F1200 while T26200 and D30000 stay unchanged, pick up through actual collision and recover the same wallet PDC, repeat the transfer via real vanilla death and respawn packet, then verify the original existing restart assertions. Use fresh stats observations and actual item checks, not synthetic Bukkit events. Existing approved semantics unchanged; no production source edits, no new CAT oracle. Death is covered by 02B and #5, not the narrow ground-item PI alone. This does not establish all storage/BE operations or production load.
