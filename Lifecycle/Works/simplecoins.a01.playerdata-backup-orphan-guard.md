---
cat_version: '0.2'
kind: work
id: simplecoins.a01.playerdata-backup-orphan-guard
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.a01.playerdata-backup-orphan-guard

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java |
| https://github.com/PaperMC/Paper/blob/ver/26.2/paper-server/patches/sources/net/minecraft/world/level/storage/PlayerDataStorage.java.patch |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.playerdata-backup-orphan-guard | simplecoins.world-storage-discovery;simplecoins.a01.saved-source-closure |

## Technologies

| Technology |
| --- |
| Java 25 / Paper 26.2 saved playerdata, JUnit 5, Java NIO filesystem |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.playerdata-backup-orphan-guard.md;src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java;src/test/java/co/surumene/simplecoins/domain/WorldStorageDiscoveryTest.java | src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java | src/test/java/co/surumene/simplecoins/domain/WorldStorageDiscoveryTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| playerdata-backup-closed | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.WorldStorageDiscoveryTest | missing-primary;backups-modern-legacy;live-primary-not-duplicated;noncanonical-backup;excluded-world | WorldStorageDiscovery;Paper26.2 PlayerDataStorage | JUnit XML | 180 |

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

## Source-grounded acceptance

- Adopted 02B/G0: Minecraft playerdata is world-independent accounting data; missing authoritative saved sources must not be silently treated as empty currency holdings, and unidentified sources invalidate publication.
- The Paper 26.2 source `PlayerDataStorage.save` writes a backup `<UUID>.dat_old` via `Util.safeReplaceFile(realFile,tmpFile,oldFile)`. The existing SimpleCoins discovery only recognizes `<UUID>.dat`, silently disregarding a backup left as the sole player record after a failed save/rename. This is a saved source closure risk, **not evidence the backup can safely be used as currency authority**.
- In each recognized legacy `playerdata/` and modern `players/data/` directory, validate canonical UUID spelling for `.dat_old` and treat it as *potential recovery evidence*, not an independent counted asset. When a backup exists without a canonical sibling `.dat`, fail the entire manifest discovery even for an excluded WorldScope. Do not silently count the backup as money or import it in place of `.dat`.
- When a valid primary `.dat` exists, backups are not added to manifest/player count or currency totals. Detect a nonregular primary instead of treating it as absent. Reject malformed backup filename and nonregular `.dat_old` file as potentially unaccounted source.
- Use deterministic JUnit fixtures for current/missing-primary/both-layout/invalid filename/excluded scope and fail-closed behavior. No `main`/runtime adapter/admission/physical grant mutation is permitted.
- This improvement is **not** a proven Paper recovery procedure, a proof of SAVE synchronization, or a global world atomicity Gate. The correct fallback and playerdata crash protocol still require real Paper tests. No AI-provided Draft is Confirmed; process closure, model and implementation conformance remain not-run.
