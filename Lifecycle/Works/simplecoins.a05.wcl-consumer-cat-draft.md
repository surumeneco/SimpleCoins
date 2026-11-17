---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wcl-consumer-cat-draft
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/4
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
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

# A05/G1 — WCLサービスから決算候補への製品接続 CAT承認済み仕様

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/4 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-wcl-provider-contract.md |
| https://github.com/surumeneco/SimpleCoins/pull/445 |
| https://github.com/surumeneco/SimpleCoins/pull/455 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.external-storage-provider-integration;simplecoins.external-storage-provider-integration.pi.capture;simplecoins.external-storage-provider-integration.tce.capture | simplecoins.owner.wcl-provider-inventory;simplecoins.world-source-discovery |

## Technologies

| Technology |
| --- |
| CAT markdown-v2; Java 25; Paper 26.2; WCL WonderfulStorageReader; WWC/WWW providers |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wcl-consumer-cat-draft.md;docs/cat/confirmed/a05-wcl-consumer-process.md;docs/cat/confirmed/a05-wcl-consumer-pi.md;docs/cat/confirmed/a05-wcl-consumer-tce.md;.github/workflows/a05-wcl-consumer-cat-draft.yml | — | .github/workflows/a05-wcl-consumer-cat-draft.yml |

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

## Entry / definition of done

01/02A/02B/05のユーザー確定要求と、既存の配布WCL API実測を、この連携だけのProcess/PI/TCEへ分解し、2026-11-18の利用者承認に基づいてConfirmed昇格する。これまで確認済みの同じ実Paper証拠をもう一度検証するためのWorkではない。新たなAPIやテスト用の偽readerを追加しない。製品での読取接続は正式CAT Gate通過後の独立implementation Workで扱う。

- Provider集合の全取得（単一`ServicesManager.load`ではない）、有効Plugin/配布JARとの一致、同一EntityのLIVE/SAVEDの重複防止、未ロードのPDC供給、読取結果のSuccess/NotFound/Failure、関係UUID、nested ItemStackの帰属候補とG0同世代を明記。
- `follow_target`/`commander`/`owner`の不在と不明、検証済みFREEと不明の区別を保持し、承認済みの指揮者→飼い主→FREE優先順位や現在BAN/playerdataの判定を変更しない。
- CAT linterと`cat_flow validate`で構造だけ確認。ユーザー承認によるConfirmedと、未実行のProcess Closure/Model/実装Gate、製品配線・実VPS読取完了を混同しない。次は正式Gate後の`spec-implementation`へ引き渡し、G0/G3不成立中はAdmission default-denyを維持する。
