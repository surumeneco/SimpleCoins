# Phase 5: server-repository plugin inventory and BlockLocker version parity

**Snapshot:** 2026-11-02 UTC. **Source:** [MineCraftXPlayServer `main` at `97ab138d53f457b7053ee87da49bd1f5d4cf1c8b`](https://github.com/surumeneco/MineCraftXPlayServer/tree/97ab138d53f457b7053ee87da49bd1f5d4cf1c8b/plugins). This is the **tracked Git repository**, not an attestation of the running Paper server, its complete plugin directory, loaded services, or deployed JAR bytes.

## Observed relevant plugin artifacts

| Tracked path in `plugins/` | Git blob | Relevance |
| --- | --- | --- |
| `blocklocker-1.15.jar` | `25eb3d01ce8d1f096c16aaf8aa30729665587a5b` | Filename indicates BlockLocker 1.15; actual running version must be verified |
| `WhatAWonderfulChicken-2.0.1.jar` | `3f46bc1beccb3627dd7aae8e15c2cca6ad78d27a` | WWC runtime artifact candidate |
| `WhatAWonderfulWolf-1.0.1.jar` | `b7e354fcbd4609cdf7abe930c91d2af58b140821` | WWW runtime artifact candidate |
| `WonderfulGenomeLib-1.0.1.jar` | `09acaf08b42cad7343de92de262518da2edec3f1` | Shared genome library, not evidence of entity ownership |
| `LuckPerms-Bukkit-5.5.71.jar` | `63b6fb15bd1cbd56eedea535bfef6a3c8085db79` | Permissions, not an asserted external BAN source |

The 22 tracked root-level JAR filenames contain **no explicitly named external BAN plugin** and no `WonderfulCoreLibrary` / WCL JAR. This is an observation about the Git tree only: untracked files, runtime-only plugins, transitive service registrations, alternative filenames and a different deployment revision are not ruled out. The server repository's [`.gitignore`](https://github.com/surumeneco/MineCraftXPlayServer/blob/97ab138d53f457b7053ee87da49bd1f5d4cf1c8b/.gitignore) excludes `banned-ips.json`, `banned-players.json` and server-specific state. Therefore no current BAN status or complete required-source set can be inferred from this repository.

## BlockLocker version and contract check

- The tracked JAR is named **1.15**, introduced in server-repository commit [`86ce6c14`](https://github.com/surumeneco/MineCraftXPlayServer/commit/86ce6c14e8fae89204a45a209655107ba0d6aa15) on 2026-10-28. The tracked [`plugins/BlockLocker/config.yml`](https://github.com/surumeneco/MineCraftXPlayServer/blob/97ab138d53f457b7053ee87da49bd1f5d4cf1c8b/plugins/BlockLocker/config.yml) still says it was originally created by 1.14.1, and retains `connectContainers: false`, `autoExpireDays: 0` and `languageFile: translations-en.yml`. The comment is a configuration provenance marker, **not** proof of the loaded version.
- The previously studied BlockLocker `v1.14.1` and upstream `v1.15` tags have **identical Git blob SHAs** for these eleven owner/geometry/text-related source files:

| Upstream source file | Identical blob SHA in both tags |
| --- | --- |
| `impl/SignParserImpl.java` | `601a9573870153697125281d66f2f96e4f005e77` |
| `impl/NbtSecretSignEntry.java` | `5254b8782cef222158175f43aef526ac03ffcd75` |
| `impl/JsonSecretSignEntry.java` | `7456e7009b6d9b45f8ed54c5f9f3fe2373a66e98` |
| `impl/profile/ProfileFactoryImpl.java` | `19ad9bdc4f6af6746df83dc3cd571768e2ad97b5` |
| `impl/profile/PlayerProfileImpl.java` | `12b39847c7eaa9765bc1179b7937fd54848f3fc0` |
| `impl/profile/EveryoneProfileImpl.java` | `26d1bed4d0e842727050a0492fa1b68f3b3c573b` |
| `impl/ProtectionSignImpl.java` | `aa0abca88daea164a51ef00c76a07f22786e9594` |
| `impl/blockfinder/BlockFinder.java` | `27db22ebfb6bd32b5e383eff54d0ac579843d46b` |
| `impl/blockfinder/ConnectedContainersBlockFinder.java` | `2da4ea03d5ff69543f1ac470768149c581ff2cd7` |
| `impl/blockfinder/SeparateContainersBlockFinder.java` | `f056b5333411074599fb42e06d499356beaf8de7` |
| `BlockLockerAPIv2.java` | `d4c1e8f807a2192ec48226d4719b4562b2be0a7c` |

Source comparison: [upstream `v1.14.1`](https://github.com/rutgerkok/BlockLocker/tree/v1.14.1/src/main/java/nl/rutgerkok/blocklocker) and [`v1.15`](https://github.com/rutgerkok/BlockLocker/tree/v1.15/src/main/java/nl/rutgerkok/blocklocker). Identical source blobs support parity for **only these files**. They do not establish binary identity, all-version API compatibility, plugin configuration behavior, or actual Paper 26.2 runtime correctness.

### `connectContainers: false` and double-chest semantics (source verification)

The tracked BlockLocker configuration has `connectContainers: false`, but **this does not disable double-chest grouping**. The upstream [`BlockFinder.create`](https://github.com/rutgerkok/BlockLocker/blob/v1.15/src/main/java/nl/rutgerkok/blocklocker/impl/blockfinder/BlockFinder.java) (blob `27db22ebfb6bd32b5e383eff54d0ac579843d46b`) selects `SeparateContainersBlockFinder` when false. Its [`findContainerNeighbors`](https://github.com/rutgerkok/BlockLocker/blob/v1.15/src/main/java/nl/rutgerkok/blocklocker/impl/blockfinder/SeparateContainersBlockFinder.java) (blob `f056b5333411074599fb42e06d499356beaf8de7`) returns the starting block for a single chest, or **both halves** when the starting chest's `LEFT/RIGHT` block state identifies a neighbor. It does not search a wider same-material container chain.

With `connectContainers: true`, upstream [`ConnectedContainersBlockFinder`](https://github.com/rutgerkok/BlockLocker/blob/v1.15/src/main/java/nl/rutgerkok/blocklocker/impl/blockfinder/ConnectedContainersBlockFinder.java) (blob `2da4ea03d5ff69543f1ac470768149c581ff2cd7`) additionally searches contiguous same-material containers along cardinal and vertical directions, with a per-direction search bound of 10 blocks. The two modes therefore differ in **extended container adjacency**, not whether a vanilla double chest can have a second half.

| Source-backed case | `connectContainers: false` | Settlement implication |
| --- | --- | --- |
| Single chest (`Type.SINGLE`) | One starting block | Do not infer neighboring unrelated chests belong to the same protected unit |
| Double chest (`Type.LEFT/RIGHT`) | Starting block plus its calculated adjacent half | Inspect attached protection signs on **both** halves; `false` is not permission to omit the other half |
| Adjacent non-paired containers | No chain traversal | Do not merge them merely because they touch; verify the actual reciprocal chest state independently |

This is a **pinned upstream source/configuration interpretation**, not a Paper runtime assertion. The tracked server config may differ from the live config, and `SeparateContainersBlockFinder` does not itself prove that a neighboring block has a reciprocal chest state. The SimpleCoins geometry/ownership validation and saved/live completeness remain separate gates.

## WCL provider deployment boundary (source verification, not runtime attestation)

The current [WonderfulCoreLibrary `develop` README](https://github.com/surumeneco/WonderfulCoreLibrary/blob/develop/README.md) (Git blob `5f44c0407a2a3ebe98e3c898fba5d43f6851000a`) describes **WCL 0.1.0 as an unreleased integration candidate**, alongside WWC **2.1.0** and WWW **1.1.0**. It explicitly says that `main` remains unchanged and live Paper/JE/BE and performance acceptance is outstanding. The only intended WCL deployable is `WonderfulCoreLibrary-0.1.0.jar` from `wcl-plugin`; the API and core JARs are not separate server plugins. WCL exposes a Bukkit service and read-only storage snapshot/result contracts, but **does not own consumer Entity/PDC identity, ownership policy, or world scans**.

The pinned server Git tree above instead tracks WWC **2.0.1** and WWW **1.0.1**, with **no WCL JAR** among its 22 root-level JARs. Their current `main` READMEs describe those versions ([WWC](https://github.com/surumeneco/WhatAWonderfulChicken/blob/main/README.md), blob `db97d56612b1cef545f29e5e40c6661afb93a852`; [WWW](https://github.com/surumeneco/WhatAWonderfulWolf/blob/main/README.md), blob `7227ece0f88a681dfe3290fa092a814aa1dfe0d8`). **This is a repository-level version and deployment-evidence gap, not proof of runtime absence or binary incompatibility.** The candidate WCL/WWC/WWW service contract must not be treated as available on the live server without a loaded-plugin/service inventory and version-specific provider check.

For SimpleCoins owner/commander and inventory settlement, an unavailable WCL service, missing provider, unverified entity UUID/world UUID/provider ID binding, or incomplete saved/loaded coverage must remain **UNKNOWN**, never FREE or a zero-asset snapshot. Capture actual plugin versions and service registrations, establish which provider owns each relationship field, and verify source completeness before using WCL storage results as settlement evidence. No production migration or plugin deployment is authorized by this source comparison.

## Remaining concrete acceptance checks

1. On the actual Paper host, capture the loaded plugin list and plugin version, the deployed BlockLocker/WWC/WWW/WCL JAR identities, and all enabled BAN providers. Compare against this pinned repository snapshot without assuming they are equal.
2. For every actual BAN source, establish a UUID-keyed current-state API and authoritative IP-to-UUID mapping; missing/failed sources remain UNKNOWN rather than CLEAR. Do not use absence from the tracked JAR list as negative BAN evidence.
3. Verify BlockLocker 1.15 live sign text/PDC, localized Everyone aliases, sign attachment and reciprocal double-chest handling on Paper; then validate saved/loaded reconciliation and the **actually deployed** WWC/WWW/WCL provider versions and service contracts. Do not substitute the WCL 0.1.0 candidate for the observed server runtime. Formal CAT confirmation and machine Gates remain separate requirements.

**TaskQueue:** `TASK-20261101-004`. This is a source-backed inventory and version-parity verification artifact, not a completed adapter, successful Paper test, or permission to publish owner settlement.
