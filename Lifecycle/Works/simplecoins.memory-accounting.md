---
cat_version: '0.2'
kind: work
id: simplecoins.memory-accounting
status: draft
process: simplecoins.accounting.memory-journal
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

# Work: simplecoins.memory-accounting

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.memory-journal | simplecoins.accounting.memory-journal | — |

## Technologies

| Technology |
| --- |
| Java; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | src/main/java/co/surumene/simplecoins/domain/MemoryAccounting.java;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/paper/PaperProtectedSettlementService.java;src/test/java/co/surumene/simplecoins/domain/MemoryAccountingDriver.java;src/test/java/co/surumene/simplecoins/domain/MemoryAccountingChecks.java;src/test/java/co/surumene/simplecoins/domain/MemoryAccountingTest.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeChecks.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeTest.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedMemoryAccountingTest.java;tests/cat/memory-accounting-junit-binding.json;Lifecycle/Works/simplecoins.memory-accounting.md | src/main/java/co/surumene/simplecoins/domain/MemoryAccounting.java;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/paper/PaperProtectedSettlementService.java | src/test/java/co/surumene/simplecoins/domain/MemoryAccountingDriver.java;src/test/java/co/surumene/simplecoins/domain/MemoryAccountingChecks.java;src/test/java/co/surumene/simplecoins/domain/MemoryAccountingTest.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeChecks.java;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeTest.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedMemoryAccountingTest.java |

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
| @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-process.md | @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-pi.md | @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-tce.md | — | — | — | @work/.cat-flow/cache/memory-model.json | @work/.cat-flow/cache/memory-vectors.json | @work/.cat-flow/cache/memory-queue.json | @repo:SimpleCoins/tests/cat/memory-accounting-junit-binding.json | @work/.cat-flow/generated/GeneratedMemoryAccountingTest.java | @work/.cat-flow/cache/memory-obligations.json | — | false |

## Scope and acceptance

Use the revised A01 publication decision, complete source traversal, and an in-memory journal rather than a fenced durable WAL. The source-stage owner still must observe known concurrent world writers and normal tracking before admission can open. Keep the previous protected path isolated for historical fixture tests. No assertion of startup or physical event coverage follows from this local cache implementation.
