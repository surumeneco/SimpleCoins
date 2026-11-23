---
cat_version: '0.2'
kind: work
id: simplecoins.a02.real-paper-protected-saved-stages
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/636
  - https://github.com/surumeneco/SimpleCoins/pull/638
  - https://github.com/surumeneco/SimpleCoins/pull/642
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on:
  - simplecoins.a02.protected-saved-ack-bridge
  - simplecoins.a02.protected-partial-saved-progress
  - simplecoins.a02.paper-real-plan-save-readback
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A02/G3 — real detached Paper player save through protected product stages

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/636 |
| https://github.com/surumeneco/SimpleCoins/pull/638 |
| https://github.com/surumeneco/SimpleCoins/pull/642 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.real-paper-protected-saved-stages | simplecoins.a02.protected-saved-ack-bridge;simplecoins.a02.protected-partial-saved-progress |

## Technologies
| Technology |
| --- |
| Java25; official Paper 26.2 build134; real CraftPlayer.saveData, detached offline player, genuine coin/PDC, 10 independent Paper JVM save/SIGKILL/restore; existing GrantLedger, SavedGrantProtectedAckStage, SavedGrantProtectedPartialStage |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.real-paper-protected-saved-stages.md;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

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

## Bounded verification contract

Existing 10-JVM Paper 26.2 proof uses real detached `CraftPlayer.saveData/loadData`, saved `players/data/UUID.dat`, real `CoinItemCodec`, `PaperGrantInventorySlotPlan`, `PaperGrantInventoryMaterializer`, `PaperGrantReceiptPdc`, `SavedGrantDeliveryFileReadback` and durable `GrantLedger`. It includes genuine persisted 5000C single-batch grant, 130×5000C split into 128-coin initial 640000C partial then last 2-coin final, both no-premature-ACK and SIGKILL/reboot recovery paths. Current product components `SavedGrantProtectedAckStage.finish` (#636/#638) and `SavedGrantProtectedPartialStage.verifyPartial` (#642) have been tested against genuine compressed NBT fixtures but not called on the real Paper-saved file from this end-to-end probe.

Without adding another production entrypoint, extend **existing isolated disposable Paper probe** to pass the actual post-`CraftPlayer.saveData` Manifest, authentic Ledger/grant, exact `PaperGrantInventorySlotPlan` and pre-save root through these **existing package-private unregistered product components**, by reflection from the test class loader (no product method exposed to user commands). For the genuine single-batch grant 2, let `SavedGrantProtectedAckStage.finish` perform the *actual* file/dir fsync, readback and durable ACK **instead of** independent direct test code acknowledging the same proof. For the first partial batch in grant 3, let `SavedGrantProtectedPartialStage.verifyPartial` establish saved active cursor 640000C, same 2 physical stacks and pending; preserve the already-tested deliberate low-level premature-ACK refusal. Existing final-batch separate-JVM save/recovery remains independent of this bounded product path and MUST NOT call the **single-batch** final helper on a previous partial receipt. Preserve all previous low-level readback assertions as independent oracles.

Each real-Paper product invocation creates a **TEST-ONLY**, closed/drained `WriterEpochFence` after the actual playerdata save. This does NOT prove entry/exit coverage during the preceding save, that all Paper+Plugin+async+direct-file writers stopped, or G3 live admission; and it must not touch the product startup writer fence, Agent state, installed real `AdmissionPermit` or production player inventory. Add explicit marker strings for these bounded product checks, require the markers in JVM 4 and JVM 5 CI logs. The persisted 2nd and 3rd journal records, physical amounts and 10-JVM SIGKILL/reboot invariants must remain exactly as before. Reuse existing official Paper build 134 SHA verification, immutable Source Manifest and existing CAT issue-work; validate new Work with pinned CAT source. No synthetic all-writer closure claim, no main/VPS/release.

Acceptance: real Paper 5000C save then protected fsync+fresh proof+ACK in same local test seal and durable journal after restart; real Paper 128×5000C first partial save then protected proof with no ACK, subsequent SIGKILL restart with cursor/pending and exact remaining 2 delivered (existing independent path); real guard default-deny in all JVMs. If actual Paper equipment serialization differences block the protected transition, do not paper over the mismatch: record and reproduce before proposing a narrow input normalization under correct authority.

This Work is **verification** within issue-work, not a new normative A02 confirmation, not script-owned Implementation Conformance, and not G3 distribution enablement.
