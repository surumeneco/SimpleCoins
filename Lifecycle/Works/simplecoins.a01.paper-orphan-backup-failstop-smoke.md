---
cat_version: '0.2'
kind: work
id: simplecoins.a01.paper-orphan-backup-failstop-smoke
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# Work: simplecoins.a01.paper-orphan-backup-failstop-smoke

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/427 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/.github/workflows/paper-saved-discovery-live-smoke.yml |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.paper-orphan-backup-failstop-smoke | simplecoins.a01.playerdata-backup-orphan-guard |

## Technologies

| Technology |
| --- |
| GitHub Actions, official Paper 26.2 build 134, Bash, Java 25 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.paper-orphan-backup-failstop-smoke.md;.github/workflows/paper-saved-discovery-live-smoke.yml | — | .github/workflows/paper-saved-discovery-live-smoke.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| paper-orphan-backup-ci | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.WorldStorageDiscoveryTest | orphan-backup;source-closure | WorldStorageDiscoveryTest;paper-saved-discovery-live-smoke.yml | JUnit XML;GitHub Actions logs | 480 |

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

- Paper 26.2 on an empty/isolated server starts with independent SimpleCoinsGuard in default-deny mode, opens journals, and observes saved source directory closure. This existing test is Green but does not prove the new `UUID.dat_old` orphan failure is wired through real startup to `Bukkit.shutdown`.
- Add a second official Paper 26.2 CI startup after the clean stop: inject one canonical orphan `world/playerdata/<UUID>.dat_old` without a matching primary; do not create money, persist grant or change playerdata. Require `SIMPLECOINS_SAVED_DISCOVERY_FAILED`, the exact orphan-backup diagnostic, independent admission guard still active, and self-stop **without console-initiated stop**. Reject timeout and any `SIMPLECOINS_SAVED_DISCOVERY_OBSERVED` success marker.
- Existing first boot remains clean, no world/accounting source fixture from the test may leak into deployment, and CI leaves the production admission default-deny.
- This is an integration/verification Work; not an additional production behavior, CAT Confirmed spec, or proof of full G0/global time coherence/G3. Formal normative Process/PI/TCE Gates remain not-run.
