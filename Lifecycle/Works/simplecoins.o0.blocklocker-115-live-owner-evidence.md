---
cat_version: '0.2'
kind: work
id: simplecoins.o0.blocklocker-115-live-owner-evidence
status: draft
process: simplecoins.settlement.ownership-evidence
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# Work: simplecoins.o0.blocklocker-115-live-owner-evidence

## Evidence

| URI |
| --- |
| https://github.com/rutgerkok/BlockLocker/tree/v1.15 |
| https://github.com/rutgerkok/BlockLocker/blob/v1.15/src/main/java/nl/rutgerkok/blocklocker/BlockLockerAPIv2.java |
| https://github.com/rutgerkok/BlockLocker/blob/v1.15/src/main/java/nl/rutgerkok/blocklocker/impl/SignParserImpl.java |
| https://github.com/surumeneco/SimpleCoins/issues/4 |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.ownership-evidence | simplecoins.o0.blocklocker-115-live-owner-evidence | simplecoins.ban-source-coverage |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2; BlockLocker v1.15; reflection-free public API preferred; source-bound compatibility test |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/paper/BlockLockerLiveEvidence.java;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveEvidenceTest.java;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;tests/paper-blocklocker-smoke/*;build.gradle.kts;.github/workflows/paper-blocklocker-115-live-smoke.yml | src/main/java/co/surumene/simplecoins/paper/BlockLockerLiveEvidence.java | src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveEvidenceTest.java;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;tests/paper-blocklocker-smoke/*;build.gradle.kts;.github/workflows/paper-blocklocker-115-live-smoke.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

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
| ci | passed | [Paper 26.2 + BlockLocker 1.15 live](https://github.com/surumeneco/SimpleCoins/actions/runs/43572441989); [JUnit](https://github.com/surumeneco/SimpleCoins/actions/runs/43572441863); [Phase6 static](https://github.com/surumeneco/SimpleCoins/actions/runs/43572441855); [wallet](https://github.com/surumeneco/SimpleCoins/actions/runs/43572441864); [profile BAN](https://github.com/surumeneco/SimpleCoins/actions/runs/43572441869); [coin](https://github.com/surumeneco/SimpleCoins/actions/runs/43572441839) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Source-grounded technical scope

- The current server repository ships **BlockLocker v1.15**; 1.14.1 is a stale version for current deployment. Official upstream release `v1.15` has `blocklocker-1.15.jar` SHA-256 `8e21655ce029c2fe17b62568b2fefbe24e90b98543dfe54990777529edc0b962` (GitHub release metadata).
- The normative P/F/D rule is in 02A/02B: `[Everyone]` on effective protection means F even if a main owner is present; a non-Everyone protected container with a confirmed UUID belongs to that UUID, then BAN/playerdata determines P/D; unknown owner must not be guessed from display name. A missing protection maps to F only with trusted complete BlockLocker evidence.
- Official BlockLocker 1.15 public `BlockLockerAPIv2.getOwner(Block)` may update protections and fallback to name-based lookup when UUID is not stored. Do **not** call this API for authoritative accounting; no guessed name→UUID.
- Official `getPlugin().getProtectionFinder().findProtection(Block)` is read-only by source inspection for the lookup path; `getOwner()`, `getAllowed()` and `getProfileFactory().fromEveryone()` are typed API methods. Some types are not promised stable beyond v1.15: pin tested plugin version and refuse unknown version, missing provider or reflective/API invocation failure. Only consult on Bukkit main thread. Do not directly write protection data.
- For any protection, `getAllowed().contains(fromEveryone())` has higher classification priority than owner. If not Everyone, owner must be actual PlayerProfile with `getUniqueId().isPresent()`, else return unavailable/throw, not F and not P. Neither BAN status nor playerdata state is inferred here; this adapter supplies UUID/Everyone evidence only, to be combined later with a complete matching BAN generation.
- Scope this Work to read-only **loaded Paper Block** observation and independent CI. Saved/unloaded BlockLocker signs, long-range/topological attribution, loaded-to-saved source generation timing and full settlement integration are separate unverified dependencies. Do not publish statistics/open admission or pretend this covers all containers.

## Acceptance boundaries

1. Test `no protection → Free`, `active Everyone → Free regardless of unresolved owner`, `protected UUID → that exact UUID`, and `owner unknown or malformed → IOException rather than Free`. Do not identify a protected player by case-insensitive name.
2. Run official Paper 26.2 build 134 with pinned v1.15 BlockLocker release JAR, standalone SimpleCoinsGuard/SimpleCoins and a test-only probe; verify actual unprotected and protected sign states with main-thread API. Any mismatch must be reported as incomplete rather than inventing a success.
3. Ensure no BlockLocker API class is packaged/shaded into SimpleCoins and no test plugin is shipped. Keep admission closed throughout.
4. Product CAT Process/PI/TCE confirmation, saved+LIVE accounting completeness and live owner→BAN/playerdata final classification remain independent not-run gates.

## Verified observed evidence (2026-11-14)

- At GitHub branch HEAD `a745d6e194530c3ee07ff5cb2f6f2e56b360bedc`, the six source-head CI workflows completed successfully. The dedicated official Paper 26.2 build 134 + pinned `blocklocker-1.15.jar` SHA-256 run [43572441989](https://github.com/surumeneco/SimpleCoins/actions/runs/43572441989) observed `PAPER_BLOCKLOCKER_115_OWNER_PASSED` and `PAPER_BLOCKLOCKER_115_READ_ONLY_SUCCESS` in actual server logs. The fixture contains an unprotected chest, UUID-bearing `Name#UUID` protected chest, an `[Everyone]` protection with unresolved main name, and an owner-name-only protection rejected as unknown.
- Source-only JUnit classification, actual API lookup via verified provider release, version lock, off-thread rejection, no guessed player name-to-UUID and guarded startup have been checked independently. The main JAR does not include BlockLocker API classes or test plugin classes; no published snapshot or admission opening occurs.
- Saved/unloaded BlockLocker signs, older sign/PDC formats, multiple-sign conflicts, complete BAN evidence and saved/LIVE temporal closure remain **not-run**. A double chest spanning adjacent loaded Chunks and a back-side Everyone sign are now verified separately; this adapter is still a limited loaded-Block observation, not full O0/O1 closure.
- This is an `issue-work` Draft. Its shape may pass CAT validation, but CAT normative Process/PI/TCE, implementation conformance, oracle semantics and real player tests remain **not-run**. Do not label this Work Confirmed.

## Extended loaded double-chest source evidence (PR #402)

- Official BlockLocker 1.15 and Paper 26.2 build 134 were launched with two additional high-risk fixtures in [CI #43574631478](https://github.com/surumeneco/SimpleCoins/actions/runs/43574631478): a real 54-slot double chest across the x=15/16 Chunk boundary, with one ownership sign and matching UUID evidence on both halves; and a front `[Private]` sign whose back `[More Users]` / `[Everyone]` was recognized as Free. That run succeeded.
- New JUnit safety case at `7ecd9c2c58badbd9bcc5860bbd8ae25bea9a6db9` failed as expected because `requireLoadedPartner()` did not yet exist ([CI #43574871382](https://github.com/surumeneco/SimpleCoins/actions/runs/43574871382), compile-time Red). Production implementation at `02e12c1aa560289814c317fea51029e18fe05e84` checks the companion half's loaded Chunk by coordinates **before** any BlockLocker provider read, without a `getRelative` or chunk-load call. Exact negative east/west/north/south and negative-coordinate boundaries are checked by JUnit.
- [JUnit #43575039399](https://github.com/surumeneco/SimpleCoins/actions/runs/43575039399) and [real Paper/BlockLocker #43575039364](https://github.com/surumeneco/SimpleCoins/actions/runs/43575039364) were Green on the same implementation SHA. Actual Paper log contains `PAPER_BLOCKLOCKER_115_OWNER_PASSED ... cross-chunk double chest and back-side Everyone`, followed by `PAPER_BLOCKLOCKER_115_READ_ONLY_SUCCESS`.
- Partial positive tests of loaded structures do not imply saved BlockLocker PDC identity, ambiguous multi-sign ownership, unloaded source provenance, a coherent settlement generation, or opening the admission gate. Process/PI/TCE and product CAT conformance are **not-run**, not promoted by ordinary CI.
