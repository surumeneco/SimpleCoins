---
cat_version: '0.2'
kind: work
id: simplecoins.product-bedrock-network-verification
status: draft
process: simplecoins.presentation.geyser-real-loader-verification
source_refs:
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1RVbNpheNNWTaN0WYxZsO__fm5H-0_nZh/view
spec_status: unknown
---

# Work: product Bedrock network admission and item identity

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/690 |
| https://github.com/surumeneco/SimpleCoins/pull/688 |
| https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.presentation.geyser-real-loader-verification | simplecoins.product-bedrock-network-verification | simplecoins.settlement.admission-barrier;simplecoins.currency.value-conservation |

## Technologies

| Technology |
| --- |
| Paper 26.2; Geyser 2.11.1 b1210; Java 25; Node 24; bedrock-protocol 3.61.0 |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.product-bedrock-network-verification.md;.github/workflows/paper-wcl-services-probe.yml;tests/paper-wcl-services-probe/product-bedrock-acceptance.sh;tests/paper-wcl-services-probe/bedrock-join.cjs;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java | — | .github/workflows/paper-wcl-services-probe.yml;tests/paper-wcl-services-probe/product-bedrock-acceptance.sh;tests/paper-wcl-services-probe/bedrock-join.cjs;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

Real external CI verification, not a JUnit Check or formal CAT gate.

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

Extend the existing #688 normal product fixture with actual Geyser; reuse official verified build and real chunk transfer of #690. Download and hash every advertised pack before acknowledging the stack; verify actual nine item registry identifiers and nine actual inventory stacks after a 16666C grant, authoritative player balance, successful Java backend login and persisted source IP. Self-signed authentication is used only on an independently checked loopback disposable CI listener; this cannot prove Microsoft/Floodgate authentication, user-device rendering, or real BE GUI operation. No product source, runtime configuration, or normative CAT changes. The first real connection reached the Java backend but failed closed at WclLiveMarker.provider during EntitiesLoadEvent (run 45571787174). Diagnostic run 45575478196 passed all four CI workflows; artifact 15004022562 records both packs, all nine registry IDs and inventory items, 16666C balance and persisted loopback IP. Only initialized holders were observed; the exact prior failing marker was not reproduced. Final uninstrumented acceptance removes the probe again. The separate pending-founder production correction #694 is incorporated for combined verification, not presumed accepted.
