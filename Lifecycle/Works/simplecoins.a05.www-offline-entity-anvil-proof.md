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
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Bounded verification

1. Reuse the exact pinned WWC/WWW/WCL/WGL distributable JARs, isolated Paper 26.2, default-deny admission and authentic SimpleCoins PDC/model money from prior A05 CI.
2. On Paper process A create a disposable **vanilla Wolf** fixture in already loaded Chunk 4,4 with `wonderful_wolf` PDC marker, schema v1 and the production WWW codec's documented v3 binary runtime structure. Encode **different owner and commander UUIDs** plus one genuine 10000-unit money weapon and five 1000-unit coins in cargo slot 7. Validate the packaged WWW `readStored` result before saving. Record immutable identity/runtime bytes; Paper itself must write its Entity Anvil.
3. After Paper exits, use the existing standalone **non-Bukkit** offline Anvil scanner on that very same saved `entities/r.0.0.mca` to find the original tagged Wolf and compare physical Entity UUID, type, source chunk and original `BukkitValues` namespace/schema/v3 runtime bytes. Require exactly one tagged Wolf distinct from the WWC Chicken, preserving each as a separate authority.
4. Reboot Paper as process B using the same world; find the same single Wolf via real Paper Chunk (no replacement fixture), compare the stored PDC via the distributed WWW reader and verify owner/commander and physical currency values. Since this manually typed test Wolf is **not** a genuine WWW managed individual with genome, it need not appear in WWW's `readLoaded` registry. Do not treat that NotFound as proof of a missing saved source.
5. Keep all existing WWC/WWW registration, empty, corrupt-schema, WWC nested LIVE/Stored, real persistence/reload, offline Anvil and original product Gradle regressions green. Reject altered or absent PDC before reporting success.

## Limitations

- Fixture PDC is not proof of real WWW genome/phenotype initialization or on-game lifecycle compatibility, and does not prove SimpleCoins has the full stored-Entity-to-WCL-reader adapter. This is a bounded persisted-value/read-side compatibility check.
- No product code/source or real player/VPS data changes. CAT status is Draft, normative Confirmed and formal Gates remain not-run. G0/G1 and admission remain unresolved.
