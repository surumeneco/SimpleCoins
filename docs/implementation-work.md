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
- Automatic Geyser resource installation and Nexo/Scaffolding asset integration.

No release or production readiness claim is made for this branch. Keep main
unchanged. Geyser export uses the v2 mapping format documented at
https://geysermc.org/wiki/geyser/custom-items/ ; actual target installation remains unverified.
