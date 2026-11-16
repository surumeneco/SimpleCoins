---
cat_version: '0.2'
kind: work
id: simplecoins.a02.cat-partial-vs-complete-ack
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/467
  - https://github.com/surumeneco/SimpleCoins/pull/468
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
spec_status: draft
---

# G3/A02 — 部分受領を完了ACK判定から分離するCAT Draft整備

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/467 |
| https://github.com/surumeneco/SimpleCoins/pull/468 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/drafts/a02-grant-delivery-process.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/drafts/a02-grant-delivery-pi.md |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/cat/drafts/a02-grant-delivery-tce.md |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.grants.pi.delivery;simplecoins.grants.tce.delivery;simplecoins.a02.cat-partial-vs-complete-ack | simplecoins.a02.paper-partial-receipt-restart;simplecoins.a02.paper-final-save-pre-ack-crash |

## Technologies

| Technology |
| --- |
| CAT-TDD pinned 896cae83; Paper 26.2; persisted playerdata receipts; actual GrantLedger; specification-authority boundary |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.cat-partial-vs-complete-ack.md;docs/cat/drafts/a02-grant-delivery-process.md;docs/cat/drafts/a02-grant-delivery-pi.md;docs/cat/drafts/a02-grant-delivery-tce.md;.github/workflows/cat-a02-grant-draft-lint.yml | — | .github/workflows/cat-a02-grant-draft-lint.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | ---: |

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

## Existing authority and observed Draft defect

Drive 03/02B already decides the behavioral rule: saving partial physical delivery (real money and active receipt in the same saved playerdata) **does not authorize ACK**. A journal ACK is allowed only after a verified complete delivery. Partial state must remain pending through inventory-full conditions, clean restart and SIGKILL. The actual Paper #467 and #468 probes separately proved both partial and complete saved receipts and two distinct pre-ACK crash positions.

The **current Draft TCE** has `v0` and `r0` allowing `出力.ack許可=真` when `入力.保存済み現物受領対=真`, `入力.保存世代証拠有効=真`, `入力.他収納保全=真` and `入力.矛盾あり=偽`, but it does not distinguish an authentic **partially complete** saved receipt from a **fully completed** saved receipt. Thus a valid partial receipt may satisfy the old boolean inputs and incorrectly qualify for an ACK in the Draft. This is a real specification abstraction bug, not a newly undecided game rule.

Refine the Draft PI by adding a distinct mandatory bool `入力.予約全額受領済`, whose true means the actual authenticated journal grant is fully completed by the saved same-generation receipt and its verified physical delivery evidence. For the authentic partial receipt, it MUST be false, although `入力.保存済み現物受領対` may be true. Update Process observation/meaning, and TCE `v0/r0` require that field true. Add disjoint `v1p/r1p` cases (valid partial ⇒ maintain pending, no ACK, no abnormal shutdown), and make existing no-saved-pair `v1/r1` require valid source/witness and unchanged unrelated storage so they cannot overlap with anomalous `v2/r2`. Retain partial coverage and known concurrent-writer/OS-power-failure/real-login uncertainty; avoid guessing complete state equivalence.

This Work is a Draft-only specification correction in the canonical `issue-work` flow, not a CAT Confirmed decision, TestModel, Model Conformance or production authorization. No world data, app command, admission or `main` changes.
