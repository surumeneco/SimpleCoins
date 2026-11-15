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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43735175572 (exact source CI, official Paper restart/readback only; formal CAT Gates not-run) |
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

## Verified Paper evidence

- [Official Paper 26.2 build 134, CI #43735175572](https://github.com/surumeneco/SimpleCoins/actions/runs/43735175572): authenticated CAT Work structural validator returned `contract_valid:true`, `semantic_approval_verified:false`; both JUnit/Gradle and Paper smoke completed successfully on head `db6644554c1c1e65127edde7a53811bbc957cb10`.
- Distinct Paper process **A** created a valid Bukkit-indexed Chicken in a preloaded disposable Chunk 4,4, attached the real SimpleCoins coin-valued ShulkerBox bytes in WWC-compatible PDC, wrote a saved UUID witness and invoked world save. The workflow confirmed a nonempty real entities `r.0.0.mca` Anvil region.
- Distinct Paper process **B** loaded the **same** disk-backed world, located exactly one matching scoreboard-tagged Chicken in the known test chunk, and verified the original exact Chicken UUID and world UUID, WWC `readLoaded` and `readStored` responses, `follow_target`, one serialized shulker and genuine 5000-unit coin ×2. Source log markers: `PAPER_A05_WWC_ENTITY_PERSISTED_BEFORE_RESTART_PASSED`, `PAPER_A05_WWC_REAL_ENTITY_RELOAD_COIN_SHULKER_PASSED` and `PAPER_A05_REAL_SAVED_ENTITY_CURRENCY_CROSS_RESTART_PASSED`.
- No synthetic new Chicken or currency is inserted on process B. The prior two-provider registration, WWW v3 mock saved-item fixture, and unsupported-schema rejection also passed on all three process boots.
- This confirms Paper Entity **save/restart/reload for a manually WWC-typed PDC test fixture**. It does not claim that a naturally generated genome-initialized WWC entity or loaded/unloaded mixed source generation has been verified, or that SimpleCoins's settlement publisher is safe to enable.

## Authority

`issue-work` verification only. No SimpleCoins production code, real user data, actual VPS, confirmed normative CAT artifacts or formal Gates changed. CI Green is strictly limited to its checked test cases.
