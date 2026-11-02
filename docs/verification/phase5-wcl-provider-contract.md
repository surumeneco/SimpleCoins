# Phase 5 — WCL / WWC / WWW storage evidence contract

Status: **source-verified integration evidence**, not a CAT Confirmed artifact and not a live Paper acceptance. Verified 2026-11-02 against the `develop` source of WonderfulCoreLibrary (WCL), WhatAWonderfulChicken (WWC), and WhatAWonderfulWolf (WWW). This document is a handoff for TASK-20261101-004; it does not implement a SimpleCoins adapter.

## Verified source contract

| Source | Exact Git blob SHA | Contract |
| --- | --- | --- |
| WCL `wcl-plugin/.../inventory/WonderfulStorageReader.java` | `0c0d1b5eb37e31f3aeebdbd6efe2c7214916322a` | `providerId()`, `readLoaded(UUID)`, `readStored(UUID, UUID, Optional<Position>, PersistentDataContainer)`; Paper primary thread; offline PDC must be supplied by an independent reader; no chunk loading or writes |
| WCL `wcl-api/.../inventory/StorageReadResult.java` | `409d4654cc1cd0b64945863a1daa214c62faceb5` | Sealed `Success(snapshot)` / `NotFound` / `Failure(code,detail)`; NotFound and Failure are **not empty inventories** |
| WCL `wcl-api/.../inventory/StorageSnapshot.java` | `94b2baaecef32c62828a9cfa934582ffb8beeab2` | Immutable `entityId`, `providerId`, `worldId`, optional position, relationship UUID map and serialized item list; duplicate slot rejected |
| WCL `wcl-api/.../inventory/StoredItem.java` | `2ec6b42ab8ccbde46fe5eb4b44e62abfa9fd4a58` | Slot + defensive-copy serialized bytes; nested container contents remain opaque until a caller explicitly traverses them once |
| WWC `.../service/WonderfulChickenStorageReader.java` | `fed4ea0ef3b0d9f8bbea23f14db7ee2e8cd1bed0` | `providerId=wwc`; `relationships.follow_target` is positive-only UUID evidence; slots `carpet`, `shulker`, `head`; missing marker returns NotFound, corrupt/unsupported data returns Failure |
| WWW `.../persistence/WonderfulWolfStorageReader.java` | `37e35aff978686183056bbc090b54e9b44e20ac9` | `providerId=www`; `relationships.commander` and `owner`; slots `weapon` and `cargo:<index>`; missing loaded individual state is Failure, not verified empty inventory |

The WWC plugin registration is verified at `WhatAWonderfulChickenPlugin.java` blob `fa44e58119d05b04c784d48d68e1692e0b79cce6`; WWW registration at `WhatAWonderfulWolfPlugin.java` blob `c381d36a113cc57854e17e9b89324731dd2e3172`. **Both register the same `WonderfulStorageReader.class` service interface** with `ServicePriority.Normal`, each owned by its respective plugin, and unregister on disable. Consequently a single `ServicesManager.load(WonderfulStorageReader.class)` is not a complete two-provider inventory. A future SimpleCoins adapter must enumerate registrations, validate owner/enabled status and `providerId()`, and reject missing/duplicate/ambiguous required providers; it must not infer that one loaded registration represents both species.

### WWW public loaded-owner API visibility (source verification)

The optional SimpleCoins reflection path `loadedIndividuals() → find(UUID) → commanderId()/ownerId()` was checked against the actual WWW `main` and `develop` source, rather than a synthetic Java visibility fixture:

| WWW source | `develop` blob SHA | `main` blob SHA | Verified public declaration in both branches |
| --- | --- | --- | --- |
| `co.surumene.www.WhatAWonderfulWolfPlugin` | `c381d36a113cc57854e17e9b89324731dd2e3172` | `09b1d2a4719258118a93711b23bc575a8e43606f` | `public final class`; `public WonderfulWolfLoadedIndividuals loadedIndividuals()` |
| `co.surumene.www.persistence.WonderfulWolfLoadedIndividuals` | `61c417d6866dd3ed3c756e3bc8252388ad1c14ca` | `e0b31006c841f14c19831fc51eb1efdc3c7d9eed` | `public final class`; `public Optional<WonderfulWolfIndividual> find(UUID)` |
| `co.surumene.www.individual.WonderfulWolfIndividual` | `ad2bd722d6e3ba7e5ddaec457f17e834b9b560a9` | `ad2bd722d6e3ba7e5ddaec457f17e834b9b560a9` | `public record` with `Optional<UUID> commanderId` and `ownerId` public record accessors |

The `main` README (blob `7227ece0f88a681dfe3290fa092a814aa1dfe0d8`) describes WWW **1.0.1**, whereas the WCL integration evidence above concerns the newer `develop` candidate. Both inspected source branches expose the reflection method chain expected by `PaperWonderfulWolfOwnerAdapter`; the individual record blob is identical. The differing plugin/registry blobs are **not** binary-compatibility proof for the tracked `WhatAWonderfulWolf-1.0.1.jar` or the loaded Paper instance.

Consequently, the package-private implementation classes in SimpleCoins [PR #98](https://github.com/surumeneco/SimpleCoins/pull/98) are a **hypothetical compatibility fixture**, not a reproduced visibility defect in either inspected WWW branch. Its two expected-Red cases do not justify a production reflection change for the inspected public declarations. Source inspection does **not** establish deployed JAR identity, plugin enabled state, a loaded individual, saved/offline completeness, or real Paper reflection success. Those remain runtime acceptance conditions. Missing/failed lookup is UNKNOWN, not verified FREE.

## Required fail-closed decision matrix for SimpleCoins

| Evidence case | Safe interpretation |
| --- | --- |
| `Success` with matching entity UUID, world UUID, expected provider ID, valid snapshot and zero `items` | Empty **provider storage for that verified entity only**; does not prove complete world inventory or absence of other sources |
| `Success` with wrong entity/world/provider identity, duplicate provider registration, or an unverified snapshot origin | UNKNOWN / reject settlement staging; do not count zero |
| `NotFound` for `readLoaded` | Entity not present in this loaded provider; **not** evidence that saved/offline entity or items are absent |
| `NotFound` for `readStored` | Provider marker not recognized in the supplied PDC; do not infer that the authoritative offline catalog is complete |
| `Failure`, missing/disabled provider, exception, absent offline PDC, unsupported schema | UNKNOWN / incomplete source; do not silently convert to an empty item list |
| WWC `follow_target` present and validated | Positive commander/follow-target evidence; missing key does not establish verified ownerlessness |
| WWW `commander` and `owner` present and verified | Commander takes precedence; fallback to owner only after verified commander absence under a complete provider contract |
| Stored `shulker` / `cargo` bytes | Preserve PDC and nested contents; decode with the correct Paper item codec and count nested items exactly once |
| Saved and loaded representations of the same entity | Reconcile by canonical entity/world identity and source snapshot consistency; do not double count or publish on unresolved conflict |

### Registration and provider-result acceptance vectors

The following are **source-derived handoff vectors**, not live-service observations or a Confirmed CAT TestModel. They narrow the existing decision matrix without assuming that either plugin is installed on the target Paper server.

| Vector | Observed registration/result | Required consumer behavior |
| --- | --- | --- |
| WCL-REG-01 | WWC and WWW each register `WonderfulStorageReader.class` at `ServicePriority.Normal` | Enumerate **all** registrations; `ServicesManager.load` alone cannot establish the two-provider inventory |
| WCL-REG-02 | One expected enabled provider is missing or disabled | Mark that source incomplete/UNKNOWN, not a verified empty inventory; preserve other provider evidence separately |
| WCL-REG-03 | Two registrations claim the same expected `providerId` | Reject ambiguous provider selection even if one returns `Success` |
| WCL-REG-04 | Registration owner/plugin identity disagrees with the expected WWC/WWW source, or `providerId` is unexpected | Reject the untrusted source; a matching string alone is insufficient proof of provider authority |
| WCL-REG-05 | Provider returns `Success` but snapshot entity/world/provider differs from the independently verified request | Reject without counting the items; do not substitute another provider's snapshot |
| WCL-REG-06 | `readLoaded` returns `NotFound` | Do not infer offline absence; consult the independently verified saved source and reconcile loaded/saved identity |
| WCL-REG-07 | WWC `readStored` sees its marker but no `data_version` | Current WWC source permits legacy success; **do not** equate that with a verified commander/owner, which requires separate evidence |
| WCL-REG-08 | WWW `readStored` sees its marker but no schema or runtime bytes | Current WWW source returns `Failure(MISSING_DATA)`; do not convert this into zero stored assets |
| WCL-REG-09 | WWW `readLoaded` finds a wolf entity but no registered loaded individual | Current WWW source returns `Failure(MISSING_DATA)`, not `NotFound` or verified empty storage |

For WCL-REG-01–04, verify the actual Bukkit `RegisteredServiceProvider` owner, provider identity and enabled state on the primary thread, then compare against a caller-declared complete expected-provider inventory. A static source check cannot prove the deployed JAR versions or registration lifecycle. WWC source blob `fed4ea0ef3b0d9f8bbea23f14db7ee2e8cd1bed0` and WWW source blob `37e35aff978686183056bbc090b54e9b44e20ac9` support the distinct result semantics in WCL-REG-07–09.

The WCL `StorageSnapshot` constructor itself enforces non-null identities, immutable relationships/items and unique slots. It does **not** independently establish the completeness of the world/entity catalog, source freshness, canonical owner UUID, or Paper/WWC/WWW runtime compatibility. Those are caller obligations.

## Direct reproducible evidence

The exact three WCL `develop` API blobs above were independently reproduced and matched with `git hash-object`. Java 21 compiled them without Paper dependencies; a focused direct harness passed **16/16 assertions** for the three result variants, empty-but-successful snapshot distinction, identity fields, defensive copies, duplicate slots, malformed failures and invalid positions. This validates only the pure Java value contract. It is **not** a Gradle/JUnit run against WWC/WWW or a Paper server test.

## Remaining implementation and acceptance boundary

1. Draft/confirm the SimpleCoins WCL integration CAT Process/PI/TCE, run the required deterministic pre-implementation Gates, and create a separate adapter implementation Work on an uncontended feature branch.
2. Enumerate and validate both Bukkit service registrations on the main thread; independently supply complete loaded-world and saved/offline PDC catalogs, and verify exact entity/world/provider identity for every snapshot.
3. Add consumer contract tests for NotFound vs empty Success, Failure propagation, duplicate providers, wrong identity, commander precedence, missing relationship semantics, nested items and loaded/disk conflict. Execute exact-head Gradle/JUnit and real Paper 26.2 / WWC / WWW / WCL acceptance before considering settlement complete.

No production code, main/develop merge, deployment, external API runtime or live-server test is claimed by this document.
