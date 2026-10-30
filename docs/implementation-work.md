# SimpleCoins 1.0.0 implementation work

Baseline: `dd51e87e570b8f7daefb5a4d604836d68b4f8bb4`, existing feature branch
`feature/initial-domain-20261027` (originates from develop).

Normative requirements: Drive `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/`
01, 02A, 02B, 03 and 05. Register blocks remain out of scope (1.1.0).

## Execution boundary

The user explicitly permits reasoning to replace environment-impossible work.
CAT-TDD `9dec8c01feb86a4cb4c7f897875de9cffcb34817` was obtained and its
16 locked core files passed the plugin integrity verifier. This revision only
emits Vitest tests (`cat_compile_v2.emit_vitest`); tech-junit has instructions
but no Java renderer. Consequently Java expectations are derived manually from
the normative requirements. This is an authorized adaptation, **not** a passed
CAT process-closure/model-conformance/generated-test/implementation-conformance
gate. Actual Java tests, builds and CI remain separate executable checks.

## Dependency order and acceptance

1. Settlement failure latch: duplicate, unknown or late input must make an entire
   settlement unpublishable even when the caller catches its exception.
2. Bounded offline-data reader: malformed/truncated/unknown data fails closed;
   currency identifiers, nested stacks and wallets retain exact integer value.
3. Durable issuance: pending reservation does not count as physical money;
   player inventory and receipt share a save; recovery never reissues a receipt.
4. Paper adapters and commands: known offline players only; permission checks;
   bounded delivery; current ownership and data-source failures remain explicit.
5. Resource adapters and existing wallet/recipe regression.
6. Build, CI, JAR inspection, integration and a separate real-server checklist.

No step is completed by this plan. Test reports and current source establish
implementation status; real Paper/Geyser/Nexo behavior needs separate evidence.

## Executed changes (2026-10-29)

- Latched settlement input errors so a caught error cannot publish a partial result.
- Added a checksummed, fsynced atomic grant journal and head; detects missing tail,
  malformed records and receipt rollback. Recover a journal/head interrupted write.
- Added bounded NBT decoding and Paper playerdata receipt readback after saveData.
- Routed known offline and online monetary grants through reservations; partial
  deliveries persist active sequence and cumulative paid amount in the same playerdata.
- Protected wallet GUI collect/hotbar/offhand/creative paths and native bundle use.
  New wallet crafts and Crafter outputs receive fresh UUIDs; bulk wallet crafting is refused.
- Generated all nine Bedrock textures, manifest and Geyser v2 definition mappings
  from the same images and stable model identifiers as the Java pack.

Tests exercise pure Java journal/restart/corruption semantics, NBT decoding,
settlement, currency math, and generated pack structure. They do not establish
Paper event behavior, power-loss durability, client rendering or full accounting.

## Still unimplemented (not an environment-only test limitation)

- Complete world/playerdata/region/entity discovery, consistent startup snapshot,
  zero-player join gate, commit orchestration and ongoing accounting journal.
- BlockLocker ownership/Everyone, all current BAN types, WWC/WWW reader adapters.
- Admin world statistics, arbitrary balance and dead-asset location queries.
- Nexo/Scaffolding asset integration. Geyser-Spigot installation is now implemented;
  external Geyser deployments still require transfer of generated artifacts.

No release or production readiness claim is made for this branch. Keep main
unchanged. Geyser export uses the v2 mapping format documented at
https://geysermc.org/wiki/geyser/custom-items/ ; actual target installation remains unverified.

## Geyser installation design

Geyser source `e963aeca854711b5ba843541f6e03ccfcadd3ef1` creates its core in
Spigot onLoad, but loads registries in onEnable/start. Generate and install our
pack/mapping in SimpleCoins onLoad, before any plugin onEnable. The soft dependency
makes Geyser's data folder available. Only owned, digest-matching files may be
replaced/removed. When Nexo is present remove only unchanged SimpleCoins-managed
Geyser outputs, leaving Nexo to own resource distribution. An altered file is an
explicit error. This does not prove compatibility with older Geyser builds or
Nexo/Scaffolding, nor enable a disabled Geyser custom-content setting.

## 2026-10-30 continuation: denomination receipts and settlement source discovery

### Durable exact-denomination grants (implemented on this feature branch)

- SCG2 issuance records include the original denomination and coin count for
  the four-argument online coin give. The prior SCG1 monetary records remain
  readable, and new monetary grants retain their minimum-coin breakdown.
- A denomination grant is never silently converted into a different coin
  denomination during partial inventory delivery or after a restart.
- The existing receipt/playerdata save-and-acknowledge path is used for
  denomination grants; unknown earlier undelivered grants cannot be skipped by
  a later persisted receipt. This remains subject to real-server crash tests.
- Test evidence: CI failed at commit `5aef993` because the new denomination
  test could not compile (missing API). The initial implementation commit
  `a591ca1` passed GitHub Actions workflow `40992641134`. Regression
  tests for SCG1 compatibility and skipped receipt rejection passed at commit
  `ec41135` (workflow `40992714884`).

### G0/Phase 5 discovery-only spike (implementation; not full settlement)

- `WorldStorageDiscovery` accepts actual world folders from the caller.
  It finds `region` / `entities` Anvil candidates in old or 26.x
  dimension layouts, and modern or legacy playerdata candidates.
- World-independent playerdata is discovered even if its world is excluded
  from asset statistics. Duplicate UUID playerdata candidates, missing roots,
  symlinks, read failures and discovery-budget exhaustion abort discovery.
- This outputs a **file manifest only**. It cannot be used to publish any
  economic total. It does not yet establish the authoritative playerdata
  location, lock/snapshot concurrently changing world files, decode chunk
  streams, reconcile memory and disk, detect BlockLocker/BAN/Mob ownership,
  or feed validated input to `SettlementAccumulator`.
- No tests in this phase imply server join-gate, full-world completeness,
  startup reconciliation, real Paper 26.2 compatibility or production readiness.

Full 1.0.0 accounting, custom Nexo import, server integration and release
remain blocked on their explicit acceptance and integration gates. Keep this
feature PR as draft, `develop` and `main` unchanged.

## 2026-10-30 continuation: Anvil G0 adapter (draft PR, not releasable)

- Implemented `AnvilRegionReader.scan` as a read-only, per-chunk bounded
  reader for sectors, GZIP/Zlib/uncompressed NBT, and header-referenced
  external `c.x.z.mcc` payloads. Corrupt allocations, overlaps, unsupported
  codecs (including LZ4), missing external chunks, and exhausted budgets fail.
- This is a **storage decoding component**, not a complete or time-consistent
  settlement scanner. It has no automatic world freeze, ownership/BAN/BlockLocker
  classification, Bukkit memory reconciliation, journal replay, or publish gate.
- Added tests for normal and external chunks, corrupt headers, unsupported
  compression and resource limits.
- Found a concrete bug in `WorldStorageDiscovery`: it incorrectly collects
  standalone `.mcc` sidecar files in `worldFiles`, even though those bytes
  are external chunk payloads referenced by `.mca` headers. A regression test
  `externalChunkSidecarsAreNotStandaloneRegionFiles` reproduces it.
- The first attempt to fix the discovery predicate was blocked by a tool
  safety check; readback at that time confirmed no change. A later authorized
  GitHub update fixed it in `c3b757d`, and CI for `65a9041` passed
  (run `41008921089`). Historical CI `40994018375` was red before the fix.
- The feature branch has since regained Green CI, but is still an open Draft
  PR and is not releasable until the remaining settlement and external gates
  are satisfied. Do not merge to `develop` or `main` on these partial tests.
- Additional gate requirements remain: complete 26.x item NBT extraction,
  BlockLocker and playerdata ownership, WWC/WWW read APIs, consistent snapshot,
  fail-closed login restriction, economic journal and authoritative reconciliation.



## 2026-10-30 continuation: saved-asset decoding and source staging

The following scope was implemented and verified by the GitHub Actions
runs named below; it does **not** complete 1.0.0's asset accounting.

- `c5f1c0f` (test Red) / `b6f38e5` (Green except the previous MCA bug):
  issue journals reject symbolic record files through `DurableFile.read`.
- `c3b757d`: `.mcc` files are external chunk sidecars, not independent
  `.mca` region manifest entries.
- `65a9041`: unexpected currency denomination tags do not silently become
  ordinary paper.
- `9e59476` (Red) / `a2c6af5` (Green; CI `41011369635`):
  `PlayerDataReader` decodes an authoritative gzip playerdata file using
  independent compressed and inflated-byte budgets, rejects missing, corrupt,
  noncanonical and symbolic inputs.
- `03b21cd` (Red) / `1596f71` (Green; CI `41011707539`):
  `StoredItemValue` reads known coin/wallet Bukkit PDC values from stored
  `minecraft:custom_data`, never confusing vanilla PAPER with currency.
- `5ac7531` (Red) / `8a1b71f` (Green; CI `41012031191`):
  bundle and shulker container descendants are recursively included with a
  finite nesting limit, and corrupt descendants abort.
- `92286b9` (Red) / `02f4f4d` (Green; CI `41012330529`):
  `SavedPlayerAssets` separates player inventory / ender chest and rejects
  duplicate physical slots or missing source sections.
- `918131c` (Red) / `9cb1a58` (Green; CI `41012684777`):
  `PlayerDataSettlementImport` stages stored player assets into
  `SettlementAccumulator`, with world-independent items always included.
- `20c7f8c` (CI `41012852238`): added passing tests for dead-owned (banned)
  saved assets and unreadable playerdata preventing transaction commit.
- `faf17c3` (Red) / `ba98595` (Green; CI `41013156016`):
  a currency wallet with nonempty nested bundle/container contents aborts
  rather than silently dropping the nested value.

### Release blockers still present (not test-only gaps)

1. No consistent snapshot or server admission gate, memory/disk reconciliation
   or all-world settlement orchestration. The readers require a stable source.
2. Missing full region/block-entity/item-entity inventory enumeration and
   BlockLocker protected-container ownership, including Everyone and linked
   double chests; location deduplication beyond player slots is unverified.
3. Missing authoritative standard/IP/external BAN resolver and unloaded
   WWC/WWW/WCL inventory/owner snapshot adapters.
4. Missing complete persistent journal for every live value transfer, generation
   and consumption; immutable generation swaps and restart recovery not proven.
5. Missing usable admin statistics / dead-asset location query and verified
   Nexo/Scaffolding, Geyser/Bedrock/Java client rendering matrix.
6. No Paper 26.2 real-server tests, load benchmarks, crash-recovery fault
   injection or normative CAT-TDD conformance pass in this environment.

Consequently, CI Green means the existing **unit/build** checks are passing,
not that G0/G1/G2/G3, all Phase 0-8 acceptance, or the release gate passed.
Keep the current branch on Draft PR #2; both `develop` and `main` remain
unchanged and 1.0.0 remains **unreleased**.

## 2026-10-30 continuation: terrain chunk BlockEntity extraction

- `be507b7` introduced tests for modern `block_entities` entries containing
  physical `Items` slots; CI run `41013399415` was Red because the
  extractor did not yet exist.
- `923bbee` added `StoredBlockEntityAssets`, returning values keyed by
  absolute block coordinates and inventory slot. It validates coordinates,
  block identity, duplicate BlockEntity positions, duplicate slots, and
  recursively decoded item components. CI `41013570804` passed.
- This only extracts stored block inventories on a known consistent chunk.
  It does not enumerate all region/entity sources, classify protected
  BlockLocker ownership or `[Everyone]`, reconcile loaded memory entities,
  resolve external BAN state or build an atomic world-wide settlement.
  It must not be used alone to publish a claimed complete world balance.

### Current handoff

- Feature PR #2 remains Draft. Neither `develop` nor `main` is updated.
- The unit-test/Gradle build gate is Green at `923bbee` and the last code
  release remains unverified in a real Paper/Geyser/Nexo server.
- Incomplete Phase 5-8 and G0/G1/G2/G3 gates are blockers to `develop`
  integration and 1.0.0 release, not reasons to count missing assets as zero.

## 2026-10-31 continuation: registration contract and decorated-pot inventory

- `5228a4d` corrected the saved-player zero-balance regression tests against
  02A's registration contract: an existing registered UUID with valid
  playerdata remains registered at zero, but playerdata presence alone
  does not register a never-participating UUID. The prior test conflated
  those cases. CI run `41094567495` passed.
- `aa6e0d3` introduced three Red tests for a decorated pot's lowercase
  `item` NBT stack, unsupported single-item storage, and contradictory
  `item`/`Items` representations. CI `41094819604` failed as expected:
  83 tests, 3 failures in `StoredBlockEntityAssetsTest`.
- `7e0a3e1` extended `StoredBlockEntityAssets` to count the stored
  decorated-pot stack using the existing recursive item value decoder and
  reject ambiguous or unsupported single-item NBT rather than treating it
  as an empty container. CI `41094920181` passed (Gradle build,
  domainContractCheck, JUnit task and plugin descriptor inspection).
- This is a **partial BlockEntity decoder**, not an all-world settlement.
  It does not resolve BlockLocker ownership, all other container formats,
  unloaded/loaded Entity inventory, standard/IP/external BAN, WWC/WWW,
  consistent snapshots, join gate, journal or atomic statistics publication.
  Those remain release blockers. No Paper 26.2 real-server or JE/BE/Nexo
  rendering validation was performed. CAT normative gates were not run.

Keep PR #2 Draft; do not merge to develop/main or release 1.0.0.


## 2026-10-31: ownership consistency and dropped-item scan blockers

- Readback confirmed the previous baseline CI at `18a7f107` was Green (workflow
  `41095126112`). The existing `BlockEntitySettlementImport.stageChunk`
  invokes its ownership resolver for every nonzero slot, including multiple
  slots of the same physical block. A resolver observing a changing protection
  state could assign different owners to one container in a single chunk.
  The planned fix is one authoritative ownership decision per block per chunk,
  followed by snapshot-generation validation at the orchestration boundary.
- A regression test was committed as `b1cbbda`, but the corresponding
  production-code update was explicitly refused by the GitHub write safety
  check. An independent dropped-item Entity decoder was likewise planned,
  with test commit `ae417c5`, but its production-file creation was refused.
  Neither feature is implemented or verified; do not treat those test-only
  commits as completed Red/Green cycles.
- To avoid leaving deliberately uncompilable/Red tests in the shared feature
  branch, the Entity test was removed in `aa179b7`, and the block ownership
  test restored to the original contents in `6c22cd2`. These are cleanup
  commits, not fixes. The feature branch still requires a fresh CI run for
  its current HEAD. No safety refusal was circumvented.
- Release blockers remain unchanged: complete Entity and BlockEntity scans,
  BlockLocker/WWC/WWW/BAN integration, coherent snapshot, join gate, journal,
  administrative queries, and external JE/BE/Nexo/Paper verification.
