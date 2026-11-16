---
cat_version: '0.2'
kind: work
id: simplecoins.a05.unloaded-stored-provider-readback
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

# A05 — 未ロードEntityの保存NBTからWCL Readerまでの実経路検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/443 |
| https://github.com/surumeneco/SimpleCoins/pull/442 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/AnvilRegionReader.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.unloaded-stored-provider-readback | simplecoins.a05.www-offline-entity-anvil-proof;simplecoins.a05.wwc-offline-entity-anvil-proof |

## Technologies

| Technology |
| --- |
| Java 25; official Paper 26.2 build 134; pinned WCL/WWC/WWW/WGL release JARs; production AnvilRegionReader |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.unloaded-stored-provider-readback.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43742248253 (official Paper integration, CAT structure only) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Test obligations

- Under isolated official Paper 26.2 with default-deny admission and pinned release WGL/WCL/WWC/WWW, create **two genuine disposable Bukkit Entities** in an explicitly loaded **far-away Chunk 40,40**, one Chicken with authentic nested SimpleCoins coin ×2 and WWC follow-target, one Wolf with independent WWW owner/commander IDs and real weapon/cargo coins in the documented runtime schema. Both PDCs are **manually valid fixtures, not genome-managed individuals**. Persist via Paper, then shut down process A.
- Process B must begin without any game-level `getChunkAt` for Chunk 40,40. Assert that World reports this chunk **not loaded before** the scan and **not loaded after** the scan. If Paper has already loaded it, the test must fail rather than silently claim an unloaded source.
- In the running Paper process B, use the existing production read-only `AnvilRegionReader.scan` against actual `World#getWorldPath()/entities/r.1.1.mca`. Validate chunk coordinate 40,40, exactly one tagged Entity of each type, distinct UUIDs from process A and the exact persisted `BukkitValues` PDC keys. Do not call `getChunk`, `getEntity` or use Paper-loaded Entity instances for those fixtures.
- Convert only recognized, type-verified saved `BukkitValues` values to detached Bukkit `PersistentDataContainer` objects without inventing, repairing or changing missing fields; dispatch WWC and WWW individually to their **actual registered** `WonderfulStorageReader.readStored` methods. Confirm WWC's reconstructed shulker still contains the real two 5000-unit coins and follow-target; WWW's v3 decoded state retains owner/commander and weapon/cargo genuine currency.
- Reject duplicate tagged sources, missing/wrong type, absent PDC, bad runtime/version and any chunk-load side effect. No grouping of a missing provider into zero-valued money.
- Keep prior full Paper save/offline-JVM MCA/reload tests and Domain/static CI green.

## Verified bounded result

- Actual official Paper 26.2 build 134, exact PR head `4145bed2d6178bed0e56d0275a7a77a06f665fcb`: [CI #43742248253](https://github.com/surumeneco/SimpleCoins/actions/runs/43742248253), plus Domain foundation #43742248336 and Phase6 static #43742248327, all successful. Work `contract_valid:true`, `semantic_approval_verified:false`; no formal CAT gates ran.
- First Paper process saved one WWC-typed Chicken containing a real SimpleCoins 5000-unit coin ×2 inside a serialized ShulkerBox, and one WWW-typed Wolf with distinct owner/commander and 10000-unit coin weapon + 1000-unit coin ×5 cargo in **far Chunk 40,40**; Paper's actual `entities/r.1.1.mca` was produced.
- In the **independent next Paper JVM**, before touching Chunk 40,40, `world.isChunkLoaded(40,40)` was false. The test used `AnvilRegionReader.scan` on the real saved Entity MCA, validated both unique UUIDs and type-correct saved `BukkitValues`/runtime bytes against process-A witnesses, reconstructed only those recognized keys in detached `PersistentDataContainer` instances and called **actual registered** `WonderfulStorageReader.readStored` providers. Their returned serialized ItemStacks reproduced the WWC nested money and WWW owner/commander + both cargo classes. Source log `PAPER_A05_UNLOADED_WWC_WWW_SAVED_NBT_TO_REAL_WCL_PASSED source=entities/r.1.1.mca entities=2 regionChunk=40,40 totalCoins=25000`.
- After readback, `world.isChunkLoaded(40,40)` remained false. Existing process A/B reload and standalone offline JVM tests from PR #442/#443 also passed. All fixture entities have **manually typed provider PDC**, not provider-managed genome lifecycle.

## Boundary

This verifies one known detached two-provider saved-Entity source path, not **discovery of every entity region file**, wallet UUID global deduplication, BAN classification, provider initialization of genome individuals, same-generation saved/LIVE atomicity or safe admission. No production source or VPS data is modified; CAT remains an issue-work Draft with formal Gates not-run.
