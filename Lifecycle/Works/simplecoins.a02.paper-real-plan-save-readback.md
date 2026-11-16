---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-real-plan-save-readback
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/450
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

# A02/G3 — 実Paper保存の前後で既存Grant部品を接続するreadback実験

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/450 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantInventoryMaterializer.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-real-plan-save-readback | simplecoins.a02.after-ack-crash-replay-verification;simplecoins.a02.paper-save-commit-verification |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; isolated detached NMS CraftPlayer; real UUID playerdata + inventory + receipt PDC; loaded GrantLedger and verified discovered file manifest |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-real-plan-save-readback.md;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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

## Purpose and bounded acceptance

- The adopted 03 grant contract requires saved inventory and completed/partial grant receipt in one authoritative playerdata generation; **only then** can the reserved issuance be ACKed. Failed saves and full storage must not advance the ledger. G0 and cross-plugin world snapshot remain separate.
- Existing Paper #450 demonstrates manual test-only coin+receipt writes, SIGKILL before/after ACK, and three-process replay, but does NOT actually call the available **production** `PaperGrantInventorySlotPlan.stage`, `PaperGrantInventoryMaterializer.buildCopy` and `SavedGrantDeliveryFileReadback.verify` in one physical-save path.
- Run the earlier 3-phase crash test unchanged. In an **additional fourth independent Paper process**, load that already-saved real 5000 coin, original unrelated stone stack and durable ACK1; confirm the prior state and save file. Reserve exactly one new 5000 monetary grant for the same UUID, stage from real Paper inventory and receipt, make a detached storage candidate, re-check no drift, write both inventory and receipt on the real isolated CraftPlayer, call actual `Player.saveData()`, read the same UUID file through discovered shared `players/data` and verify with production saved-file transition proof. Require all previous occupied storage and money identities unchanged, the new actual coin matching the next plan and receipt, and a fully completed proof before journal ACK2. A re-opened ledger must show exactly two delivered grants and zero pending without reissue.
- The negative control invokes the independent readback **before the second physical save**, when the staged receipt does not yet exist on disk, and requires an exception. Assert the ledger is still pending. This makes the test meaningful even if a helper is accidentally bypassed.
- This is verification-only `issue-work`: product delivery service **is not installed or enabled**. No online Player, no live/server world money, no command, no opening admission, no journal policy change; the test's new reserved grant is isolated inside its own CI world. Passing a local readback does not establish disk fsync, OS power-loss durability, post-read concurrent writer exclusion, full exactly-once for a connected player, or CAT Confirmed/Gates. Those need a properly authorized implementation Work and additional fault tests.
