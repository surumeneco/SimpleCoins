---
cat_version: '0.2'
kind: work
id: simplecoins.wcl-open-cargo-accounting
status: draft
process: simplecoins.accounting.wcl-cargo-source
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
decision_ref: 'user-chat:2026-11-24:continue-and-complete-PR681;approved-02B:通常時の仕訳・差分反映'
refs:
  - simplecoins.accounting.wcl-cargo-source
  - simplecoins.accounting.wcl-cargo-source.pi.capture
  - simplecoins.accounting.wcl-cargo-source.tce.capture
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view
spec_status: confirmed
---

# Work: simplecoins.wcl-open-cargo-accounting

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/681 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/45417917327 |
| https://github.com/surumeneco/SimpleCoins/actions/runs/45417917933 |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.wcl-cargo-source | simplecoins.accounting.wcl-cargo-source | simplecoins.accounting.memory-journal;simplecoins.external-storage-provider-integration |

## Technologies

| Technology |
| --- |
| Java |
| JUnit |
| Paper API |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.wcl-open-cargo-accounting.md;docs/cat/confirmed/wcl-cargo-source-*.md;src/main/java/co/surumene/simplecoins/paper/PaperEntityAccounting.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperGroundItemAccounting.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml;tests/cat/wcl-cargo-source-junit-binding.json;src/test/java/co/surumene/simplecoins/paper/WclCargoSourceDriver.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedWclCargoSourceTest.java | src/main/java/co/surumene/simplecoins/paper/PaperEntityAccounting.java;src/main/java/co/surumene/simplecoins/paper/PaperWonderfulLiveCandidates.java;src/main/java/co/surumene/simplecoins/paper/PaperGroundItemAccounting.java | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml;tests/cat/wcl-cargo-source-junit-binding.json;src/test/java/co/surumene/simplecoins/paper/WclCargoSourceDriver.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedWclCargoSourceTest.java |

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

## Scope and acceptance

- Existing approved 02A/02B ordinary journal requirements, Confirmed memory-accounting and A05 WCL consumer artifacts remain normative. This Work corrects the external observation boundary; no provider release or new ownership/accounting policy.
- WWC 2.1.1: 36-slot GUI has cargo raw slots 9–35, backed by one serialized shulker; 9-slot GUI has no cargo. For an open cargo inventory, replace only the shulker contents with actual GUI cargo prior to valuing it, retain its identity and the other provider slots. A shulker swap first saves old contents and intentionally skips obsolete close synchronization. Do not reapply that obsolete GUI on close/reopen.
- WWW 1.1.2: cargo index = raw slot − 9. Only the usable capacity, not all visible row cells, is writable; hidden stored slots must survive. Preserve equipment and the existing native alias rule. Do not treat a disabled decorative cell as an empty writable cargo slot or remove hidden items.
- Observe the real player and entity before ordinary interaction; keep the existing tick-local combined journal. Use the actual verified holder instance at accepted open and close; transition back to provider storage after persistence without counting the same cargo again. No persistent per-holder balance cache; no full-world rescan. Reuse existing owner resolution and valuation.
- Existing #681 probe already detects missing openCargo/closeCargo (compiler failure, not a canonical current-item Red). Extend the real Paper acceptance to changed and emptied cargo, close after actual persistence, disabled/hidden WWW slots and WWC shulker swap, while preserving the existing provider and native-mirror regressions. A synthetic holder alone does not prove the real player's GUI lifecycle.
- Source investigation: WWC main 05508cc263a02d8288e81a4fa82d76bbae64c886 InventoryService; WWW main 92683837f6e61db2c9c3a506a1a63855f2e7eddb WonderfulWolfInventoryService/Policy. Runtime compatibility is still to be checked against the pinned distributed JARs in CI.
- The confirmed source-selection contract is narrow, not a claim of physical observation or full admission conformance. The source-selection closure and model conformance do not establish runtime observation or complete implementation conformance. Do not reopen A01 global writer closure or redo startup settlement for this work.

## Compilation

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/wcl-cargo-source-process.md | @repo:SimpleCoins/docs/cat/confirmed/wcl-cargo-source-pi.md | @repo:SimpleCoins/docs/cat/confirmed/wcl-cargo-source-tce.md | — | — | — | @work/.cat-flow/cache/wcl-cargo-source-model.json | @work/.cat-flow/cache/wcl-cargo-source-vectors.json | @work/.cat-flow/cache/wcl-cargo-source-queue.json | @repo:SimpleCoins/tests/cat/wcl-cargo-source-junit-binding.json | @repo:SimpleCoins/src/test/java/co/surumene/simplecoins/domain/generated/GeneratedWclCargoSourceTest.java | — | — | false |
