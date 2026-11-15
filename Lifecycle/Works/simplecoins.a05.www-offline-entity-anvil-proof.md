---
cat_version: '0.2'
kind: work
id: simplecoins.a05.www-offline-entity-anvil-proof
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

# A05 — WWW型Entityの実Paper保存・オフラインMCA・再起動照合

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/442 |
| https://github.com/surumeneco/SimpleCoins/pull/440 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/WhatAWonderfulWolf/blob/develop/src/main/java/co/surumene/www/persistence/WonderfulWolfStorageReader.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.www-offline-entity-anvil-proof | simplecoins.a05.wwc-offline-entity-anvil-proof;simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |
| Java 25; official Paper 26.2 build 134; pinned WGL/WCL/WWC/WWW distributables; AnvilRegionReader and NbtReader |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.www-offline-entity-anvil-proof.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/OfflineEntityRegionProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/OfflineEntityRegionProbe.java;.github/workflows/paper-wcl-services-probe.yml |

## Checks

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43740096615 (isolated Paper two-JVM, genuine Entity MCA; CAT formal Gates not-run) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Bounded verification

1. Reuse the exact pinned WWC/WWW/WCL/WGL distributable JARs, isolated Paper 26.2, default-deny admission and authentic SimpleCoins PDC/model money from prior A05 CI.
2. On Paper process A create a disposable **vanilla Wolf** fixture in already loaded Chunk 4,4 with `wonderful_wolf` PDC marker, schema v1 and the production WWW codec's documented v3 binary runtime structure. Encode **different owner and commander UUIDs** plus one genuine 10000-unit money weapon and five 1000-unit coins in cargo slot 7. Validate the packaged WWW `readStored` result before saving. Record immutable identity/runtime bytes; Paper itself must write its Entity Anvil.
3. After Paper exits, use the existing standalone **non-Bukkit** offline Anvil scanner on that very same saved `entities/r.0.0.mca` to find the original tagged Wolf and compare physical Entity UUID, type, source chunk and original `BukkitValues` namespace/schema/v3 runtime bytes. Require exactly one tagged Wolf distinct from the WWC Chicken, preserving each as a separate authority.
4. Reboot Paper as process B using the same world; find the same single Wolf via real Paper Chunk (no replacement fixture), compare the stored PDC via the distributed WWW reader and verify owner/commander and physical currency values. Since this manually typed test Wolf is **not** a genuine WWW managed individual with genome, it need not appear in WWW's `readLoaded` registry. Do not treat that NotFound as proof of a missing saved source.
5. Keep all existing WWC/WWW registration, empty, corrupt-schema, WWC nested LIVE/Stored, real persistence/reload, offline Anvil and original product Gradle regressions green. Reject altered or absent PDC before reporting success.

## Observed real-Paper result

- [Paper 26.2 build 134, CI #43740096615](https://github.com/surumeneco/SimpleCoins/actions/runs/43740096615): exact source HEAD `5c1e8555ed8f55cd98300431a223ecfe17d23b6e`, current pinned Work validator `contract_valid:true` and `semantic_approval_verified:false`, Gradle test and Paper integration all passed. Exact blob-pinned WGL 1.0.1, WCL 0.1.0, WWC 2.1.0 and WWW 1.1.1 distributables were checked first.
- Paper process A created a new indexed Wolf in the known disposable Chunk 4,4. Its WWW v1 typed PDC carried a v3 runtime with explicit different owner and commander UUIDs and genuine SimpleCoins 10000-unit weapon and five 1000-unit cargo coins. The shipped `readStored` API decoded those physical items and relation IDs before save. Paper then wrote the Entity Anvil.
- Between Paper processes, **no server boot and no chunk load**: a standalone Java program using the production `AnvilRegionReader.scan` read `entities/r.0.0.mca`, validated the unique tagged Wolf's saved UUID/type, `BukkitValues` WWW marker/schema, **507 exact runtime bytes** against first-JVM evidence and its distinct identity from the WWC Chicken. Logs include `PAPER_A05_OFFLINE_MCA_WWW_MONEY_OWNER_PHYSICAL_SOURCE_PASSED` and `PAPER_A05_OFFLINE_MCA_TWO_PROVIDERS_DISTINCT_SOURCES_PASSED`.
- Paper process B reloaded that same world and the original Wolf UUID from Entity MCA. The shipped WWW `readStored` API recovered both coins and owner/commander, while the original WWC Chicken's money remained preserved. `PAPER_A05_WWW_REAL_ENTITY_RELOAD_OWNER_CURRENCY_PASSED` was emitted.
- The Wolf was **manually seeded with correctly structured WWW PDC**, not generated and registered as a managed WWW individual; no `readLoaded` guarantee for WWW arises from this test. Entity MCA source discovery, offline PDC-to-API reconstruction, monetary deduplication, generation and BAN remain separate obligations.

## Limitations

- Fixture PDC is not proof of real WWW genome/phenotype initialization or on-game lifecycle compatibility, and does not prove SimpleCoins has the full stored-Entity-to-WCL-reader adapter. This is a bounded persisted-value/read-side compatibility check.
- No product code/source or real player/VPS data changes. CAT status is Draft, normative Confirmed and formal Gates remain not-run. G0/G1 and admission remain unresolved.
