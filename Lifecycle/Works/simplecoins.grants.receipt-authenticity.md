---
cat_version: '0.2'
kind: work
id: simplecoins.grants.receipt-authenticity
status: draft
process: simplecoins.grant.receipt-replay
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.grants.receipt-authenticity

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grant.receipt-replay | simplecoins.grants.receipt-authenticity | simplecoins.grant.durable-immutability;simplecoins.grant.exactly-once-pending |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.receipt-authenticity.md;src/main/java/co/surumene/simplecoins/domain/GrantLedger.java;src/test/java/co/surumene/simplecoins/domain/GrantLedgerTest.java | src/main/java/co/surumene/simplecoins/domain/GrantLedger.java | src/test/java/co/surumene/simplecoins/domain/GrantLedgerTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| grant-identity | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.GrantLedgerTest | altered-grant-id;altered-amount;foreign-sequence;acknowledged-stale-handle | GrantLedger and persisted records | JUnit XML | 180 |

## Gates

External/semantic gates are reported evidence. `passed` or `not-applicable` requires a concrete evidence/rationale reference.

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

## Source-grounded defect and acceptance

- **Adopted decision**: `03_運営設定・コマンド` specifies a durable UUID/amount/issuer/reservation-ID issuance record, playerdata receipt evidence, replay prevention and rejecting ambiguous/offline delivery. The journal is never money and an unissued reservation is excluded from T.
- **Observed code**: `GrantLedger.wasApplied(Grant,long)` checks `grant.player()` and calls `validateReceipt` on the receipt, but does not verify that the supplied Grant is the stored reservation at that sequence. A fabricated or altered grant having the same player/sequence can return `true`. `acknowledge` checks the UUID only, not the other immutable reservation fields.
- **Acceptance**: no receipt can affirm a grant handle unless its immutable reservation fields match the durable in-memory journal: sequence, id, player, amount, issuer, creation time, physical denomination and coin count. A stale handle whose `delivered` flag changed due to later acknowledgement remains a valid identity; acknowledgement is idempotent. Forged or unknown handles must be rejected without writing the journal or causing issuance. Valid receipt progress and legacy records remain supported.
- **Test first**: independently attempt to replay a forged reservation with the right player/receipt but wrong ID/amount/denomination or unknown sequence, including the `acknowledge` path. Observe expected-reason Red and only then implement one bounded journal identity check, preserving no other runtime behavior.
- **Limit**: checking a grant handle proves only a journal identity match. It is not proof that playerdata physically contains the corresponding money or that crash recovery/partial delivery is exactly-once. The active server must remain default-deny and `main` untouched.
- **CAT authority**: This issue-work Draft maps the already user-approved requirement to a confirmed source defect. No new normative Process/PI/TCE has been Confirmed, and no process-closure/model-conformance/implementation-conformance gate is asserted passed merely because JUnit/CI tests may be Green.
