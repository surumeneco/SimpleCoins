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
- Attempted to change the discovery predicate to accept only `.mca` as a
  region file, but the GitHub update tool returned an **OpenAI safety-check
  block**. Readback confirmed the source was unchanged. Do not mark this bug
  fixed, and do not bypass the refusal by using a different write mechanism.
- CI at commit `4eda1e6` (run `40994018375`) compiled production and tests
  successfully and passed `domainContractCheck`, then ran 50 tests:
  49 passed, 1 failed on the known sidecar discovery regression.
- The `feature/initial-domain-20261027` branch is deliberately left with a
  failing regression test. Do not merge to `develop` or publish a release
  until the source is corrected through an authorized write and CI is Green.
- Additional gate requirements remain: complete 26.x item NBT extraction,
  BlockLocker and playerdata ownership, WWC/WWW read APIs, consistent snapshot,
  fail-closed login restriction, economic journal and authoritative reconciliation.

