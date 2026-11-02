# Phase 7 — GrantLedger pure-domain extraction (CAT Candidate)

Status: **Candidate / not Confirmed**. Scope: TASK-20261101-006. This is a code-to-spec and integration-boundary artifact; it does not approve new behavior or a production release.

## Observed source and test

- Source: PR #2 fixed commit `01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153`, `GrantLedger.java` blob `df93d2c042cf83601a6c9d7492601579a3bfe4fc`.
- Existing regression suite: `GrantLedgerTest.java` blob `adfa143c7bb220107a6ac6dfb659e2ffc890e90a`. Its tests exercise reservation/replay, typed denomination, post-save acknowledgement, recipient receipts, corruption/missing records, legacy schema and symlinked record rejection.
- Existing domain dependencies already in `develop`: `DurableFile.java` blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8`, `CoinDenomination.java` blob `f6d8a15fab50c242886f00d35e902289342ea2ef`.
- Related semantic Candidates remain separate: PR #111 (`phase7-offline-grant-interleaving-replay.md`) and PR #124 (`phase7-grant-interleaved-receipt-observation.md`). Do not duplicate their tests or promote them to Confirmed here.

## Candidate extraction contract

Copy the two existing source/test files **byte-identically** into a fresh `develop`-based branch; do not alter public APIs or runtime code. The extracted domain journal is not a Paper plugin and cannot establish offline inventory delivery, playerdata durability, JE/BE/Nexo distribution, or admin command permissions.

## Verification and gates

Compare both resulting Git blob SHAs to the fixed originals. Run the repository's actual Gradle/JUnit test suite where available; distinguish direct tests from CI. Preserve Draft until the applicable normative CAT flow/Gates and independent integration safety are verified. A passing JUnit suite is not CAT Confirmed or Paper acceptance.

## Open decisions

- Whether this code-to-spec Candidate and the related interleaving Candidates may be promoted to Confirmed under the current CAT-TDD contract.
- How journal replay interacts with live Paper inventory/playerdata and grant receipt persistence under crashes.
- How the Python read-only journal verifier's Unicode whitespace predicate should match the actual Java writer (PR #117 expected Red).
