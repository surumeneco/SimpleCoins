---
cat_version: '0.2'
kind: work
id: simplecoins.o2.paper-profile-observation
status: candidate
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

# Work: simplecoins.o2.paper-profile-observation

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/69d6a0b2c40ac52257d7950cbc998faa03909d8e/src/main/java/co/surumene/simplecoins/paper/PaperProfileBanSource.java |
| https://github.com/surumeneco/SimpleCoins/blob/69d6a0b2c40ac52257d7950cbc998faa03909d8e/src/test/java/co/surumene/simplecoins/paper/PaperProfileBanSourceTest.java |
| https://github.com/surumeneco/SimpleCoins/pull/395 |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.ownership-evidence | simplecoins.o2.paper-profile-observation | simplecoins.ban-source-coverage |

## Technologies

| Technology |
| --- |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;build.gradle.kts;src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java;.github/workflows/paper-profile-ban-live-smoke.yml | — | src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java |

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
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Work scope and checked source state

- Target: Paper 26.2, SimpleCoins 1.0.0, Issue #4 / O2a **verification only**, not a new runtime release or normative decision. Permitted test-only build/probe/workflow files are scoped in Repositories; no production Java sources or plugin entrypoint changes.
- Inspected `develop@69d6a0b2c40ac52257d7950cbc998faa03909d8e`. PR #395 is already merged; this Work must not recreate its profile adapter.
- `PaperProfileBanSource.observe(UUID)` requires the primary thread, checks returned UUID, reads `OfflinePlayer.isBanned()`, and fails on missing/mismatched/unreliable responses. Existing test class exercises true, false, null provider, UUID mismatch, missing arguments through a proxy, **not live Bukkit**.
- `paper-profile: CLEAR` is **not** proof of `paper-ip: CLEAR` or any external provider; `BanSourceCoverage.capture` requires each independently declared source per UUID. This Work does not establish the completeness of the required provider list or a time-frozen cross-provider generation.

## Verification acceptance and evidence still required

1. On isolated Paper 26.2 build 134 with the actual SimpleCoins/guard plugin versions, invoke the profile observer on Paper's main thread for one banned and one unbanned profile with known UUIDs; record resulting source ID, BANNED/CLEAR and execution log. This must be a read-only probe; no admission opening. A dedicated CI isolation probe (test-only Java class/JAR + workflow) may satisfy this after its actual Paper execution and logs are independently checked. **Not run yet.**
2. Exercise non-main-thread, missing/mismatched UUID and unreadable provider cases; verify fail-closed errors without interpreting them as CLEAR. Existing pure proxy tests cover part; real Paper invocation is **not run**.
3. Audit the active server's standard IP BAN and every external BAN provider plus UUID-to-IP subject identification. If any provider or identity mapping cannot be proven complete, mark full `BanSourceCoverage` **blocked**; do not create placeholder CLEAR observations. **Unverified.**
4. Only after 1–3 and the distinct owner/BlockLocker/WWC/WWW prerequisites are established may Issue #3 use one matching saved+LIVE generation. This Work must not publish monetary statistics or allow logins. **Not ready.**

## Stage and handoff

`issue-work / verification` is the current scope. There is no approved CAT Confirmed Process/PI/TCE decision reference in this Work; product CAT process-closure/model/implementation Gates remain `not-run`. A future implementation Work must take actual normative confirmation and standalone Red/Green evidence before any production edit.
