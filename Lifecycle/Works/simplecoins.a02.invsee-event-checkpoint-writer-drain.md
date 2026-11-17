---
cat_version: '0.2'
kind: work
id: simplecoins.a02.invsee-event-checkpoint-writer-drain
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/499
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

# G3 — InvSee++実writerで「保存イベント許可済み」と「ファイル書込み完了」を区別

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/478 |
| https://github.com/surumeneco/SimpleCoins/pull/486 |
| https://github.com/surumeneco/SimpleCoins/pull/499 |
| https://github.com/Jannyboy11/InvSee-plus-plus/blob/722def99e663561a1e8e4cbc865003af003cdb0e/InvSee++_Platforms/Impl_Paper_26_2/src/main/java/com/janboerman/invsee/paper/impl_26_2/InvseeImpl.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.invsee-event-checkpoint-writer-drain | simplecoins.a02.cooperative-drain-verify-ack;simplecoins.a02.cooperative-veto-fsync-ack |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build134; pinned InvSee++ 0.31.22; SpectatorInventorySaveEvent two priority observers; real offline UUID.dat and SimpleCoins receipt; fresh saved-file proof; CAT issue-work structural validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.invsee-event-checkpoint-writer-drain.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-invsee-post-entry-veto.yml | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-invsee-post-entry-veto.yml |

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

## Technical question

InvSee++ Paper 26.2 `InvseeImpl.save()` calls `EventHelper.callSpectatorInventorySaveEvent` synchronously, checks `event.isCancelled()`, then queues the actual `FakeCraftPlayer.loadData()/saveData()` inside `CompletableFuture.supplyAsync` via `scheduler.executeSyncPlayer`. Existing #486 registered a veto **before** this event and proved that an immediate cancellation keeps the original 5000C coin+receipt through ACK and restart. It did not establish how to account for a writer that has **already passed** the veto checkpoint. #499 tested in-flight permit drain in JUnit, not this real writer.

## Bounded actual-plugin probe

With the actual pinned InvSee++ JAR and default-deny independent guard in a zero-player official Paper JVM:
1. First create genuine saved 5000C PDC currency and completed receipt using the actual product ledger/materializer and `CraftPlayer.saveData` on a detached player. Confirm a fresh positive production `SavedGrantDeliveryFileReadback` while ledger is pending.
2. Register a test-only InvSee++ save listener at LOWEST. It allows the **first** matching save and cancels every later matching save after the test-only gate is closed. Register MONITOR observer which closes the gate as the **first event completes**, i.e. after the first save passed the earlier listener but before `saveInventory` returns and runs the deferred save task. Do not pretend this is an atomic production barrier; the test deliberately closes too late for the first request.
3. The first pending `saveInventory` must return `SaveResponse.saved`; freshly read actual UUID.dat must show 0 saved coin, intact completed receipt and unrelated STONE, and still-unACKed grant. This demonstrates that preventing *new* save events does not revoke the already admitted in-flight writer.
4. A second actual InvSee++ API save must return `notSaved` after gate closed; final count remains 0 and exactly one event was allowed, second vetoed. A fresh readback must reject the original proof. Do not ACK or publish; keep guard closed.
5. Clear event listeners on shutdown. The test does not simulate a closed writer set that already drained: #499's permit model describes the additional condition needed before the first readback used for ACK.

This is a precise **real plugin** schedule/effect witness, not a general asynchronous OS race, a production fencing implementation, a real online player trade, or all-writer proof. No production source, config, actual server world, `main` or admission is changed; Work `status:draft`, formal CAT Gates not-run.

## Acceptance

Official pinned Paper and InvSee++ hash checks, real readback and exact two event outcomes, saved physical coin loss with matching receipt and original durable reservation still pending, guard default-deny, no ACK and no data loss outside disposable test world. Dedicated real Paper CI plus Domain/Phase6 exact-head checks must pass before scoped merge to `develop`. Avoid treating Green failure reproduction as G3 safety completion.
