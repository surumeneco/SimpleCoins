---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-playerdata-fsync-abrupt-recovery
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/478
  - https://github.com/surumeneco/SimpleCoins/pull/481
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

# A02/G3 — Paper playerdata fsync→ACK後の非正常終了と独立再起動

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/434 |
| https://github.com/surumeneco/SimpleCoins/pull/450 |
| https://github.com/surumeneco/SimpleCoins/pull/475 |
| https://github.com/surumeneco/SimpleCoins/pull/478 |
| https://github.com/surumeneco/SimpleCoins/pull/481 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-playerdata-fsync-abrupt-recovery | simplecoins.a02.paper-invsee-actual-offline-writer;simplecoins.a02.paper-selected-mainhand-mirror |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134, Java 25, Linux FileChannel.force(true), verified exact compressed UUID.dat, same real SimpleCoins GrantLedger, genuine cold-restart Paper JVM |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-playerdata-fsync-abrupt-recovery.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-forced-playerdata-sync-recovery.yml | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-forced-playerdata-sync-recovery.yml |

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

## Boundary and negative evidence

Existing [#450](https://github.com/surumeneco/SimpleCoins/pull/450) proves SIGKILL after saved playerdata and ACK at a real Paper/JVM boundary but does **not** explicitly sync the stored playerdata bytes or its containing directory. The product `DurableFile` synchronizes each journal record before atomic installation and `fsync`s the journal directory; Paper `CraftPlayer.saveData()` returns void, with no comparable durability completion value. Such a returning method must not be interpreted as an OS power-loss persistence guarantee.

This experiment differs in one material respect: it deliberately calls `FileChannel.force(true)` on the actual compressed `world/players/data/<UUID>.dat` **and its parent directory**, checking for nonregular/symlink inputs, then obtains a **fresh product** `SavedGrantDeliveryFileReadback` and writes the normal `GrantLedger.acknowledge` only after successful sync. The grant plan places one genuine 5000C stack at slot 1, retains an unrelated stone stack in selected hotbar slot 0 (avoids the separate #480 empty-equipment bug), and persists the signed receipt in the same real UUID.dat. The first disposable Paper JVM then exits via `Runtime.halt(137)`, with no Bukkit shutdown hooks, and the workflow requires that exact abrupt exit status. An **independent Paper 26.2 JVM** boots the *same* world and product plugin, verifies the 5000C saved physical asset, receipt sequence and nonactive state, complete journal history, zero pending reservation, and continued default-deny admission. A clean Paper shutdown would be a false test, so it is not accepted as the first phase.

All file sync/ack experiments are **test-only in a newly created temporary server**. No product `saveData` adapter, journal API, admission, actual server, `main`, or deployment is modified. Run the existing three independent InvSee++ experiments as regression on the changed probe test class.

## Interpretation limits

A successful two-JVM result establishes only that explicit Linux file/directory force and the existing journal ACK are *feasible in the pinned Paper 26.2 fixture* and survive a JVM process halt. It does not simulate kernel crash, device write-cache loss, power failure, mount/symlink ancestor races, an interrupted `force`, or any noncooperating writer overwriting the saved file after the sync. The critical verify→ACK interval remains unprotected, and post-ACK legitimate coin movement must not be mistaken for a rollback. The proposed product design still needs an actual writer-generation exclusion/fence, correct handling of save failure and partial progress, a durable restart protocol, and legitimately Confirmed CAT specification/Gates before being wired into SimpleCoins. Do not promote this Work or count this as G3 completion.

## Acceptance

- Fixed Paper/InvSee server distribution and exact verified product JARs; no online clients.
- First JVM: real saved coin+receipt, successful file + directory force, valid reread, ACK after force, abrupt halt exit 137; journal never ACKs before force.
- Second independent JVM: original 5000C+receipt and exactly one durable ACK recovered, pending empty and guard still blocked.
- Existing #478/#479/#480 smoke paths, Domain and Phase6 not regressed.
- Work structurally valid; no formal CAT Gate claim.
