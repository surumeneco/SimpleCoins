---
cat_version: '0.2'
kind: work
id: simplecoins.wallet.network-product-verification
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

# Work: product wallet network acceptance

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
| simplecoins.wallet.paper-runtime | simplecoins.wallet.network-product-verification | simplecoins.currency.value-conservation;simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |
| Paper 26.2; Java 25; pinned MCProtocolLib 26.2; existing product startup acceptance fixture |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.wallet.network-product-verification.md;docs/cat/drafts/wallet-paper-gui-domain.md;src/main/java/co/surumene/simplecoins/paper/WalletMenu.java;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/LoopbackClient.java;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/WalletNetworkScenario.java;tests/paper-wcl-services-probe/product-startup-acceptance.sh;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/WalletMenu.java | tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/LoopbackClient.java;tests/paper-protected-settlement-runtime/client/src/main/java/co/surumene/probes/WalletNetworkScenario.java;tests/paper-wcl-services-probe/product-startup-acceptance.sh;.github/workflows/paper-wcl-services-probe.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

Actual Paper/JE protocol integration is external CI evidence; not a CAT JUnit suite runner.

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| real-use | not-run | — |

## Acceptance

Reuse the admitted product fixture of #688, including actual WCL and BlockLocker. After normal issuance and clear regression, issue one empty wallet and two 100C coins; the JE protocol client opens the nine-slot wallet with use-item and sends real shift-click packets to deposit 200C, withdraw four 50C coins and redeposit. Verify authoritative live wallet PDC and actual inventory by server commands, personal balance remains 200C across each operation, and the next normal product startup recovers exactly 200C in addition to the original settlement baseline. No simulated Bukkit events or diagnostic plugin during product startup. Keep Nexo/BE rendering and other input modes separate; no production code or normative CAT changes. A failing observation is evidence of a defect or fixture fault, not a formal CAT Red.

## Observed failure and scoped correction

Run [45562046444](https://github.com/surumeneco/SimpleCoins/actions/runs/45562046444) reached real product admission, verified the actual empty wallet and two 100C coins, then failed to open the nine-slot GUI. Artifact 14998132848, SHA-256 `22fc587107ab0ebe6d6bdf18fc6dae3df88531d1c1a5fd757c8f822e68bac4f4`. Paper 26.2 source `3e24be0` PlayerInteractEvent initializes the block result to DENY for air, item result DEFAULT, and deprecated isCancelled tests the block result. A handler using ignoreCancelled=true skips ordinary air use. The source-approved wallet use contract (Drive 01) requires opening the GUI. The adapter must check item-use denial independently and not override denied block interactions. No currency, wallet cap, ownership, or admission semantics change. The actual product protocol test remains the external Red/Green oracle; not a formal CAT TDD Red.
