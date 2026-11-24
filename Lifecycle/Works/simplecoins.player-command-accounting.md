---
cat_version: '0.2'
kind: work
id: simplecoins.player-command-accounting
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

# Work: simplecoins.player-command-accounting

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
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.player-command-accounting.md;src/main/java/co/surumene/simplecoins/paper/PaperGroundItemAccounting.java;tests/paper-wcl-services-probe/product-startup-acceptance.sh | src/main/java/co/surumene/simplecoins/paper/PaperGroundItemAccounting.java | tests/paper-wcl-services-probe/product-startup-acceptance.sh |

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
| @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-process.md | @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-pi.md | @repo:SimpleCoins/docs/cat/confirmed/memory-accounting-tce.md | — | — | — | @work/.cat-flow/cache/command-model.json | @work/.cat-flow/cache/command-vectors.json | @work/.cat-flow/cache/command-queue.json | @repo:SimpleCoins/tests/cat/memory-accounting-junit-binding.json | @work/.cat-flow/generated/GeneratedCommandAccountingTest.java | @work/.cat-flow/cache/command-obligations.json | — | false |

## Scope and acceptance

Implement the existing 02B explicit requirement that command deletion changes the world amount, using the existing confirmed memory journal. Before vanilla clear (direct player/console command or nested execute), observe the affected player inventory preimages, then use actual end-of-tick values via the existing shared local transaction. No amount is inferred from the command string. Test-only max-count zero, no matching items, failed command and cancellation must not change currency balances. Physical source selection, P/D attribution, already recorded grants and de-duplication remain unchanged. Nested or non-resolvable selectors may conservatively capture online players only; no offline data or world-wide rescan. Verify actual product clear after issued value and cold settlement against preserved physical fixture.
