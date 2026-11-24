---
cat_version: '0.2'
kind: work
id: simplecoins.memory-hopper-accounting
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

# Work: simplecoins.memory-hopper-accounting

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
| SimpleCoins | ../.. | develop | feature/* | src/main/java/co/surumene/simplecoins/domain/InventoryAccountingDelta.java;src/main/java/co/surumene/simplecoins/paper/PaperHopperAccounting.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/InventoryAccountingDeltaTest.java;Lifecycle/Works/simplecoins.memory-hopper-accounting.md;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;.github/workflows/paper-blocklocker-115-live-smoke.yml;build.gradle.kts | src/main/java/co/surumene/simplecoins/domain/InventoryAccountingDelta.java;src/main/java/co/surumene/simplecoins/paper/PaperHopperAccounting.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/InventoryAccountingDeltaTest.java;src/test/java/co/surumene/simplecoins/paper/BlockLockerLiveProbe.java;.github/workflows/paper-blocklocker-115-live-smoke.yml;build.gradle.kts |

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
Capture physical block inventories before Paper 26.2 hopper search completes, and compare actual slot values after the tick. Do not debit a proposed move before it is confirmed. Preserve per-slot Dead locations, deduplicate double-chest halves, and reject a missing preimage for a monetary transfer. The remaining hopper-minecart path uses the same approved 02B physical before/after contract: source from Paper's search before the source debit, actual receiving entity inventory before transfer, and combined block/entity movements. Real protected chest to hopper minecart transfer must reclassify 10000C from Dead to Free; cancellation must leave both sides and sequence unchanged. No new ownership/accounting decision. No claim of arbitrary direct plugin mutation.
