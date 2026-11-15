---
cat_version: '0.2'
kind: work
id: simplecoins.a05.paper-stored-currency-providers
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
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

# A05 — 配布WWC/WWW Readerから実貨幣付き保管物を読み取る独立Paper検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/439 |
| https://github.com/surumeneco/WhatAWonderfulChicken/blob/develop/src/main/java/co/surumene/whatawonderfulchicken/service/WonderfulChickenStorageReader.java |
| https://github.com/surumeneco/WhatAWonderfulWolf/blob/develop/src/main/java/co/surumene/www/persistence/WonderfulWolfRuntimeCodecV1.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.paper-stored-currency-providers | simplecoins.a05.live-multiple-provider-registration;simplecoins.wallet-uuid-duplicate-detection |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 build 134; pinned distributable WGL/WCL/WWC/WWW; item PDC and BlockStateMeta |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.paper-stored-currency-providers.md;tests/paper-wcl-services-probe/build.gradle.kts;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/build.gradle.kts;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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

## Bounded real-Paper scenarios

1. Load the exact four release-JAR blobs and isolated SimpleCoins under default-deny login. Reuse the already measured two-provider registration check. No real player login, VPS world, real balance or production source mutation.
2. Build a genuine SimpleCoins PDC+item_model coin and store it in an actual Paper `SHULKER_BOX` `BlockStateMeta`. Pass that shulker as WWC's authenticated `equipment_shulker_box` serialized ItemStack and a valid `follow_target` UUID. Invoke the packaged WWC reader's `readStored`; verify the returned `StoredItem` bytes preserve the physical shulker and real nested coin, with no flattening/double count.
3. Load the probe's disposable chunk **before** spawning one Chicken. Without loading more chunks in the reader path, require the Chicken to be indexed/valid, then compare `readLoaded(entityId)` to `readStored(entityId,...)` by entity UUID, world UUID, relationship and serialized item bytes. This establishes read-path identity, not a correct accounting deduplication implementation.
4. Construct a version-3 WWW runtime fixture using the **published byte format implemented by the production codec** (magic, optional owner/commander, slot-indexed serialized ItemStacks, pedigree and override terminators). Pass it through the packaged WWW `readStored` with authentic schema marker and verify the returned weapon and cargo bytes are real individually tagged coins, and both owner and commander UUIDs survive intact. A manually written fixture is a reader-input test, not proof of a production writer lifecycle.
5. Corrupt WWC and WWW schemas independently; require explicit `Failure(UNSUPPORTED_SCHEMA)` instead of `NotFound`, `Success(empty)` or arbitrary fallback. The empty/unrelated entity case remains `NotFound`.
6. If a packaged version disagrees with the documented source format, fail the executable probe, preserve the actual error and bound a follow-up verification. Do not edit the provider's storage or attempt a speculative fallback.

## Non-goals

- This is verification-only `issue-work`; no production file, current player inventory or journal is touched.
- Loaded WWW's actual game-owned individual store, off-chunk saved Entity NBT extraction, external owner/BAN evidence, nested Bundle scanning, source-generation atomicity, and complete G1 remain unverified by this probe.
- CAT Work structure and JUnit/Paper CI never substitute for a legitimately Confirmed Process/PI/TCE or normative conformance Gates.
