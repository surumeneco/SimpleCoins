---
cat_version: '0.2'
kind: work
id: simplecoins.a02.after-ack-crash-replay-verification
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/434
  - https://github.com/surumeneco/SimpleCoins/pull/437
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A02 — 実PaperのACK後クラッシュ・次回起動時の二重配布拒否

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/434 |
| https://github.com/surumeneco/SimpleCoins/pull/437 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.after-ack-crash-replay-verification | simplecoins.a02.paper-save-commit-verification |

## Technologies

| Technology |
| --- |
| Java 25; actual Paper 26.2 build 134; real UUID playerdata; independent persisted GrantLedger; SIGKILL |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.after-ack-crash-replay-verification.md;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Narrow technical acceptance

- Reuse the actual Paper 26.2 synthetic detached Player and UUID `world/players/data/UUID.dat` in an isolated zero-player server with the independent default-deny admission plugin. No actual user data, VPS, generated wallet or production grant service is affected.
- Process A must save genuine 5000-unit coin, matching receipt PDC and unrelated existing item into the same real Paper playerdata file, verify persistent ledger is **pending**, and then be killed with SIGKILL before ACK (existing bounded test).
- Process B must load the **same** physical playerdata, verify coin denomination and receipt PDC, perform one authenticated `GrantLedger.acknowledge`, confirm ledger's durable delivered record, then be killed with SIGKILL **after** ACK without running Paper shutdown hooks.
- Process C must load the **same unchanged playerdata** and persistent ledger, require its **only reservation remains delivered**, `pending(ID)` is empty, physical coin and unrelated item are unchanged, and receipt PDC still refers to the original reservation. A second invocation of acknowledgement with the original authenticated handle must be idempotent, not cause an additional coin, issuance entry or unexpected disk change; re-open the ledger and check its state.
- Negative case: before the ACK stage verify that the ledger still has one pending reservation; after ACK, no new reserve or physical grant is performed. Require all three independent JVM exits and exact logs in the CI. Any missing/duplicate or persistence error fails the test.
- This proves a **local saved-before-ACK and ACK-before-crash boundary**, not integrated player login distribution, OS/power failure durability, multi-grant partial cursor semantics, other-plugin concurrent writes, automatic replay or full G3 exactly-once. Its Work is Draft and may not be self-promoted to Confirmed or CAT formal Gates.
