---
cat_version: '0.2'
kind: work
id: simplecoins.a01.startup-world-universe-identity
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

# A01/G0 — 起動時のPaper登録World集合を保存ソース正本と照合する

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/533 |
| https://github.com/surumeneco/SimpleCoins/pull/555 |
| https://github.com/surumeneco/SimpleCoins/pull/557 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.startup-world-universe-identity | simplecoins.a01.paper26-shared-playerdata-discovery;simplecoins.external-storage-provider-integration |

## Technologies

| Technology |
| --- |
| Java 25, Paper 26.2 build 134, JUnit, GitHub Actions |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.startup-world-universe-identity.md;src/main/java/co/surumene/simplecoins/domain/WorldSourceIdentitySnapshot.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;src/test/java/co/surumene/simplecoins/domain/WorldSourceIdentitySnapshotTest.java;.github/workflows/paper-saved-discovery-live-smoke.yml | src/main/java/co/surumene/simplecoins/domain/WorldSourceIdentitySnapshot.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java | src/test/java/co/surumene/simplecoins/domain/WorldSourceIdentitySnapshotTest.java;.github/workflows/paper-saved-discovery-live-smoke.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| registered-world-identity | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.WorldSourceIdentitySnapshotTest | exact-registered-roots;world-add-remove;UUID-path-replacement;duplicate-worlds;missing-manifest | WorldSourceIdentitySnapshotTest.java | JUnit XML;Actions logs | 480 |

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

- Drive 02B has already determined that a settlement must include the complete selected world/holder universe and cannot treat unobserved money as zero. This Work only implements a **read-only product precondition** for saved-source discovery, never new accounting semantics. Work is a draft issue-work artifact; it does not self-confirm Process/PI/TCE, assert passed CAT Gates or grant admission.
- At existing `ServerLoadEvent` postload main-thread stage, obtain the **entire current Bukkit.getWorlds()** set with exact World name, UUID and actual `World.getWorldPath()` plus the existing shared player roots. Reject missing, duplicate, swapped identities; do not infer UUID from name or file path.
- The asynchronous `WorldStorageDiscovery.discover` Manifest must have a genuine discovery origin with **exact** world name and normalized path coverage (even empty/excluded worlds). Resolve the saved UUID from per-dimension `data/paper/metadata.dat` with existing `PaperSavedWorldMetadataWitness.capture`, require match to the originally captured Bukkit IDs and hold a drift witness. Existing bounded saved GrantLedger audit and file manifest re-discovery remain required.
- Only after the asynchronous scan/audit completes, schedule a **main-thread** comparison with the CURRENT entire `Bukkit.getWorlds()` set. Reject added, removed, renamed, UUID-replaced or path-replaced worlds and changed shared player roots. Recheck stored World metadata witness. A mismatch/exception must call existing safeStop, preserve admission default-deny and never report or expose an observed Manifest.
- `PaperSavedSourceDiscovery.observed()` becomes populated only **after** this final check, and timeout/closed/fatal paths must not publish it later. Preserve the existing single-run watchdog. The existing Paper startup/backup fail-stop tests must remain green; additionally assert a specific actual Paper success marker for the in-between World identity comparison.
- This witness is a point-in-time positive precondition only. It proves neither later World registrations/writers, LIVE value immutability, end-to-end G0 world/source/owner epoch, atomic publish, G3 ACK, 1.0.0 readiness nor an admission permit. It must not alter `main`, VPS or production guard.
