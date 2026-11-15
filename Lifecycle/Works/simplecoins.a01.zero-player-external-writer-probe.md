---
cat_version: '0.2'
kind: work
id: simplecoins.a01.zero-player-external-writer-probe
status: draft
process: simplecoins.settlement.source-generation
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
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

# A01 — 0人でも外部writerによって貨幣正本が変化する実Paper検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperCapturedHybridSettlement.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.source-generation | simplecoins.a01.zero-player-external-writer-probe | simplecoins.settlement.admission-barrier;simplecoins.a01.playerdata-backup-orphan-guard |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 build 134; disposable Bukkit writer plugin; actual Anvil region; process restart |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.zero-player-external-writer-probe.md;tests/paper-zero-player-writer-probe/*;.github/workflows/paper-zero-player-writer-probe.yml | — | tests/paper-zero-player-writer-probe/*;.github/workflows/paper-zero-player-writer-probe.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43728221982 (bounded Paper source proof and canonical Work structure; not normative CAT conformance) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Source-backed scope and acceptance

- The existing 02B normative design already explicitly states that zero online players does **not** prove that loaded chunks or external plugins are quiescent. This Work tests that environmental fact, not a new monetary design choice.
- Run only an isolated Paper 26.2 build 134 JVM with independent default-deny guard and **zero connected players**, bound to loopback with no real server world data. The probe is a separate disposable plugin, never packaged into SimpleCoins.
- Boot A: the external writer (different plugin from SimpleCoins) places an authentic tagged currency item in a chest in a loaded chunk and saves world; verify exact in-memory type/value and actual region file presence. Exit without opening player admission.
- Boot B: with still zero connected players, read the chest and increment physical coin count in the same existing block inventory; persist, require the region bytes to differ, and exit. This is a legitimate external plugin writer while no player is online.
- Boot C: after a separate Paper restart, reload the same chest from actual Anvil storage and verify the new count and authentic coin identity. All phases must emit individual diagnostic markers and fail if another count, source, or path is observed.
- Interpret only the observed implication: player count zero, without a stronger writer barrier / certified generation protocol, is **not** a complete G0 consistency guarantee. The probe does not implement a writer barrier, measure all plugin interleavings, prove A01/G0, or relax admission.
- If a Paper asynchronous/chunk save race, world-layout mismatch or unknown writer effect causes ambiguity, record it as a failing/rejected test, not a successful accounting snapshot.

## Authority

- Draft verification under `issue-work`: no production code, normative Confirmed, source schema, user session, deployment or public gate action.
- External CI proves only the actual probe. CAT formal closure, model, implementation conformance and semantic approval remain not-run; do not infer them from CI Green.
## Observed bounded result (2026-11-15)

- On a disposable official Paper 26.2 build 134, with an independent admission default-deny guard and no network players, the separate writer plugin saved one genuine 5000-unit coin in a loaded overworld chest. Actual `dimensions/minecraft/overworld/region/r.0.0.mca` SHA-256: `408944f75eeb8697e34aec8cad62625d10fca1d92969fa08ac4d5a67f9dd54c2`.
- Next clean process start under the same zero-player condition observed one authentic coin, changed it to two and saved. Region SHA-256 changed to `9e5a5906748eed7ac945bb0b570bf728b04b8ea0258c2c96941b61ed2b4f7145`.
- Third process start loaded two authentic coins in the same chest and finished without exposing admission. Its entire region hash changed again due to ordinary save/reload metadata; the **real ItemStack state**, not unchanged bytes across an extra startup, is the correct third-stage oracle.
- Real Paper CI [#43728221982](https://github.com/surumeneco/SimpleCoins/actions/runs/43728221982) passed, including the fixed pinned CAT Work `validate` (`contract_valid=true`, `semantic_approval_verified=false`). Domain and Phase6 static workflows at this HEAD also passed. None is a formal normative Gate or proof of arbitrary external writer quiescence.
- Therefore a G0 mode that treats a player count of zero as equivalent to absence of monetary writers violates the already accepted 02B assumption. A stronger provable writer barrier, source-generation reconciliation or enforced fail-closed mode is necessary. This Work selects **no** implementation architecture.

