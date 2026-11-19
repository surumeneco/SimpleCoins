---
cat_version: '0.2'
kind: work
id: simplecoins.a01.world-registry-transition-veto
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

# A01/G0 — 保存走査中のWorld registry一時的入替を検出し候補を失効させる

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/558 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.world-registry-transition-veto | simplecoins.a01.startup-world-universe-identity;simplecoins.external-storage-provider-integration |

## Technologies

| Technology |
| --- |
| Java 25, Paper 26.2 build 134, JUnit, GitHub Actions |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.world-registry-transition-veto.md;src/main/java/co/surumene/simplecoins/domain/WorldRegistryTransitionWitness.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;src/test/java/co/surumene/simplecoins/domain/WorldRegistryTransitionWitnessTest.java;.github/workflows/paper-saved-discovery-live-smoke.yml;tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java;.github/workflows/paper-zero-player-writer-probe.yml | src/main/java/co/surumene/simplecoins/domain/WorldRegistryTransitionWitness.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java | src/test/java/co/surumene/simplecoins/domain/WorldRegistryTransitionWitnessTest.java;.github/workflows/paper-saved-discovery-live-smoke.yml;tests/paper-zero-player-writer-probe/src/main/java/co/surumene/simplecoins/a01probe/ZeroPlayerWriterProbe.java;.github/workflows/paper-zero-player-writer-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a01-world-registry-transition-witness | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.WorldRegistryTransitionWitnessTest | transition-before-capture;transient-transition-ABA;postcapture-transition;not-signalled-unchanged | WorldRegistryTransitionWitnessTest.java | JUnit XML;GitHub Actions logs | 480 |

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

- Already accepted Drive 02B requires the source universe to be complete, consistent and not silently stale. The previous #558 implementation compares only the initial/final Bukkit World name, UUID, and root identities and actual saved metadata; an intermediate `unload → reload` of the same World with the same name/UUID/physical folder leaves those sets equal but invalidates the original in-memory source generation. This Work adds only the missing registration-transition evidence. It is a technical issue-work Draft, not an independently approved new CAT Process or a G0 all-writer critical section.
- Count all actual `WorldLoadEvent` and non-cancelled `WorldUnloadEvent` notifications in a monotonic lifecycle witness, registered with the real Paper plugin on startup. Capture its generation at the main-thread `ServerLoadEvent` post-load world-root selection. Previous normal world loads *before* the capture are baseline and do not veto.
- At final main-thread handoff after asynchronous disk discovery, require the **same event generation** in addition to #558's exact World name/UUID/folder, shared-player roots, actual metadata, and source-set checks. A transient removal+recreation must fail even when the beginning and ending World sets are identical. On mismatch, do not expose the candidate `Manifest`; preserve admission denied and invoke existing guarded failure/stop.
- If WorldLoad/Unload is observed **after** a successful read-only `SIMPLECOINS_SAVED_DISCOVERY_OBSERVED`, synchronously invalidate `observed()` and use the existing fatal safe-stop callback. Close/disable guards must make events inert. Don't add any new admission/release capability or redesign the saved-world manifest; don’t use polling that can miss transient transitions.
- Positive JUnit: initial capture after old events remains valid, unchanged epoch remains valid and is one-shot. Negative JUnit: one or two transitions after capture cannot be made valid simply by a later identical registry snapshot; a new generation token is distinct and only a deliberate fresh capture can resume. Real official Paper 26.2 startup smoke must retain the success marker and backup fail-stop tests.
- A separate genuine Paper 26.2/Java 25 fourth-JVM test uses only the **CI-only** ZeroPlayerWriterProbe. Once the live product's real `PaperSavedSourceDiscovery.observed()` is populated, create another World using Bukkit `WorldCreator` on main thread. The actual `WorldLoadEvent` must synchronously clear the previously observed Manifest, mark discovery failed and invoke guarded self-stop; it must not require player login, product publication, a mock event or synthetic data. The probe may use an isolated reflective test seam to inspect the private discovery field but no reflective production mutations. Earlier three JVM normal money-writer assertions stay intact.
- **Limits:** Paper event visibility is not proof that all Paper/other-plugin memory and on-disk writers are captured. An unload/load event does not prove saved Entity/Block source quiescence; writer epoch and G0 publication remain blocked. No `main`, production VPS, physical currency mutation, GrantLedger ACK or admission change.
