---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-main-worker-saved-commit
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/647
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on:
  - simplecoins.a02.paper-saved-batch-product-coordinator
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — hand off actual Paper save to protected off-main disk verification and durable ACK

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/647 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-main-worker-saved-commit | simplecoins.a02.paper-saved-batch-product-coordinator;simplecoins.a02.final-partial-protected-ack |

## Technologies
| Technology |
| --- |
| Paper 26.2 Java25; main-thread detached Paper Player; background immutable world-source discovery and fresh NBT; GrantLedger; WriterEpochFence; GitHub Actions real multiple-JVM crash regression |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-main-worker-saved-commit.md;src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperGrantSavedBatchCoordinator.java | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates
| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Confirmed behavior; release-blocking runtime scheduling boundary

Current product `PaperGrantSavedBatchCoordinator.saveAndFinalizeFirstBatch` does real PDC/ItemStack and `Player.saveData()` on Paper main, but also synchronously performs full playerdata/Anvil discovery, file/dir fsync, NBT verification and GrantLedger read/write inside that **same main-thread call**. In a large world, that would block Paper tick and is NOT a usable runtime delivery orchestration. The existing current-A02 Confirmed specification requires physical save and signed same-root receipt, exact durable ACK or pending partial before any admission, as before: **no monetary semantic change** is authorized.

Add product-owned **two-stage same-epoch handoff**:
1. `saveAndSealFirstBatchOnMain`: require primary thread, detached authentic Player, no logged-in users, authentic pending grant, saved/memory signed receipt consistency, origin-backed verified manifest, 36-slot immutable plan and real canonical coins; stage inventory/PDC and call actual `Player.saveData`. Immediately SEAL the already OPEN supplied WriterEpochFence, reject any undrained writer without scheduling a main-thread wait, then return an opaque once-only `SealedFirstBatch`. Full inventory returns empty without saving or progressing the durable reservation.
2. `SealedFirstBatch.finishOffMain`: **reject calling on Paper primary thread**; rediscover the source universe only from the stored `Manifest.origin()` immutable Path/WorldScope parameters (never call live Bukkit, recapture live chunks or trust a user-supplied path); under the SAME SEALED epoch validate original playerdata identity and invoke existing protected product `SavedGrantProtectedAckStage.finish` or `SavedGrantProtectedPartialStage.verifyPartial`. This performs file/dir force, fresh physical money+receipt NBT proof, preservation of unaffected items and real GrantLedger ACK only for completed grant. Still require pending grant for partial progress. Terminal method is **one-shot**; failures invalidate the exact fence and require cold recovery, never try to mint a second time.
3. The current synchronous API is retained solely for existing internal test compatibility; neither it nor the new path is registered in normal `SimpleCoinsPlugin`. Later real service must use the two-stage main/worker protocol, a plugin-owned bounded executor/shutdown contract and independently established complete all-writer scope before enabling. A test-only fence cannot certify that.
4. Do not promote G0/G3, arm Agent, permit login, alter `main`, tag, or deploy.

**Red first**: new alternate genuine Paper 26.2 boot on an independently copied third-JVM world reserves grant2, runs the missing `saveAndSealFirstBatchOnMain` on the main thread and requires that an actual independent Java worker (not primary) finishes the saved proof/ACK. Before product change, reflection must fail at method discovery. **Green:** 10,000C original+new physical coins, signed completed seq2, real ledger grants2/pending0 and STONE preserved, all with no finish/fdatasync on the primary thread; subsequent independent Paper restart proves no reissue. The unchanged original ten-JVM G3 crash sequence, original completed and partial product coordinator paths and Domain/Phase6 must remain Green. Inject synchronous-main misuse and repeat-finalization negatives without manufacturing new currency.

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-process.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a02-grant-delivery-tce.md | — | — | — | @work/.cat-flow/cache/a02-worker-model.json | @work/.cat-flow/cache/a02-worker-vectors.json | @work/.cat-flow/cache/a02-worker-queue.json | — | — | @work/.cat-flow/cache/a02-worker-obligations.json | — | false |
