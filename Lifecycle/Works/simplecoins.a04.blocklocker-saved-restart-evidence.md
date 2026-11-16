---
cat_version: '0.2'
kind: work
id: simplecoins.a04.blocklocker-saved-restart-evidence
status: draft
process: simplecoins.settlement.ownership-evidence
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/451
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A04 — 実Paper再起動後のBlockLocker保護解釈の保持

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/401 |
| https://github.com/surumeneco/SimpleCoins/pull/402 |
| https://github.com/surumeneco/SimpleCoins/pull/451 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.ownership-evidence | simplecoins.a04.blocklocker-saved-restart-evidence | simplecoins.a04.blocklocker-saved-sign-paper-source;simplecoins.o0.blocklocker-115-live-owner-evidence |

## Technologies

| Technology |
| --- |
| Java 25; official Paper 26.2 build 134; SHA-pinned BlockLocker 1.15; actual saved world and restarted Paper process |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.blocklocker-saved-restart-evidence.md;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;.github/workflows/paper-blocklocker-115-live-smoke.yml | — | src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;.github/workflows/paper-blocklocker-115-live-smoke.yml |

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43798718772 (real Paper restart and provider readback; CAT normative Gates not-run) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Verified saved-world restart result

- [Official Paper 26.2 build 134 + SHA-256-pinned BlockLocker 1.15 CI #43798718772](https://github.com/surumeneco/SimpleCoins/actions/runs/43798718772) successfully executed initial-world creation, standalone offline Anvil sign-source verification and a **second independent Paper JVM** reading the same World UUID (`5c0399c7-0742-4ede-9c71-441224881bd0`) and persisted fixture height Y=118.
- No fixture chest or sign was rebuilt in the second JVM. The actual loaded BlockLocker API via `BlockLockerLiveEvidence.observe` returned the original UUID `11111111-1111-4111-8111-111111111111` for the personal chest and both 54-slot double-chest halves across the Chunk boundary. Front and back `[Everyone]` still overrode unknown profile, unprotected chest remained Free, and a non-UUID primary remained a fail-closed error. Real sign back text and original World UUID matched. Source log: `PAPER_BLOCKLOCKER_115_SAVED_OWNER_AND_EVERYONE_UUID_PASSED` and `PAPER_BLOCKLOCKER_115_RELOADED_PROTECTION_PASSED`.
- The separate offline scanner still found the five saved sign records, whose PDC owner-hints were absent in this fixture; raw front/back text remained present. This does **not** make display names authoritative saved ownership or prove unloaded-source topology without loading.
- CAT Work validation reported `contract_valid:true`, `semantic_approval_verified:false`. Process/Model/Implementation Gates and live VPS acceptance remain not-run.

## Bounded Paper acceptance

- Process A loads exact pinned Paper 26.2 and BlockLocker 1.15, constructs distinct loaded chests with UUID owner, Everyone/front, unprotected, unresolved UUID, cross-chunk double chest, back-side Everyone, and stores its immutable test Y-coordinate. It cleanly saves the world.
- With Paper stopped, the existing PR #451 scanner independently checks **real** saved terrain Anvil sign text/PDC candidate set (five signs in two Chunks). No Bukkit on that stage; missing or malformed source fails.
- Process B restarts **the same saved world** with no new signs or chests created. It uses the original committed `BlockLockerLiveEvidence.observe` against reloaded protected and unprotected chests and proves primary owner UUID, Everyone priority, cross-chunk double-chest identity and unknown-UUID fail-closed are unchanged. It also checks a back-side `[More Users]` / `[Everyone]` exists after reload.
- All tests run under default-deny admission and zero connected real players. This verifies provider behavior for a **deliberately loaded known chunk** after restart; it does not prove that the saved-only scanner can resolve the topology of all unloaded containers or that different writers are frozen in one accounting generation.
- Do not edit product code, actual VPS/world, CAT Confirmed, release or admission.
