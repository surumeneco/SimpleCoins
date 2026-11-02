---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.grant-ledger-overflow-replay
status: draft
process: simplecoins.offline-grant-ledger
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
  - https://github.com/surumeneco/SimpleCoins/pull/125
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: checked-arithmetic overflow on durable grant replay (CAT Draft Red)

## Observation

On PR #125 pinned HEAD `ca0e09032e071904c31ed46dca1f382511b00d4a`, `GrantLedger.java` blob `df93d2c042cf83601a6c9d7492601579a3bfe4fc`, `Grant` validates typed coin amount with `Math.multiplyExact(coinCount, denomination.value())`. `decode` converts `IllegalArgumentException` to `IOException` but does not convert `ArithmeticException` (a distinct `RuntimeException`). A checksummed but semantically invalid SCG2 record may therefore leak an unchecked arithmetic exception during replay.

This is a candidate error-taxonomy boundary for an offline grant journal. It does not authorize a production fix, change the denomination rules, or claim a Confirmed CAT contract.

## Targets

| Ref | Aspect |
| --- | --- |
| `GrantLedger.Grant` | Typed denomination arithmetic validation |
| `GrantLedger.decode` | Durable record replay error classification |
| `GrantLedgerOverflowReplayTest` | Checksummed SCG2 replay controls and expected Red |

## Candidate vectors

| ID | Checksummed SCG2 record | Expected candidate outcome |
| --- | --- | --- |
| GRANT-OVERFLOW-01 | denomination 500, coinCount 1, amount 500 | Replay succeeds and pending grant is preserved (Green control) |
| GRANT-OVERFLOW-02 | denomination 500, coinCount 1, amount 501 | Replay fails with `IOException` (existing Green control) |
| GRANT-OVERFLOW-03 | denomination 500, coinCount `Long.MAX_VALUE`, amount 500 | Replay fails with `IOException`, not an unchecked `ArithmeticException` (expected Red) |

The test writes actual checksummed journal bytes through `DurableFile.write`, plus a coherent `head`, and reconstructs `GrantLedger`. It does not mutate the production source or an actual playerdata file. This is distinct from PR #126's directory symlink escape and other receipt sequencing Drafts.

## Status and gates

Draft code-to-spec Candidate. Required normative Process/PI/TCE decision, CAT script Gates, Green, Gradle/JUnit and Paper live/offline give/restart acceptance are separate. Do not merge a known-Red Draft into `develop`.
