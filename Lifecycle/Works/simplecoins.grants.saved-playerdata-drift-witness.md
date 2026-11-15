---
cat_version: '0.2'
kind: work
id: simplecoins.grants.saved-playerdata-drift-witness
status: draft
process: simplecoins.grants.saved-receipt-audit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.grants.saved-playerdata-drift-witness

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.saved-receipt-audit | simplecoins.grants.saved-playerdata-drift-witness | simplecoins.grants.receipt-audit;simplecoins.grants.content-witness |

## Technologies

| Technology |
| --- |
| Java 25, JUnit 5, SHA-256, playerdata saved file |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.saved-playerdata-drift-witness.md;src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java;src/main/java/co/surumene/simplecoins/paper/SavedPlayerdataContentWitness.java;src/test/java/co/surumene/simplecoins/paper/SavedPlayerdataDriftAuditTest.java | src/main/java/co/surumene/simplecoins/paper/SavedGrantReceiptUniverseAudit.java;src/main/java/co/surumene/simplecoins/paper/SavedPlayerdataContentWitness.java | src/test/java/co/surumene/simplecoins/paper/SavedPlayerdataDriftAuditTest.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| playerdata-witness | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.paper.SavedPlayerdataDriftAuditTest | same-size-replacement;after-player-read-drift;unchanged-success;unverified-manifest | SavedGrantReceiptUniverseAudit;WorldStorageDiscovery;PlayerDataReader | JUnit XML | 180 |

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

- Existing saved receipt audit checks the discovered file **set** before/after reading saved GZIP playerdata and checks the issuance journal history, but not whether already-read playerdata bytes changed while the audit was in progress. Source-set equality alone cannot rule out a same-path rollback/mutation.
- Adopted G3 and 02B require fail-closed receipt/physical consistency across authoritative saved sources, including crash recovery and playerdata rollback. Reuse existing WorldSnapshotTraversal content witness rationale without pretending it freezes the world.
- Read-only ContentWitness captures bounded SHA-256 fingerprints of each discovered actual playerdata file (including physical identity, size, timestamps), rejects symlinks/invalid physical files and rechecks all captured bytes; a same-size or mtime-repaired rewrite must be detected.
- Wire witness into SavedGrantReceiptUniverseAudit: capture with the same discovery closure before reading, validate again at end and fail with IOException on drift. Test deterministic mutation **after** the saved player's receipt was read (test-only observation seam); show the preexisting audit erroneously returns success for a changed file, then correct it.
- Do not change monetary policy, acknowledge grants, open admission, publish stats, issue coins, or claim a true frozen-world/atomic playerdata-save proof. CAT Work Draft verified syntactically via fixed canonical runtime, official Process/PI/TCE Confirmed and conformance Gates not-run.
