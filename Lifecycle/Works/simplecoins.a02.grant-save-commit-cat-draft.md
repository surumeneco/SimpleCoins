---
cat_version: '0.2'
kind: work
id: simplecoins.a02.grant-save-commit-cat-draft
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# A02 — 予約配布の保存・受領・ACK境界のCAT規範候補

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/434 |
| https://github.com/surumeneco/SimpleCoins/pull/437 |
| https://github.com/surumeneco/SimpleCoins/pull/468 |
| https://github.com/surumeneco/SimpleCoins/pull/473 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.grants.pi.delivery;simplecoins.grants.tce.delivery | simplecoins.a02.paper-save-commit-verification;simplecoins.grants.saved-file-readback |

## Technologies

| Technology |
| --- |
| CAT-TDD markdown-v2; Paper 26.2 playerdata; GrantLedger; PDC receipt; persistent physical money |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.grant-save-commit-cat-draft.md;docs/cat/drafts/a02-grant-delivery-process.md;docs/cat/drafts/a02-grant-delivery-pi.md;docs/cat/drafts/a02-grant-delivery-tce.md;.github/workflows/a02-grant-delivery-cat-draft.yml | — | .github/workflows/a02-grant-delivery-cat-draft.yml |

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

## Existing evidence and normative limits

- Approved product 03/02B requires UUID+reservation identity and restart-stable pending delivery until the exact physical coins and PDC receipt are persisted as one playerdata generation; receipt-only or ledger-only is never an actual issue.
- [#434](https://github.com/surumeneco/SimpleCoins/pull/434) demonstrated genuine Paper 26.2 `CraftPlayer.saveData()/loadData` and silent non-durable `saveData()` return under disabled writing/IO fault; verified persisted item and PDC before ACK and post-SIGKILL acknowledgment recovery on another JVM. [#437](https://github.com/surumeneco/SimpleCoins/pull/437) added full-inventory refusal and preservation of unrelated existing stored items.
- `GrantLedger.acknowledge(grant,seq)` is a lower-level API trusting a caller-supplied sequence; a standalone Java filesystem probe succeeded at ACK without any real playerdata. This is a caller proof obligation; **no live issuance service connected in SimpleCoinsPlugin has been shown to exploit it**.
- Draft Process/PI/TCE in `docs/cat/drafts/a02-grant-delivery-*` specify candidate proof-before-ACK, no delivery on failed save, owner of recovery, and explicit uncertainty. They **cannot self-promote to Confirmed**. Full inventory, logout, race, OS power-loss and external Plugin mutation may need refined state abstraction; TCE remains `網羅要求: 部分`.
- [#467](https://github.com/surumeneco/SimpleCoins/pull/467)/[#468](https://github.com/surumeneco/SimpleCoins/pull/468) cover partial-save/final-save-before-ACK crash boundaries; [#473](https://github.com/surumeneco/SimpleCoins/pull/473) adds **real Paper pre-save SIGKILL** and the ninth/tenth independent JVM recovery. Existing durable ACK replay must not cause a new physical delivery; however an acknowledged coin can legitimately leave its original slot later.
- New Draft cases isolate **no-reservation yet contradictory receipt**, **already-ACKed reservation** (idempotent no-op, not new issuance), and **post-proof writer gap** (a finite after-save hash witness is not an atomic verify-to-ACK barrier). Inapplicable/incomplete observation must not be guessed as a safe false contradiction flag.
- Legitimate normative decision must select the guarantee boundary: Paper JVM crash (SIGKILL) vs filesystem/OS power loss, stable physical source evidence, concurrent login/logout and recovery ownership. After that, run upstream process closure/model-conformance/TDD/conformance before any product connection; remain default-deny until exact evidence.
