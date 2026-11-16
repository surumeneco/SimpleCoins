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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43796948573 (real Paper three-process crash injection, not CAT normative Gate) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Observed Paper 26.2 evidence

- Exact PR HEAD `4f6da7bb7d49e654d83c24a2ac46a28b0eaf1830`: official Paper build 134, zero connected players, real `world/players/data/00000000-0000-0000-0000-00000000a002.dat`, independent on-disk `GrantLedger`. [Real Paper CI #43796948573](https://github.com/surumeneco/SimpleCoins/actions/runs/43796948573) succeeded, together with Domain foundation and static Phase6 CI.
- **JVM A**: refused delivery when all 36 storage slots were full; preserved unrelated item; saved one authentic 5000-unit money coin and its receipt PDC together; refused to infer write success from disabled `saveData()` and a swallowed IOException; with journal still pending, ended by actual `SIGKILL` (exit 137) without clean shutdown.
- **JVM B**: restored original physical coin, receipt and unrelated item from saved Paper playerdata; re-opened authentic durable reservation, ACKed once, persisted no pending grants; ended by a second real `SIGKILL` (exit 137) immediately **after** ACK and before clean Paper shutdown.
- **JVM C**: independently re-opened playerdata and journal. Exactly one original reservation remained `delivered=true`, with no pending grants. Original coin denomination/quantity, unrelated item and PDC receipt survived unchanged. Repeating ACK for the same authenticated reservation did not rewrite the journal or playerdata; a fresh `GrantLedger` still showed one delivered and none pending. Source log: `PAPER_A02_SAVE_CRASH_ACK_CRASH_THIRD_BOOT_NO_REISSUE_PASSED`.
- This is an isolated fixture-level boundary proof (not real player network login, source-wide atomic snapshot, OS power loss, multi-reservation partial cursor, or production give/service connection). The CAT Work structural validator returned `contract_valid:true`, `semantic_approval_verified:false`; all formal CAT Gates remain `not-run`.

## Narrow technical acceptance

- Reuse the actual Paper 26.2 synthetic detached Player and UUID `world/players/data/UUID.dat` in an isolated zero-player server with the independent default-deny admission plugin. No actual user data, VPS, generated wallet or production grant service is affected.
- Process A must save genuine 5000-unit coin, matching receipt PDC and unrelated existing item into the same real Paper playerdata file, verify persistent ledger is **pending**, and then be killed with SIGKILL before ACK (existing bounded test).
- Process B must load the **same** physical playerdata, verify coin denomination and receipt PDC, perform one authenticated `GrantLedger.acknowledge`, confirm ledger's durable delivered record, then be killed with SIGKILL **after** ACK without running Paper shutdown hooks.
- Process C must load the **same unchanged playerdata** and persistent ledger, require its **only reservation remains delivered**, `pending(ID)` is empty, physical coin and unrelated item are unchanged, and receipt PDC still refers to the original reservation. A second invocation of acknowledgement with the original authenticated handle must be idempotent, not cause an additional coin, issuance entry or unexpected disk change; re-open the ledger and check its state.
- Negative case: before the ACK stage verify that the ledger still has one pending reservation; after ACK, no new reserve or physical grant is performed. Require all three independent JVM exits and exact logs in the CI. Any missing/duplicate or persistence error fails the test.
- This proves a **local saved-before-ACK and ACK-before-crash boundary**, not integrated player login distribution, OS/power failure durability, multi-grant partial cursor semantics, other-plugin concurrent writes, automatic replay or full G3 exactly-once. Its Work is Draft and may not be self-promoted to Confirmed or CAT formal Gates.
