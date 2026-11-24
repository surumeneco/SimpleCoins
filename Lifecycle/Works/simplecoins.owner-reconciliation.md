---
cat_version: '0.2'
kind: work
id: simplecoins.owner-reconciliation
status: draft
process: simplecoins.accounting.owner-reconciliation
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/5
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# Work: simplecoins.owner-reconciliation

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.owner-reconciliation | simplecoins.accounting.owner-reconciliation | — |

## Technologies

| Technology |
| --- |
| Java; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | src/main/java/co/surumene/simplecoins/domain/MemoryAccounting.java;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/paper/PaperProtectedSettlementService.java;src/test/java/co/surumene/simplecoins/domain/OwnerReconciliationDriver.java;src/test/java/co/surumene/simplecoins/domain/OwnerReconciliationChecks.java;src/test/java/co/surumene/simplecoins/domain/OwnerReconciliationTest.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeChecks.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeTest.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedOwnerReconciliationTest.java;tests/cat/owner-reconciliation-junit-binding.json;docs/cat/confirmed/owner-reconciliation-*.md;Lifecycle/Works/simplecoins.owner-reconciliation.md | src/main/java/co/surumene/simplecoins/domain/MemoryAccounting.java;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/paper/PaperProtectedSettlementService.java | src/test/java/co/surumene/simplecoins/domain/OwnerReconciliationDriver.java;src/test/java/co/surumene/simplecoins/domain/OwnerReconciliationChecks.java;src/test/java/co/surumene/simplecoins/domain/OwnerReconciliationTest.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeChecks.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeTest.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedOwnerReconciliationTest.java |

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
| @repo:SimpleCoins/docs/cat/confirmed/owner-reconciliation-process.md | @repo:SimpleCoins/docs/cat/confirmed/owner-reconciliation-pi.md | @repo:SimpleCoins/docs/cat/confirmed/owner-reconciliation-tce.md | — | — | — | @work/.cat-flow/cache/owner-model.json | @work/.cat-flow/cache/owner-vectors.json | @work/.cat-flow/cache/owner-queue.json | @repo:SimpleCoins/tests/cat/owner-reconciliation-junit-binding.json | @repo:SimpleCoins/src/test/java/co/surumene/simplecoins/domain/generated/GeneratedOwnerReconciliationTest.java | @work/.cat-flow/cache/owner-obligations.json | — | false |

## Scope and acceptance

Complete observed owner replacement only, including preservation of unrelated F and owners, missing playerdata and zero registration. No assertion of targeted observation, live freshness or admission coverage from this local domain operation.
