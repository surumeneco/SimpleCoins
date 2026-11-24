---
cat_version: '0.2'
kind: work
id: simplecoins.a01.product-startup-settlement
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#起動時／停止時決算（全件正本照合）
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

# Work: simplecoins.a01.product-startup-settlement

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.settlement.writer-epoch-publication | — |

## Technologies

| Technology |
| --- |
| Java; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.product-startup-settlement.md;src/main/java/co/surumene/simplecoins/paper/PaperStartupSettlementCoordinator.java;src/main/java/co/surumene/simplecoins/paper/PaperStartupSettlementSource.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperProtectedSettlementService.java;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/domain/BanSourceCoverage.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/resources/config.yml;src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeChecks.java;.github/workflows/paper-wcl-services-probe.yml | src/main/java/co/surumene/simplecoins/paper/PaperStartupSettlementCoordinator.java;src/main/java/co/surumene/simplecoins/paper/PaperStartupSettlementSource.java;src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java;src/main/java/co/surumene/simplecoins/paper/PaperProtectedSettlementService.java;src/main/java/co/surumene/simplecoins/domain/SettlementRuntime.java;src/main/java/co/surumene/simplecoins/domain/BanSourceCoverage.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/resources/config.yml | src/test/java/co/surumene/simplecoins/domain/SettlementRuntimeChecks.java;.github/workflows/paper-wcl-services-probe.yml |

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
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/startup-model.json | @work/.cat-flow/cache/startup-vectors.json | @work/.cat-flow/cache/startup-queue.json | @repo:SimpleCoins/tests/cat/a01-writer-epoch-junit-binding.json | @work/.cat-flow/generated/GeneratedA01StartupTest.java | @work/.cat-flow/cache/startup-obligations.json | — | false |

## Scope and acceptance

Actual product lifecycle: after ServerLoadEvent and zero online players, freeze tick simulation, retain each World's previous autosave setting, disable autosave and wait 40 ticks for already queued saves. Discover all registered World and playerdata sources asynchronously and verify receipts. Capture real BlockLocker/WCL/standard BAN and LIVE values on primary; worker stages SAVED data and prepares an unpublished complete memory snapshot. Recheck actual loaded source IDs, values, attribution and provider settings/BAN before final handoff, then restore original tick/autosave settings. No player admitted until actual normal accounting is complete. Incomplete normal tracking is a known development hold, not a fabricated readiness flag. Real production JAR acceptance on copied known-value world, with real providers and no probe plugin mutating that world. Existing isolated mutation fixtures may explicitly use diagnostic-only mode, which never opens admission. No release or arbitrary new writer fence.
