---
cat_version: '0.2'
kind: work
id: simplecoins.a01.shared-playerdata-cat-draft
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/429
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

# A01 — Paper 26.x共有プレイヤーデータのCAT規範候補

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/429 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.world-source-discovery.pi.manifest;simplecoins.world-source-discovery.tce.manifest | simplecoins.a01.paper26-shared-playerdata-discovery |

## Technologies

| Technology |
| --- |
| CAT markdown-v2; Java 25; actual Paper 26.2 dimension paths; WorldStorageDiscovery |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.shared-playerdata-cat-draft.md;docs/cat/drafts/a01-shared-playerdata-process.md;docs/cat/drafts/a01-shared-playerdata-pi.md;docs/cat/drafts/a01-shared-playerdata-tce.md;.github/workflows/a01-shared-playerdata-cat-draft.yml | — | .github/workflows/a01-shared-playerdata-cat-draft.yml |

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

## Source-backed normative decision scope

- The current Drive 02B requires world data and world-independent playerdata to be inventoried from **actual Paper 26.x storage roots**, with playerdata counted once regardless of WorldScope exclusion. Existing `develop` discovery omits the shared `players/data`; PR #429 fixes the bounded physical-source behavior and has independent Java + actual Paper testing, but remains unmerged as an unconfirmed production implementation Draft.
- These three `docs/cat/drafts/a01-shared-playerdata-*` are **candidate normative Process/PI/TCE artifacts only**. They separate region WorldScope, shared global playerdata, primary/backup distinction, aliases and source closure. Their `TCE` is deliberately `open`, not a fully closed-world account of LIVE/other-plugin races.
- Legitimate review must confirm that `World#getWorldPath` derived level roots are authoritative in all Multiverse arrangements, that shared roots never include unrelated backups, and that any unknown/unreadable required source stops settlement rather than becoming zero. Approving this **subprocess** cannot be interpreted as G0 or complete admission readiness.
- After genuine Confirmed approval, follow exact upstream routing and run canonical `process-closure`, `model-conformance`, TDD Queue/current-generated Red–Green and `implementation-conformance` before promoting #429 to a develop merge. Ordinary CI success and this Draft cannot substitute those requirements.

## Authority

No production file changes, no CAT self-Confirmed action, no release, no gate opening. This Work is specification preparation in `issue-work` only.
