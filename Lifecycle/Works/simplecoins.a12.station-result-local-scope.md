---
cat_version: '0.2'
kind: work
id: simplecoins.a12.station-result-local-scope
status: draft
process: simplecoins.currency.station-result-local-decision
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/529
decision_ref: user-chat:2026-11-18:A12-station-result-local-decision-approved
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: specification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A12: 局所判定と全体保証の責務分離（承認済み）

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/529 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.station-result-local-decision | simplecoins.a12.station-result-local-scope | simplecoins.currency.station-consumption-exclusion |

## Technologies
| Technology |
| --- |
| Java |
| JUnit |
| Paper |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.station-result-local-scope.md;docs/cat/confirmed/a12-station-local-decision-process.md;docs/cat/confirmed/a12-station-local-decision-pi.md;docs/cat/confirmed/a12-station-local-decision-tce.md | — | — |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | Approved Confirmed scope; formal Gate remains to be run |
| model-conformance | not-run | Approved Confirmed scope; formal Gate remains to be run |
| implementation-conformance | not-run | Product/adapter not yet formally verified |
| semantic-review | not-run | User approval established; conditional review only if prescribed |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Acceptance and required decision

This issue-work records the user-approved **Confirmed** finite-input pure decision as a separate Process. It does **not** alter or waive the existing Confirmed A12 open-world Process, its Process Closure, the Java/Paper event adapter or G0/G3.

Static lint and `--allow-draft` model compilation are non-normative readiness checks. Normative vector/test generation remains blocked until legitimate confirmation. After this approval, use the actual canonical CAT flow and `spec-test-sync` against existing Paper event behavior; do not fabricate Red for code already implemented.

**Decision:** the user approved the separate pure-decision Process boundary on 2026-11-18 (reference: https://github.com/surumeneco/SimpleCoins/pull/532#issuecomment-6976353886). The existing broader A12 Process remains open-world and unresolved.
