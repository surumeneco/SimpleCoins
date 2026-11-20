---
cat_version: '0.2'
kind: work
id: simplecoins.a04.saved-blocklocker-owner-contract
status: draft
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: specification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A04/G1 — SAVED BlockLocker実保護帰属のDraft

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/issues/4 |
| https://github.com/surumeneco/SimpleCoins/pull/560 |
| https://github.com/surumeneco/SimpleCoins/pull/562 |
| https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.blocklocker.saved-owner | simplecoins.blocklocker.saved-owner;simplecoins.blocklocker.saved-owner.pi.classify;simplecoins.blocklocker.saved-owner.tce.classify | simplecoins.settlement;simplecoins.external-storage-provider-integration;simplecoins.settlement.writer-epoch-publication |

## Technologies
| Technology |
| --- |
| Java 25; Paper 26.2; BlockLocker 1.15; saved terrain Anvil; CAT markdown-v2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.saved-blocklocker-owner-contract.md;docs/cat/drafts/a04-saved-blocklocker-owner-*.md | — | — |

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

## Acceptance / technical handoff

実地形の全関連看板・保護対象・BlockLocker 1.15の表示/PDC・表裏・locale・物理接続・選択済み保存源を読取り、(a)検証済み非保護Free、(b)適用されるEveryone有効Free、(c)PRIVATE主所有者UUID検証済み、(d)未知/矛盾HOLDを区別する。所有者候補はBAN/playerdataと同一世代の上位決算へ渡し、未確認のrawテキストやPDCを根拠にするFREE昇格を禁止する。

Process/PI/TCEは規範承認前のDraft。10 Boolean入力1024ケースの排他分割、構文lint、Work validateは**正式CAT Gateや承認を意味しない**。Product implementationは利用者の正当なCAT ConfirmedとProcess/Model Gate以降に限定し、G0/G3/入室公開は別に証明する。独立して可能な読取専用実Paper証拠・既存WCL照合・writer閉包の調査は続ける。
