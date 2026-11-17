---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-invsee-actual-offline-writer
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/477
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

# A02/G3 — actual deployed InvSee++ API as an independent Paper playerdata writer

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/475 |
| https://github.com/surumeneco/SimpleCoins/pull/477 |
| https://github.com/Jannyboy11/InvSee-plus-plus/blob/722def99e663561a1e8e4cbc865003af003cdb0e/InvSee++_Platforms/Impl_Paper_26_2/src/main/java/com/janboerman/invsee/paper/impl_26_2/InvseeImpl.java |
| https://github.com/Jannyboy11/InvSee-plus-plus/blob/722def99e663561a1e8e4cbc865003af003cdb0e/InvSee++_Plugin/src/main/java/com/janboerman/invsee/spigot/DefaultScheduler.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-invsee-actual-offline-writer | simplecoins.a02.verify-to-ack-writer-gap;simplecoins.a01.packaged-writer-entrypoints |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; fixed XPlayServer distribution InvSee++.jar SHA256 01f3e053a814f76d46513ebf65a38747df11df81c577d12bed108b353d7775f9; Java 25; InvseeAPI; real compressed playerdata NBT; authentic SimpleCoins grant journal and currency PDC; disposable Paper observer plugin |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-invsee-actual-offline-writer.md;tests/paper-invsee-writer-probe/build.gradle.kts;tests/paper-invsee-writer-probe/plugin.yml;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-invsee-writer-gap-probe.yml | — | tests/paper-invsee-writer-probe/build.gradle.kts;tests/paper-invsee-writer-probe/plugin.yml;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-invsee-writer-gap-probe.yml |

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

## Bounded experiment and interpretation

The pinned server JAR bytes contain `InvseeImpl.class → FakeCraftPlayer.saveData` for the Paper 26.2 class. The upstream current source confirms that its `InvseeAPI.saveInventory` can replay a detached player's storage contents into playerdata, with the save executed through an `executeSyncPlayer` executor (so do not incorrectly assert unrestricted simultaneous async data writes). API entrypoint and actual loaded JAR version must be checked in real Paper, not inferred from a symbolic constant-pool reference.

On an isolated, loopback-only disposable Paper server: (1) create a detached NMS player and an authentic pending SimpleCoins grant, (2) save the empty physical playerdata, (3) materialize the actual 5000 C coin and signed SimpleCoins receipt into one saved playerdata generation and verify this using the production SavedGrantDeliveryFileReadback against the real discovered world/player source, (4) use the separately loaded, **actual packaged** InvSee++ offline API, with an offline spectator storage copy, to deliberately remove that monetary coin while preserving unrelated items and the receipt, (5) read the actual saved NBT, reject the previous A02 proof on a new readback, and demonstrate why blindly acknowledging with the earlier proof's sequence is unsafe. All file modifications are **test-only on a new temporary Paper world**. No client or regular player joins; this is a probe of real plugin off-line save lifecycle, not an exploit of operator permissions on the deployed server.

**A successful observation means a concrete competing writer path exists and can invalidate an old saved-file proof before ACK. It does NOT mean real-world race timing, scheduling locklessness or a 1.0.0 solution are proven.** In particular, when this InvSee++ build routes offline saves onto Paper's main thread, a correctly bounded main-thread non-yielding verify→ACK section could serialize that specific writer; the property must be verified under real scheduling. External filesystem writers, crash durability, queued callbacks, connection/quit and total G0 source closure remain distinct. This Work is only independent issue-work verification. CAT Process/PI/TCE remain Draft; formal Gates not-run. No production, admission, main or VPS changes.

## Acceptance

- Exact packaged InvSee++ Git blob / SHA256 checked before use; source-level API version not substituted for binary.
- Real Paper logs prove the initial saved exact coin+receipt and a valid independent readback, then a genuine InvSee++ API save and actual changed UUID.dat.
- Fresh readback denies the stale proof; raw ledger ACK and startup receipt audit observation are reported distinctly, with explicit limitations.
- Crash/timeout or API errors cause the CI experiment to fail (no zero-value assumption); no accidental product JAR probe class.
- Pinned CAT Work structure, CI and develop readback independently recorded; no normative approval claimed.
