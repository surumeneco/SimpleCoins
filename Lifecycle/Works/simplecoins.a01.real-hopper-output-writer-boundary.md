---
cat_version: '0.2'
kind: work
id: simplecoins.a01.real-hopper-output-writer-boundary
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/635
  - https://github.com/surumeneco/SimpleCoins/pull/634
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a01.native-hopper-pull-prebody
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0 — independently observe native Hopper output into a real Chest

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/610 |
| https://github.com/surumeneco/SimpleCoins/pull/631 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication;simplecoins.settlement.writer-epoch-publication | simplecoins.a01.real-hopper-output-writer-boundary | simplecoins.a01.native-hopper-pull-prebody |

## Technologies
| Technology |
| --- |
| Official Paper26.2 build134; Java25 Agent; genuine shipped WWC/WWW/WCL/WGL; real Chest/Hopper/NMS tick transfer; authentic 5000C coins; deny-by-default admission |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.real-hopper-output-writer-boundary.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/InventoryHopperOutputWriterProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/a01-native-save-agent-bridge.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/**;.github/workflows/a01-native-save-agent-bridge.yml |

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

## Acceptance, safety boundary and stop rule

This is test-only **issue-work** bounded verification of the opposite Hopper direction after PR #635 implemented a known NMS **pull** hook. It does not extend the production writer fence and does not modify CAT Confirmed requirements. Under official Paper26.2 build134, distributed WWC/WWW/WCL/WGL, independent default-deny Guard, no players and loopback-only port, use a downward enabled Hopper *above* a Chest in a force-loaded chunk. Require real physical transfer of exactly two authentic 5000C coins from Hopper to Chest during ordinary ticks with the Agent PASSIVE, and log an actual InventoryMoveItemEvent native stack. An unreachable passive control is a failed probe, not proof of coverage.

Restore the two coins to the Hopper while still PASSIVE, verify eight genuine transformed writer class families and zero in-flight calls, then arm+seal the product Agent *only in the disposable test JVM*. Wait another 60 ordinary Paper ticks. Classify distinct outcomes: (a) physical push while SEALED/LIMITED_TRACKING (uncovered native writer), (b) physical push before FAULTED (partial mutation), (c) preserved money with both fences FAULTED (some covered hook refused execution, not evidence of exact push-prebody coverage). Require physical value conservation and exact states. Do not accept no-action under unchanged SEALED as safe. Record exact NMS call ownership only if StackWalker actually observed it.

No production code edit, no new hook guessed from unknown descriptor, no runtime arming, G0 publication, G3 ACK, admission release or main merge. Follow a confirmed counterexample with separately scoped method-specific A01 Red/Green and full CI. Every untested writer remains outside demonstrated closure.
