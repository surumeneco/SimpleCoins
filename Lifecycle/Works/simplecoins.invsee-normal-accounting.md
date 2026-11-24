---
cat_version: '0.2'
kind: work
id: simplecoins.invsee-normal-accounting
status: draft
process: simplecoins.accounting.invsee-source
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# Work: simplecoins.invsee-normal-accounting

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.invsee-source | simplecoins.accounting.invsee-source | — |

## Technologies

| Technology |
| --- |
| Java; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | src/main/java/co/surumene/simplecoins/domain/InvseeSourceSelection.java;src/main/java/co/surumene/simplecoins/paper/PaperInvseeViews.java;src/main/java/co/surumene/simplecoins/paper/PaperGroundItemAccounting.java;src/main/java/co/surumene/simplecoins/paper/PaperOwnerAssetReader.java;src/main/java/co/surumene/simplecoins/paper/PaperOwnerStateReconciliation.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/resources/plugin.yml;src/test/java/co/surumene/simplecoins/domain/InvseeSourceDriver.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedInvseeSourceTest.java;tests/paper-protected-settlement-runtime/src/main/java/co/surumene/simplecoins/a01probe/RuntimeSettlementProbe.java;docs/cat/confirmed/invsee-source-*.md;Lifecycle/Works/simplecoins.invsee-normal-accounting.md;tests/cat/invsee-source-junit-binding.json;.github/workflows/paper-protected-settlement-runtime.yml | src/main/java/co/surumene/simplecoins/domain/InvseeSourceSelection.java;src/main/java/co/surumene/simplecoins/paper/PaperInvseeViews.java;src/main/java/co/surumene/simplecoins/paper/PaperGroundItemAccounting.java;src/main/java/co/surumene/simplecoins/paper/PaperOwnerAssetReader.java;src/main/java/co/surumene/simplecoins/paper/PaperOwnerStateReconciliation.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/resources/plugin.yml | src/test/java/co/surumene/simplecoins/domain/InvseeSourceDriver.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedInvseeSourceTest.java;tests/paper-protected-settlement-runtime/src/main/java/co/surumene/simplecoins/a01probe/RuntimeSettlementProbe.java |

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
| @repo:SimpleCoins/docs/cat/confirmed/invsee-source-process.md | @repo:SimpleCoins/docs/cat/confirmed/invsee-source-pi.md | @repo:SimpleCoins/docs/cat/confirmed/invsee-source-tce.md | — | — | — | @work/.cat-flow/cache/invsee-source-model.json | @work/.cat-flow/cache/invsee-source-vectors.json | @work/.cat-flow/cache/invsee-source-queue.json | @repo:SimpleCoins/tests/cat/invsee-source-junit-binding.json | @repo:SimpleCoins/src/test/java/co/surumene/simplecoins/domain/generated/GeneratedInvseeSourceTest.java | @work/.cat-flow/cache/invsee-source-obligations.json | — | false |


## Scope and acceptance

Existing 02A/02B ownership and ordinary confirmed journal semantics apply. InvSee++ v0.31.22 is the installed provider. Use target UUID and one authoritative main/ender partition; online mirrors are not extra assets. Track accepted open editor references only, switch closed views to saved readback, observe direct pre-save event, reconcile changed owner using the same source. No native writer fence or persisted per-holder cache. Verify real save with monetary delta, cancellation, before/after open/close transition and owner classification. Source-selection conformance alone does not prove external observations or all normal accounting.
