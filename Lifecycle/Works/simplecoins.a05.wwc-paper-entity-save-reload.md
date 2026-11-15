---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wwc-paper-entity-save-reload
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A05 — 実PaperのEntity保存・再起動とWWC内貨幣の一致検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/440 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/MineCraftXPlayServer/tree/develop/plugins |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wwc-paper-entity-save-reload | simplecoins.a05.paper-stored-currency-providers;simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |
| Java 25; official Paper 26.2 build 134; WGL/WCL/WWC/WWW pinned distribution JARs; real Entity region save |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wwc-paper-entity-save-reload.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

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

## Scoped acceptance

- Reuse the pinned four-server-JAR environment, independent default-deny guard and two actual Bukkit ServicesManager providers from the prior A05 Work.
- On real Paper server process A, explicitly load a disposable test chunk, spawn a **vanilla Chicken** as a fixture on Paper main thread, place genuine SimpleCoins 5000-unit coin ×2 in a serialized Paper ShulkerBox PDC at the existing WWC storage key; include a valid follow-target UUID and unique non-monetary scoreboard tag for independent rediscovery. Require valid/indexed Entity before save.
- Save the actual world, stop Paper cleanly and verify the physical entities/region file exists. The CI fixture must not remove the test Chicken, move chunks or create a new coin on the next process.
- On independent real Paper process B using the **same saved world**, identify exactly one tagged Chicken from the explicitly loaded test chunk, re-read the WWC ItemStack PDC through the shipped `WonderfulStorageReader` `readLoaded` and `readStored` APIs, and assert the nested physical currency PDC/model, quantity, follow target, entity UUID and world UUID. An absent, duplicated or malformed saved fixture fails.
- Distinguish readback from the reader's PDC conversion and from the full game lifecycle; an artificial PDC marker is not equivalent to a genuinely spawned and genome-initialized WWC individual.
- Keep all independent no-entity, WWW fixture and corrupt-schema regressions green. Record any provider lifecycle rewrite/erase and diagnose whether the test fixture violates the official WWC generation contract before interpreting it as a product bug.
- This proves only a particular real Paper entity persistence/reload path. It does not prove unloaded entity region discovery without chunk load, owner/BAN classification, all source completeness, G0 atomic generation or permitted player admission.

## Authority

`issue-work` verification only. No SimpleCoins production code, real user data, actual VPS, confirmed normative CAT artifacts or formal Gates changed. CI Green is strictly limited to its checked test cases.
