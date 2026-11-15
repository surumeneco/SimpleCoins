---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper26-shared-playerdata-discovery
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

# Work: simplecoins.a01.paper26-shared-playerdata-discovery

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/428 |
| https://github.com/surumeneco/SimpleCoins/pull/427 |
| https://github.com/PaperMC/Paper/blob/ver/26.2/paper-server/src/main/java/org/bukkit/craftbukkit/CraftWorld.java |
| https://github.com/PaperMC/Paper/blob/ver/26.2/paper-api/src/main/java/org/bukkit/World.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.paper26-shared-playerdata-discovery | simplecoins.a01.playerdata-backup-orphan-guard |

## Technologies

| Technology |
| --- |
| Java 25, Paper 26.2 build 134, JUnit, GitHub Actions |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper26-shared-playerdata-discovery.md;src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;src/test/java/co/surumene/simplecoins/domain/WorldStorageDiscoveryTest.java;.github/workflows/paper-saved-discovery-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java | src/test/java/co/surumene/simplecoins/domain/WorldStorageDiscoveryTest.java;.github/workflows/paper-saved-discovery-live-smoke.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| source-discovery-regression | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.WorldStorageDiscoveryTest | modern-global-playerdata;orphan-global-backup;unmodified-discovery | WorldStorageDiscoveryTest.java | JUnit XML;GitHub Actions logs | 480 |

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

## Source-grounded acceptance

- Paper 26.2 `World#getWorldFolder()` resolves a per-dimension storage path, whereas playerdata is located in the level root `players/data`. The current Paper integration must account for both without inventing additional monetary authority or silently treating missing playerdata as zero.
- Enumerate loaded dimensions by their actual Paper world name under the existing WorldScope policy. Independently discover shared top-level `players/data` and legacy `playerdata` once per level storage root, even if all corresponding dimensions are scope-excluded.
- A shared player's `UUID.dat` is recorded once; orphan `UUID.dat_old`, conflicting UUIDs, symlinked source, changed source set and visit-budget exhaustion fail closed. Do not scan an unrelated backup tree or allow overlapping region roots. Include these roots in repeated discovery closure.
- Evidence is a structural implementation Draft linked to existing 02B design source; source writes and ordinary CI must not be misrepresented as completed CAT normative Gates. Normative Process/PI/TCE confirmation and formal CAT gates remain not-run; no admission release, atomic world settlement, complete G0 or G3 is claimed.

## Current implementation evidence

- Paper 26.2 API: `CraftWorld#getWorldPath()` returns `storageSource.getDimensionPath(world.dimension())`; `World#getWorldFolder()` delegates to that path, not the global level root.
- The independent `sharedPlayerRoots` parameter captures level roots once on the main thread and scans only `players/data` or legacy `playerdata`, preserving per-world WorldScope for regions.
- A source-set recheck uses the same shared-root provenance. CI/fault-injection and user normative approval remain distinct from draft implementation.
